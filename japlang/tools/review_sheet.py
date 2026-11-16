"""Writes a CSV of every lesson text for a native-speaker reviewer: python3 tools/review_sheet.py out.csv (--check to self-test)."""
import csv
import json
import sys
from pathlib import Path

assets = Path(__file__).resolve().parent.parent / "app/src/main/assets"

def load(name):
    return json.loads((assets / name).read_text(encoding="utf-8"))

def rows():
    """Yields (id, where, japanese, romaji, english); ids derive from content ids and indices so they stay stable."""
    for u in load("course.json")["units"]:
        uid = u["id"]
        for n, p in enumerate(u.get("practices", []), 1):
            for i, s in enumerate(p.get("sentences", []), 1):
                yield f"course:{uid}:p{n}:s{i}", f"{uid} practice {n} sentence {i}", "".join(s["tokens"]), s.get("romaji", ""), s["en"]
            for i, it in enumerate(p.get("items", []), 1):
                yield f"course:{uid}:p{n}:i{i}", f"{uid} practice {n} {it.get('kind', 'item').lower()} {i}", it.get("reading") or it["jp"], it.get("romaji", ""), it.get("meaning", "")
                for j, w in enumerate(it.get("words", []), 1):
                    yield f"course:{uid}:p{n}:i{i}:w{j}", f"{uid} practice {n} kanji {it['jp']} word {j}", w.get("reading") and f"{w['jp']} ({w['reading']})" or w["jp"], w.get("romaji", ""), w["en"]
        for i, l in enumerate(u.get("dialogue", {}).get("lines", []), 1):
            yield f"course:{uid}:d{i}", f"{uid} dialogue line {i}", l["jp"].replace(" ", ""), l.get("romaji", ""), l["en"]
        for i, ph in enumerate(u.get("phrases", []), 1):
            yield f"course:{uid}:ph{i}", f"{uid} phrase {i}", ph["jp"].replace(" ", ""), ph.get("romaji", ""), ph["en"]
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
        print(f"ok: {len(all_rows)} rows, {len(set(ids))} unique ids")
        return
    with open(argv[0], "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(["id", "where", "japanese", "romaji", "english", "correction"])
        for r in rows():
            w.writerow([*r, ""])

if __name__ == "__main__":
    main(sys.argv[1:])
