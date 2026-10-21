"""Writes docs/privacy-policy.md from the in-app policy, filling in the date and contact address from config.json."""
import json
from pathlib import Path

root = Path(__file__).resolve().parent.parent
assets = root / "app/src/main/assets"
legal = json.loads((assets / "config.json").read_text(encoding="utf-8"))["legal"]
contact = legal["contactEmail"] or "the contact address in the app's store listing"
text = (assets / "legal/privacy.md").read_text(encoding="utf-8")
text = text.replace("{date}", legal["effectiveDate"]).replace("{contact}", contact)
(root / "docs/privacy-policy.md").write_text(text, encoding="utf-8")
print("Wrote docs/privacy-policy.md" + ("" if legal["contactEmail"] else " (contactEmail is still empty in config.json)"))
