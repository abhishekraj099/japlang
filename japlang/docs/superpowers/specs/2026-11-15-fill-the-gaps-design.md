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

## Decisions after the builder check (controller's calls; the owner can overrule)

1. **N4 units teach their own words.** Like every existing unit, each practice set has 3–5 new word items (common N4
   vocabulary that the grammar needs); sentences may use them plus everything earlier. This also grows vocabulary
   toward N4. Kanji in new words only if taught (N5/N4 kanji) — otherwise kana.
2. **Verb forms are already taught**: て (teform), ない (nai), た (ta), dictionary (cap1), potential (cap2). Passive and
   causative teach their own forms in their first set's note, the way cap2 teaches the potential form.
3. **Exams.** exam.json stays the N5 paper (unchanged id/content); new exam_n4.json in the same format. Exam becomes
   a list of papers loaded at start (`Exams.papers`, each with `id` "n5"/"n4"); best score is kept per paper
   (`bestExam` stays N5 so existing progress/trophies keep working; N4 has its own key). N4 is open to everyone,
   passPercent 60 like N5, ~60 questions in N5's section ratio, no new certificate.
4. **Skill mapping (one skill per exercise):** listen-mode Choice → LISTENING; fill-in Choice, Build and grammar-point
   exercises → GRAMMAR; other Choice, Match and TypeIn → READING; Write and Dictate → WRITING; Speak → SPEAKING.
   Each skill keeps its last 50 graded answers (rolling); a percentage is shown only after 10 answers; Smart practice
   picks the lowest-percentage skill among those with ≥10 answers, else the least-practised skill.
5. **N5 word list.** No official list exists; the writer reproduces a standard community JLPT N5 list (~700 common
   words) into a test fixture, and the coverage test pins it. The owner can replace the fixture with a list they trust.
