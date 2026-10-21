"""Prints every Japanese text the lessons speak, one per line with where it appears, for recording sessions."""
import json
from pathlib import Path

course = json.loads((Path(__file__).resolve().parent.parent / "app/src/main/assets/course.json").read_text(encoding="utf-8"))
seen = {}

def add(text, where):
    text = text.replace(" ", "")
    if text and text not in seen:
        seen[text] = where

def walk(node, where):
    if isinstance(node, dict):
        where = node.get("id", where)
        if "kind" in node and "jp" in node:
            add(node.get("say") or node.get("reading") or node["jp"], where)
            for w in node.get("words", []) + ([node["example"]] if "example" in node else []):
                add(w.get("reading") or w["jp"], where)
        elif "tokens" in node:
            add("".join(node["tokens"]), where)
        elif "speaker" in node or ("jp" in node and "en" in node):
            add(node["jp"], where)
        for value in node.values():
            walk(value, where)
    elif isinstance(node, list):
        for value in node:
            walk(value, where)

walk(course, "")
for text, where in seen.items():
    print(f"{text}\t{where}")
