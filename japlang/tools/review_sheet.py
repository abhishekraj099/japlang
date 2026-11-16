"""Writes a CSV of every lesson text for a native-speaker reviewer: python3 tools/review_sheet.py out.csv (--check to self-test)."""
import csv
import json
import re
import sys
from pathlib import Path

assets = Path(__file__).resolve().parent.parent / "app/src/main/assets"

def load(name):
    return json.loads((assets / name).read_text(encoding="utf-8"))

JAPANESE = re.compile("[\u3040-\u30ff\u4e00-\u9fff]")

def item_rows(pid, where, it, n):
    """Rows for one practice/lesson item: the item, its notes, its example and its kanji words."""
    kind = it.get("kind", "item").lower()
    jp = f"{it['jp']} ({it['reading']})" if it.get("reading") and it["reading"] != it["jp"] else it["jp"]
    en = it.get("meaning", "") + "".join(f"; {k} {it[k]}" for k in ("on", "kun") if it.get(k))
    yield f"{pid}:i{n}", f"{where} {kind} {n}", jp, it.get("romaji", ""), en
    if JAPANESE.search(it.get("note", "")):
        yield f"{pid}:i{n}:note", f"{where} {kind} {n} note", it["note"], "", ""
    if it.get("example"):
        e = it["example"]
        yield f"{pid}:i{n}:ex", f"{where} {kind} {n} example", e["jp"], e.get("romaji", ""), e["en"]
    for j, w in enumerate(it.get("words", []), 1):
        yield f"{pid}:i{n}:w{j}", f"{where} {kind} {n} word {j}", f"{w['jp']} ({w['reading']})" if w.get("reading") else w["jp"], w.get("romaji", ""), w["en"]

def theory_rows(pid, where, blocks):
    for i, t in enumerate(blocks, 1):
        yield f"{pid}:t{i}", f"{where} theory {i}", t["pattern"], "", f"{t['title']}: {t['body']}"

def rows():
    """Yields (id, where, japanese, romaji, english); ids derive from content ids and indices so they stay stable."""
    course = load("course.json")
    for i, ph in enumerate(course["phrases"], 1):
        yield f"course:phrases:{i}", f"course phrase {i}", ph["jp"], ph.get("romaji", ""), ph["en"]
    for i, t in enumerate(course["tips"], 1):
        yield f"course:tip:{i}", f"course tip {i}", t["title"], "", t["body"]
    for u in course["units"]:
        uid = u["id"]
        for t in u.get("theory", []):
            yield from theory_rows(f"course:{uid}", uid, [t])
        for l in u.get("lessons", []):
            pid = f"course:{uid}:{l['id']}"
            yield from theory_rows(pid, f"{uid} {l['id']}", l.get("theory", []))
            for n, it in enumerate(l.get("items", []), 1):
                yield from item_rows(pid, f"{uid} {l['id']}", it, n)
        for n, p in enumerate(u.get("practices", []), 1):
            if p.get("note"):
                g = p["note"]
                yield f"course:{uid}:p{n}:note", f"{uid} practice {n} grammar note {g.get('id', '')}", g["pattern"], "", f"{g['title']}: {g['body']}"
            for i, s in enumerate(p.get("sentences", []), 1):
                yield f"course:{uid}:p{n}:s{i}", f"{uid} practice {n} sentence {i}", "".join(s["tokens"]), s.get("romaji", ""), s["en"]
            for i, it in enumerate(p.get("items", []), 1):
                yield from item_rows(f"course:{uid}:p{n}", f"{uid} practice {n}", it, i)
        for i, l in enumerate(u.get("dialogue", {}).get("lines", []), 1):
            yield f"course:{uid}:d{i}", f"{uid} dialogue line {i}", l["jp"].replace(" ", ""), l.get("romaji", ""), l["en"]
        for i, ph in enumerate(u.get("phrases", []), 1):
            yield f"course:{uid}:ph{i}", f"{uid} phrase {i}", ph["jp"].replace(" ", ""), ph.get("romaji", ""), ph["en"]
    for sc in load("scenarios.json")["scenarios"]:
        for pn, part in enumerate(sc["parts"], 1):
            for i, v in enumerate(part["vocab"], 1):
                yield f"scenario:{sc['id']}:p{pn}:v{i}", f"scenario {sc['title']} part {pn} vocab {i}", f"{v['jp']} ({v['reading']})" if v.get("reading") else v["jp"], v.get("romaji", ""), v["meaning"] + (f" -- {v['note']}" if v.get("note") else "")
            for i, s in enumerate(part["patterns"], 1):
                yield f"scenario:{sc['id']}:p{pn}:s{i}", f"scenario {sc['title']} part {pn} pattern {i}", "".join(s["tokens"]), s.get("romaji", ""), s["en"]
    for st in load("stories.json")["stories"]:
        for i, l in enumerate(st["lines"], 1):
            yield f"story:{st['id']}:l{i}", f"story {st['title']} line {i}", "".join(l["tokens"]), l.get("romaji", ""), l["en"]
    for t in load("talks.json")["talks"]:
        for turn in t["turns"]:
            yield f"talk:{t['id']}:{turn['id']}", f"talk {t['title']} turn {turn['id']}", turn["jp"].replace(" ", ""), turn.get("romaji", ""), turn["en"]
            for i, a in enumerate(turn.get("answers", []), 1):
                yield f"talk:{t['id']}:{turn['id']}:a{i}", f"talk {t['title']} turn {turn['id']} answer {i}", a["jp"].replace(" ", ""), a.get("romaji", ""), a["en"]
    for key, name in (("n5", "exam.json"), ("n4", "exam_n4.json")):
        for i, q in enumerate(load(name)["questions"], 1):
            jp = q["prompt"] + (" " + q["passage"] if q.get("passage") else "") + " [" + " / ".join(q["options"]) + "]"
            yield f"exam:{key}:q{i}", f"{key.upper()} exam {q['section']} q{i}", jp, "", q.get("explain", "")

def main(argv):
    if "--check" in argv:
        # shortcut: counts only; a stricter content check belongs to ContentCheckTest.
        all_rows = list(rows())
        ids = [r[0] for r in all_rows]
        assert len(all_rows) > 3000, len(all_rows)
        assert len(set(ids)) == len(ids), "duplicate ids"
        assert all(r[2].strip() for r in all_rows), "empty japanese"
        # a dropped source fails: (id prefix or suffix test, minimum rows)
        mins = {"course:": 3000, "story:": 100, "talk:": 100, "exam:n5:": 50, "exam:n4:": 50, "scenario:": 50}
        for prefix, low in mins.items():
            assert sum(i.startswith(prefix) for i in ids) >= low, prefix
        for name, test, low in (("kanji words", lambda i: re.search(r":w\d+$", i), 500), ("examples", lambda i: i.endswith(":ex"), 300), ("grammar notes", lambda i: i.endswith(":p1:note") or re.search(r":p\d+:note$", i), 50), ("theory", lambda i: re.search(r":t\d+$", i), 10), ("tips", lambda i: i.startswith("course:tip:"), 1)):
            assert sum(bool(test(i)) for i in ids) >= low, name
        print(f"ok: {len(all_rows)} rows, {len(set(ids))} unique ids")
        return
    with open(argv[0], "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(["id", "where", "japanese", "romaji", "english", "correction"])
        for r in rows():
            w.writerow([*r, ""])

if __name__ == "__main__":
    main(sys.argv[1:])
