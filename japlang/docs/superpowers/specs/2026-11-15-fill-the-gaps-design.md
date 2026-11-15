# Fill the remaining gaps — design

Status: approved by the owner on 2026-11-15 ("go for it … make my app top with content and functionality, adaptive learning").

## Intent

The second competitor comparison put Manabi at standard for kana, kanji (N5+N4), the review engine and the N5 mock
test, close on sentences/grammar/conversations/reading, and below on vocabulary size, course reach (stops inside N4),
native audio and native-speaker review. The owner wants every gap that can be filled from our side closed, plus
adaptive learning so practice follows each learner's weak spots, like the big apps' personalised practice.

## Goals

1. **Full N5 vocabulary.** Compare the course's words against a standard JLPT N5 list (~700–800 words); add every
   missing common word (~150–250) in new practice sets named "More words" inside the Basic units whose topic fits
   (≤6 words per set, 6 sentences per set, ≥2 fill-in), same item shape as existing words (jp, reading, romaji,
   meaning, emoji where natural). A test pins the N5 coverage list.
2. **N4 grammar reach.** A new level "Intermediate 2 (N4)" in the Basic course with 12 units, each 2–3 practice
   sets (grammar note + 6 sentences), phrases and a 6–8-line conversation, same shape and checks as existing units:
   volitional (～よう/～ようと思う), plans (～つもり/～予定), possibility (～かもしれません/～でしょう), looks/hearsay
   (～そう/～そうです), seems (～ようです/～みたい), try/regret (～てみる/～てしまう), preparation (～ておく/～てある),
   while/and (～ながら/～し), ease/excess (～やすい/～にくい/～すぎる), passive, causative, ～ば conditional.
3. **JLPT N4 mock test.** ~60 questions (vocabulary, grammar, kanji, reading with passages), 60 minutes, offered
   beside the N5 test (exam asset per level; the Practice card lets the learner pick N5 or N4).
4. **Bigger libraries.** Stories from 19 to ~30 (new ones unlock across N4 units) and guide talks from 11 to ~20,
   same known-material rules and tests.
5. **Adaptive learning.** Track accuracy per skill — reading (recognise/recall/translate), listening (listen/dictate
   audio choices), writing (handwriting), speaking (speech checks), grammar (fill-in/build/grammar items) — from every
   graded answer, stored on the device. A "Smart practice" card at the top of the Practice tab builds a session
   weighted towards the learner's weakest skill and weakest memories (items and grammar), shows which skill it is
   training and why ("Your listening is at 62%"), and works with "Keep going". Skill accuracy is visible on Me.
6. **Native-speaker review sheet.** tools/review_sheet.py exports every Japanese line (sentences, dialogues, stories,
   talks, exam, kanji words) to one CSV with id, Japanese, romaji, English, location and an empty "correction"
   column, so a paid reviewer can mark fixes quickly.

## Non-goals

Recorded audio, online accounts/sync, AI chat, N3+ content.

## Anti-goals

Unnatural or wrong Japanese; untaught material in sentences/stories/talks; anything that sends data off the device;
an adaptive mode that hides why it chose what it chose.

## Constraints

Same as the previous plan's Global Constraints (JSON via load/dump, Japanese quality rules, known-material rule,
guard tests, Kotlin style, commits). Existing ids never change. Pre-release, no migration.

## Approaches considered

- **Adaptive: per-skill accuracy + weighted session (chosen)** — small, explainable, offline.
- Adaptive difficulty inside every lesson (skip/expand exercises live) — larger change to the lesson engine, harder
  to test; later.
- ML/IRT ability model — overkill for current data.

## Order

N5 vocabulary → N4 grammar (2 tasks of 6 units) → N4 mock test → stories & talks → adaptive learning → review sheet.
