"""Daily job: read the news sites, summarize the new stories with ONE Gemini request,
and write data/feed.json, which the app downloads.

Run by .github/workflows/daily-feed.yml. To run it by hand:
    GEMINI_API_KEY=your-key python scripts/build_feed.py
Uses only the Python standard library.
"""

import gzip
import html
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "data" / "feed.json"

# The sites the app reads. Add or remove entries here; the app picks the list up from feed.json.
# (id, name, feed url, site url, ai_only: keep only stories that mention AI)
SOURCES = [
    ("localllama", "r/LocalLLaMA", "https://www.reddit.com/r/LocalLLaMA/top/.rss?t=day", "https://www.reddit.com/r/LocalLLaMA/", False),
    ("huggingface", "Hugging Face", "https://huggingface.co/blog/feed.xml", "https://huggingface.co/blog", False),
    ("simonw", "Simon Willison", "https://simonwillison.net/atom/everything/", "https://simonwillison.net/", False),
    ("decoder", "The Decoder", "https://the-decoder.com/feed/", "https://the-decoder.com/", False),
    ("marktechpost", "MarkTechPost", "https://www.marktechpost.com/feed/", "https://www.marktechpost.com/", False),
    ("openai", "OpenAI", "https://openai.com/news/rss.xml", "https://openai.com/news/", False),
    ("googleai", "Google AI", "https://blog.google/technology/ai/rss/", "https://blog.google/technology/ai/", False),
    ("deepmind", "Google DeepMind", "https://deepmind.google/blog/rss.xml", "https://deepmind.google/blog/", False),
    ("nvidia", "NVIDIA Blog", "https://blogs.nvidia.com/feed/", "https://blogs.nvidia.com/", True),
    ("githubai", "GitHub AI & ML", "https://github.blog/ai-and-ml/feed/", "https://github.blog/ai-and-ml/", False),
    ("techcrunch", "TechCrunch AI", "https://techcrunch.com/category/artificial-intelligence/feed/", "https://techcrunch.com/category/artificial-intelligence/", False),
    ("verge", "The Verge AI", "https://www.theverge.com/rss/ai-artificial-intelligence/index.xml", "https://www.theverge.com/ai-artificial-intelligence", False),
    ("ars", "Ars Technica AI", "https://arstechnica.com/ai/feed/", "https://arstechnica.com/ai/", False),
    ("mittr", "MIT Tech Review", "https://www.technologyreview.com/topic/artificial-intelligence/feed", "https://www.technologyreview.com/topic/artificial-intelligence/", False),
    ("wired", "WIRED AI", "https://www.wired.com/feed/tag/ai/latest/rss", "https://www.wired.com/tag/artificial-intelligence/", False),
]

PER_SOURCE = 10          # newest stories kept per site
MAX_AGE_DAYS = 5         # older stories drop out of the feed
MAX_TO_SUMMARIZE = 100   # all sent in the single daily request
EXCERPT_CHARS = 700

# Tried in order, only if the previous one fails (busy / over quota). Normally the first one answers,
# so the job makes exactly one Gemini request per day.
MODELS = ["gemini-flash-lite-latest", "gemini-3.1-flash-lite", "gemini-flash-latest"]

CATEGORIES = ["LOCAL", "MODELS", "TOOLS", "RESEARCH", "INDUSTRY"]

LOCAL_WORDS = re.compile(
    r"\b(vram|gguf|quantiz\w*|llama\.cpp|ollama|lm studio|mlx|on-device|locally|local (model|llm|ai|inference)s?|"
    r"open[- ]weights?|consumer (gpu|hardware)|rtx|\d+ ?gb|laptop|raspberry pi|offline|lora|self-host\w*)\b")
MODEL_WORDS = re.compile(
    r"\b(new model|releases?|released|launch(es|ed)?|introduc(es|ing)|unveils?|now available|open[- ]sourc(es|ed)|"
    r"gpt-\S+|gemini|claude|llama|mistral|qwen|deepseek|gemma|phi-\d|grok|weights|checkpoint)\b")
RESEARCH_WORDS = re.compile(r"\b(paper|research(ers)?|study|benchmarks?|arxiv|scientists|experiment)\b")
TOOL_WORDS = re.compile(
    r"\b(tool|tools|app|api|sdk|agent|agents|plugin|feature|update|library|framework|cli|extension|workflow)\b")
AI_WORDS = re.compile(
    r"\b(ai|a\.i\.|llm|llms|model|models|neural|machine learning|generative|inference|gpt|agentic|agents?|chatbot)\b")


def guess_category(title, excerpt):
    text = f"{title} {excerpt}".lower()
    if LOCAL_WORDS.search(text):
        return "LOCAL"
    if MODEL_WORDS.search(title.lower()):
        return "MODELS"
    if RESEARCH_WORDS.search(text):
        return "RESEARCH"
    if TOOL_WORDS.search(text):
        return "TOOLS"
    return "INDUSTRY"


def clean(text):
    """HTML fragment -> plain single-line text."""
    if not text:
        return ""
    text = re.sub(r"(?is)<(script|style).*?</\1>", " ", text)
    text = re.sub(r"<[^>]+>", " ", text)
    text = html.unescape(html.unescape(text))
    return re.sub(r"\s+", " ", text.replace(" ", " ")).strip()


def parse_date(raw):
    """-> epoch milliseconds, 0 if unknown."""
    raw = (raw or "").strip()
    if not raw:
        return 0
    try:
        dt = parsedate_to_datetime(raw)
    except (TypeError, ValueError):
        try:
            dt = datetime.fromisoformat(raw.replace("Z", "+00:00"))
        except ValueError:
            return 0
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return min(int(dt.timestamp() * 1000), int(time.time() * 1000))


def local(tag):
    return tag.rsplit("}", 1)[-1]


def fetch_source(source):
    sid, name, feed_url, _site, ai_only = source
    req = urllib.request.Request(feed_url, headers={
        "User-Agent": "Mozilla/5.0 (Linux; Android) NewsReader/1.0",
        "Accept": "application/rss+xml, application/atom+xml, application/xml, text/xml, */*",
    })
    with urllib.request.urlopen(req, timeout=30) as resp:
        raw = resp.read()
    if raw[:2] == bytes([0x1F, 0x8B]):  # some sites send gzip even when not asked
        raw = gzip.decompress(raw)
    root = ET.fromstring(raw.lstrip())

    articles = []
    for item in root.iter():
        if local(item.tag) not in ("item", "entry"):
            continue
        title = link = date = description = content = ""
        for child in item:
            tag = local(child.tag)
            text = "".join(child.itertext())
            if tag == "title":
                title = text
            elif tag == "link":
                href = child.get("href")
                if href:
                    if not link or child.get("rel") in (None, "alternate"):
                        link = href
                elif text.strip():
                    link = text.strip()
            elif tag in ("pubDate", "published", "date"):
                date = text
            elif tag == "updated":
                date = date or text
            elif tag in ("description", "summary"):
                description = text
            elif tag in ("encoded", "content") and "media" not in child.tag:
                content = text
        title = clean(title)
        if not title or not link.startswith("http"):
            continue
        body = clean(description) or clean(content)
        body = re.sub(r"\s*submitted by\s+/u/\S+.*$", "", body)
        body = re.sub(r"\s*The post .{0,200} appeared first on .*$", "", body)
        body = re.sub(r"\s*\[(…|\.\.\.)\]\s*$", "…", body).strip()
        if len(body) > EXCERPT_CHARS:
            body = body[:EXCERPT_CHARS].rstrip() + "…"
        if ai_only and not AI_WORDS.search(f"{title} {body}".lower()):
            continue
        articles.append({
            "url": link.strip(),
            "title": title,
            "sourceId": sid,
            "sourceName": name,
            "publishedAt": parse_date(date),
            "excerpt": body,
            "summary": None,
            "category": guess_category(title, body),
        })
        if len(articles) >= 40:
            break
    articles.sort(key=lambda a: a["publishedAt"], reverse=True)
    return articles[:PER_SOURCE]


def summarize(api_key, articles):
    """One request for all articles. Returns {index: (summary, category, keep)}."""
    prompt = (
        "You are the editor of a time-saving AI news digest. For each article below, write a 2-3 sentence "
        "plain-English summary of the concrete news: what happened, why it matters, and key numbers "
        "(model sizes, VRAM, benchmarks, prices) when given. No hype, no filler like \"This article\". "
        "Use only the information provided.\n"
        "Pick one category: LOCAL (running models on consumer hardware, quantization, low VRAM, open weights), "
        "MODELS (new models the public can use), TOOLS, RESEARCH, INDUSTRY.\n"
        "Set keep to false only for items that are not AI news at all (ads, job posts, unrelated topics).\n"
        "Reply with a JSON array only, one object per article: "
        "[{\"i\": <article number>, \"summary\": \"...\", \"category\": \"...\", \"keep\": true}]\n"
    )
    for i, a in enumerate(articles):
        prompt += f"\n[{i}] Title: {a['title']}\nSource: {a['sourceName']}\nText: {a['excerpt'] or '(none)'}\n"
    body = json.dumps({
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {"responseMimeType": "application/json", "temperature": 0.2},
    }).encode()

    last_error = None
    for model in MODELS:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
        req = urllib.request.Request(url, data=body, headers={
            "Content-Type": "application/json", "x-goog-api-key": api_key,
        })
        try:
            with urllib.request.urlopen(req, timeout=300) as resp:
                data = json.load(resp)
            text = data["candidates"][0]["content"]["parts"][0]["text"]
            rows = json.loads(text[text.index("["): text.rindex("]") + 1])
            print(f"Gemini: {model} answered with {len(rows)} summaries")
            out = {}
            for row in rows:
                summary = str(row.get("summary", "")).strip()
                if isinstance(row.get("i"), int) and summary:
                    category = str(row.get("category", "")).upper()
                    out[row["i"]] = (summary, category if category in CATEGORIES else None, row.get("keep", True) is not False)
            return out
        except urllib.error.HTTPError as e:
            last_error = f"{model}: HTTP {e.code}"
            print("Gemini:", last_error)
            if e.code not in (429, 500, 502, 503, 504):
                break
        except Exception as e:  # timeout, bad JSON...
            last_error = f"{model}: {type(e).__name__}"
            print("Gemini:", last_error)
    raise RuntimeError(f"Gemini request failed ({last_error})")


def main():
    api_key = os.environ.get("GEMINI_API_KEY", "").strip()

    previous = {}
    hidden = set()
    if OUT.exists():
        old = json.loads(OUT.read_text(encoding="utf-8"))
        previous = {a["url"]: a for a in old.get("articles", [])}
        hidden = set(old.get("hidden", []))

    with ThreadPoolExecutor(max_workers=8) as pool:
        def safe(source):
            try:
                found = fetch_source(source)
                print(f"{source[1]}: {len(found)} stories")
                return found
            except Exception as e:
                print(f"{source[1]}: FAILED ({type(e).__name__}: {e})")
                return None
        results = list(pool.map(safe, SOURCES))

    if all(r is None for r in results):
        sys.exit("No source could be read; keeping the old feed.")

    cutoff = (time.time() - MAX_AGE_DAYS * 86400) * 1000
    seen = set()
    articles = []
    for source, found in zip(SOURCES, results):
        # A site that failed today keeps yesterday's stories instead of vanishing.
        if found is None:
            found = [a for a in previous.values() if a["sourceId"] == source[0]]
        for a in found:
            if a["url"] in seen or a["url"] in hidden:
                continue
            if a["publishedAt"] and a["publishedAt"] < cutoff:
                continue
            seen.add(a["url"])
            old = previous.get(a["url"])
            if old and old.get("summary"):  # already summarized on an earlier day
                a["summary"], a["category"] = old["summary"], old["category"]
            articles.append(a)
    articles.sort(key=lambda a: a["publishedAt"], reverse=True)

    todo = [a for a in articles if not a["summary"]][:MAX_TO_SUMMARIZE]
    if not api_key:
        print("GEMINI_API_KEY is not set: writing the feed without new summaries.")
    elif todo:
        try:
            for i, (summary, category, keep) in summarize(api_key, todo).items():
                if 0 <= i < len(todo):
                    if not keep:
                        hidden.add(todo[i]["url"])
                        continue
                    todo[i]["summary"] = summary
                    todo[i]["category"] = category or todo[i]["category"]
        except RuntimeError as e:
            print(f"{e}; publishing without new summaries, they will be retried tomorrow.")
    articles = [a for a in articles if a["url"] not in hidden]

    feed = {
        "updatedAt": int(time.time() * 1000),
        "sources": [{"id": s[0], "name": s[1], "siteUrl": s[3]} for s in SOURCES],
        "articles": articles,
        # Stories Gemini marked as not AI news; remembered so they are not sent again.
        "hidden": sorted(hidden & {u for r in results if r for u in (a["url"] for a in r)}),
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(feed, ensure_ascii=False, indent=1), encoding="utf-8")
    done = sum(1 for a in articles if a["summary"])
    print(f"Wrote {OUT.name}: {len(articles)} stories, {done} with summaries, {len(todo)} sent to Gemini today")


if __name__ == "__main__":
    main()
