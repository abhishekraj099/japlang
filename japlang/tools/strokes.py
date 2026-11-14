"""Builds app/src/main/assets/strokes.json: stroke paths, in writing order, for every kana and kanji the course uses.

The paths come from KanjiVG (CC BY-SA 3.0). SVGs are downloaded once into the folder given as the first argument
and reused on later runs:  python3 tools/strokes.py /tmp/kanjivg
"""
import json, re, sys, urllib.request
from pathlib import Path

assets = Path(__file__).resolve().parent.parent / "app/src/main/assets"
cache = Path(sys.argv[1]); cache.mkdir(parents=True, exist_ok=True)
course = json.loads((assets / "course.json").read_text(encoding="utf-8"))

chars = set()
def walk(node):
    if isinstance(node, dict):
        if "kind" in node and "jp" in node:
            chars.update(node["jp"] + node.get("reading", ""))
        for value in node.values():
            walk(value)
    elif isinstance(node, list):
        for value in node:
            walk(value)
walk(course)
for row in course["extraKana"]:
    chars.update("".join(row["chars"].split()[::2]))
chars = sorted(c for c in chars if ("ぁ" <= c <= "ヾ" or "一" <= c <= "鿿") and c != "ー")

strokes = {}
for char in chars:
    svg = cache / f"{ord(char):05x}.svg"
    if not svg.exists():
        url = f"https://raw.githubusercontent.com/KanjiVG/kanjivg/master/kanji/{svg.name}"
        try:
            svg.write_bytes(urllib.request.urlopen(url, timeout=30).read())
        except Exception as e:
            print("no stroke data:", char, e)
            continue
    paths = re.findall(r'<path id="kvg:[0-9a-f]+-s\d+"[^>]*\sd="([^"]+)"', svg.read_text(encoding="utf-8"))
    if paths:
        strokes[char] = paths
(assets / "strokes.json").write_text(json.dumps(strokes, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
print(len(strokes), "characters,", sum(map(len, strokes.values())), "strokes")
