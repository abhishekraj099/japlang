# Plan: more sentences and conversations, and endless practice

Spec: none written; this plan comes from the content audit (lessons have 2–3 sentences against roughly 10 in the
big apps; most units have no conversation) and the learner's request for a "never-ending" style like other apps.

Project: Android app (Kotlin, Jetpack Compose) in `japlang/`. Build with
`JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" sh ./gradlew -q :app:testDebugUnitTest`
run from `/Users/abhishekraj/Abhishek/Android/japlang`. All content lives in `app/src/main/assets/course.json`.

## Global Constraints

- Every edit to course.json must keep `ContentTest`, `ContentCheckTest` and `WritingTest` passing. Count assertions
  in tests that change because content grew may be updated to the new true value; nothing else in tests may loosen.
- Keep course.json formatting: 2-space indent, `ensure_ascii=False` (edit it with a Python script that loads and
  dumps the JSON, never by hand-splicing text).
- Sentence objects use the existing shape: `{"tokens": [...], "romaji": "...", "en": "...", "blank": <index>,
  "distract": [...]}`. `blank` and `distract` are optional together; when `blank` is present, `distract` has 2–3
  wrong tokens and none equals `tokens[blank]`. Tokens are split at word and particle boundaries like the existing
  sentences in the same unit; romaji follows the course's style (long vowels as `ou`, `uu`, `ei`; particles written
  `wa`, `o`, `e`; words separated by spaces).
- A new sentence may use only words and grammar the learner has met by the end of that practice set: items of the
  same unit up to and including that set, items of earlier units in the Basic course order, and the unit's grammar
  note. Natural, polite (です/ます) Japanese that a native speaker would actually say.
- Dialogue objects use the existing `dialogue` shape of units that have one (`title`, `lines` with `speaker`, `jp`,
  `romaji`, `en`, optional `heard`, and optional `sentences`). Copy the structure of unit `b1`'s dialogue.
- No new Kotlin features beyond Task 3. Code style matches surrounding code: short KDoc on why, no new abstractions.
- Commit each task with a message ending in `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`, using
  `git -c user.name="Abhishek Raj" -c user.email="abhisraj099@gmail.com" commit`.

## Task 1: Six sentences per practice set in the first ten Basic units

Units: `b1`, `b2`, `fam`, `ani`, `dish`, `comp`, `ask`, `place`, `num`, `time` (in this order in course.json).
Raise every practice set in these units to exactly 6 sentences by adding new ones (keep all existing sentences
unchanged). At least 2 of each set's 6 sentences have a `blank` with `distract`. Sentences practise that set's
grammar note where it has one. For `num` (number kanji sets, no grammar note), write counting and price sentences
using the numbers taught (e.g. "これは 三百円 です。" only if 百 and 円 are taught by then; otherwise kana numbers
like さん).

Add a test in `ContentTest` asserting each practice set of these ten units has at least 6 sentences.

## Task 2: A conversation for every one of the first ten Basic units

Units without a `dialogue` among the ten: `b2`, `comp`, `ask`, `num`, `time`. Give each one a dialogue of 6–8 lines
between two named speakers (vary names; reuse names the course already uses where sensible), set in an everyday
situation that fits the unit's title, using only what the learner knows by the end of the unit (same rule as
Task 1). Add 2 `heard` alternatives on lines whose numbers or kana a speech recogniser may write differently.

Add a test in `ContentTest` asserting each of the ten units has a dialogue with at least 6 lines.

## Task 3: Endless practice ("Keep going")

Practice sessions started from the Practice screen's "Start revision" and "Practise weakest words" buttons end on
the revision summary. Add a "Keep going" button there that immediately starts another session of the same kind,
built fresh at that moment (so it reflects the memory updates just saved), and can be repeated without limit. The
existing "Continue"/finish action stays and still leaves to where the learner came from.

- `Session` (ui/LessonScreen.kt) gets an optional `next: (() -> Session?)?` — null for every other session kind, so
  no other screen changes. The summary shows "Keep going" only when `next` returns a non-empty session.
- In ui/App.kt, the revision/weakest-words launch sets `next` so that it builds the same kind of session again:
  revision → today's remaining due items if any, otherwise the weakest items (the same weakest list "Practise weakest
  words" uses), at most the same session size; weakest-words → the weakest items again.
- The just-finished session's results must be saved (the normal finish path) before the next session is built.
- Add a unit test for whatever pure function picks the next session's items, if the selection logic lives in a
  testable function; if it is a one-line reuse of existing tested code, no new test is needed.
