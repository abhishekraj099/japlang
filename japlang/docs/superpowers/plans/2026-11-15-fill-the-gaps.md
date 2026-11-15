# Fill the Gaps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Full N5 vocabulary, 12 N4 grammar units, an N4 mock test, bigger story/talk libraries, adaptive "Smart practice", and a native-speaker review sheet.

**Architecture:** Content stays in JSON assets loaded by `data/` singletons; exercises come from `data/Exercises.kt`; sessions run through `ui/LessonScreen.kt` and save through `Progress.finish`. Adaptive learning adds a small `data/Skills.kt` plus a per-skill answer log in `Progress`, fed from `LessonScreen.advance`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, org.json, JUnit 4; Python 3 for tools.

**Spec:** `docs/superpowers/specs/2026-11-15-fill-the-gaps-design.md` (read "Decisions after the builder check").

## Global Constraints

- Project root `/Users/abhishekraj/Abhishek/Android/japlang`; git root `/Users/abhishekraj/Abhishek/Android`; branch `main` is the base — work on branch `fill-the-gaps`.
- Test/build: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" sh ./gradlew -q :app:testDebugUnitTest :app:assembleDebug` (results in `app/build/test-results/testDebugUnitTest/*.xml`; compile errors print as `e:` lines).
- JSON assets edited by a Python load/dump script (indent 2, `ensure_ascii=False`); never hand-splice. Existing tests stay green; a count assertion may move only to its new true value. Existing ids never change.
- Sentence shape `{"tokens","romaji","en","blank"?,"distract"?}`; every blank's distractors are the same class as the answer (ContentCheckTest `gapOptionsAreTheSameKindAsTheAnswer`), clearly wrong given the English, and the blank sits on the token the set teaches. Romaji style: `ou`/`uu`/`ei`, particles `wa`/`o`/`e`, words space-separated; dialogue `jp` space-separated.
- Japanese must be natural, polite (です/ます unless the set teaches plain forms), correct, and match its English; never ship a line you are unsure of. English meanings within a unit are distinct, and titles are unique across talks, conversations and stories (TalksTest).
- Known material for a unit = items, sentences, phrases, dialogue lines and notes of Basic-course units up to and including it, plus that unit's own new words; kana forms of known words; kanji only if taught (N5/N4) else kana.
- Kotlin matches surrounding style (short KDoc on why), no new libraries, permissions or network calls; new UI works in light and dark.
- Commits: `git -c user.name="Abhishek Raj" -c user.email="abhisraj099@gmail.com" commit`, message ending `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

- A brand-new learner with no answers opens Smart practice: it must still build a session (least-practised skill on learned items, or a friendly "finish a lesson first" when nothing is learned) — test in Task 6.
- A phone without handwriting support (Handwriting.State.UNSUPPORTED) or without a speech recogniser must never get a Smart practice of WRITING or SPEAKING — test in Task 6 (selector takes the unavailable skills as input).
- Skill percentages from very few answers must not be shown or used (≥10 answers, rolling 50) — test in Task 6.
- The N5 mock test's existing best score and "Test taker" trophy keep working after exams become a list — test in Task 4.
- New N4 units must not shift existing unit order/ids or placement results (they are appended as a new level) — test in Task 2.

---

### Task 1: Full N5 vocabulary

**Files:** Modify `app/src/main/assets/course.json`; Create `app/src/test/resources/n5_words.txt` (one word per line: kana reading, optionally `|kanji`); Test `app/src/test/java/com/example/jap/VocabularyTest.kt`.

- [ ] **Step 1:** Write `n5_words.txt` from a standard community JLPT N5 list (~650–800 common words; kana reading per line). Write failing test `n5ListIsCovered`: every listed reading equals the reading (or kana jp) of some WORD item in the course, after normalising (strip spaces; katakana words compared as written).
- [ ] **Step 2:** Run — expect FAIL listing missing words.
- [ ] **Step 3:** Add every missing word in new practice sets titled "More words" (or a short topical title) inside the Basic unit whose topic fits best, ≤6 words per set, each set with 6 sentences (≥2 fill-in) using only known material plus the set's words; no grammar note needed (sets without a note are allowed). Words not taught anywhere sensible may go in a final "Everyday words" unit appended to the Intermediate level.
- [ ] **Step 4:** Update moved count assertions to new true values (WORD count, written count, revision count) and run the full suite + build — expect PASS.
- [ ] **Step 5:** Commit "N5 vocabulary complete".

### Task 2: N4 grammar units 1–6 and the new level

**Files:** Modify `app/src/main/assets/course.json`; Test `ContentTest.kt`.

Units (ids): `vol` (～よう/～ようと思います), `plan` (～つもり/～予定), `maybe` (～かもしれません/～でしょう), `looks` (～そう appearance / ～そうです hearsay), `seems` (～ようです/～みたい), `trydo` (～てみる/～てしまう).

- [ ] **Step 1:** Write failing test `n4LevelExists`: course `basic` has a level named "Intermediate 2 (N4)" with `rank` 3 placed after "Intermediate", containing the six ids in order; every unit has 2–3 practice sets each with a note and 3–5 word items, ≥6 sentences (≥2 blanks), ≥3 phrases and a dialogue of 6–8 lines; the units before the new level are unchanged in order (assert the previous level lists are equal to a hard-coded snapshot taken before editing).
- [ ] **Step 2:** Run — expect FAIL.
- [ ] **Step 3:** Write the six units in the existing unit shape (id, title, subtitle, color, dark, tags, practices with items/note/sentences, phrases, dialogue), with stable grammar note ids `g-<unit>-<n>`.
- [ ] **Step 4:** Update moved counts; full suite + build — expect PASS (contentReadyUnits now includes the new units automatically).
- [ ] **Step 5:** Commit "N4 grammar units 1–6".

### Task 3: N4 grammar units 7–12

Units: `prep` (～ておく/～てある), `while` (～ながら/～し), `ease` (～やすい/～にくい/～すぎる), `passive` (受身形), `causative` (使役形), `ba` (～ば conditional). Same steps as Task 2, extending `n4LevelExists` to all twelve ids. Passive and causative teach their forms in set 1's note. Commit "N4 grammar units 7–12".

### Task 4: JLPT N4 mock test

**Files:** Create `app/src/main/assets/exam_n4.json`; Modify `data/Exam.kt`, `data/Content.kt`, `ui/ExamScreen.kt`, `ui/App.kt` (`Page.Test` gains a paper id), `ui/PracticeScreen.kt` (card offers N5 and N4), `data/Progress.kt` (best score per paper); Test `ContentCheckTest.kt`.

**Interfaces:** `class ExamPaper(val id: String, val title: String, val minutes: Int, val passPercent: Int, val questions: List<Question>)`; `object Exams { val papers: List<ExamPaper>; fun load(id: String, json: JSONObject); fun paper(id: String): ExamPaper? }` with ids `"n5"` (exam.json) and `"n4"` (exam_n4.json); `Progress.bestExam(paperId: String): Int` / `Progress.recordExam(paperId: String, percent: Int)`, where "n5" reads/writes the existing `bestExam` key.

- [ ] **Step 1:** Failing tests: `n4PaperIsAFullTest` (≥55 questions; sections Vocabulary/Grammar/Kanji/Reading each present; Reading questions have passages; minutes 60; options distinct, first option unique); `n5PaperUnchanged` (n5 paper has the same 62 questions as exam.json).
- [ ] **Step 2:** Run — expect FAIL. **Step 3:** Implement Exams + N4 content (N4-level vocabulary/kanji/grammar, natural Japanese). **Step 4:** Full suite + build — PASS. **Step 5:** Commit "JLPT N4 mock test".

### Task 5: More stories and guide talks

**Files:** Modify `app/src/main/assets/stories.json`, `talks.json`; Tests `StoriesTest.kt`, `TalksTest.kt`.

- [ ] Stories: add ~11 (total ≥30) unlocking across Intermediate and the new N4 units; talks: add ~9 (total ≥20) likewise. Update the count/unlock assertions; existing known-words tests must pass; titles unique. Full suite + build; commit "More stories and guide talks".

### Task 6: Adaptive learning — Smart practice

**Files:** Create `data/Skills.kt`; Modify `data/Progress.kt`, `data/Exercises.kt`, `ui/LessonScreen.kt` (`advance`), `ui/PracticeScreen.kt` (Smart practice card at top), `ui/App.kt` (session + Keep going), `ui/MeScreen.kt` ("Your skills" card); Test `app/src/test/java/com/example/jap/SkillsTest.kt`.

**Interfaces:**
- `enum class Skill { READING, LISTENING, WRITING, SPEAKING, GRAMMAR }`
- `object Skills { fun of(exercise: Exercise): Skill?; fun percent(log: List<Boolean>): Int?  // null below 10 answers; fun pick(logs: Map<Skill, List<Boolean>>, unavailable: Set<Skill>): Skill  // lowest percent among skills with ≥10 answers, else least-practised; never in unavailable; const val WINDOW = 50; const val MIN_ANSWERS = 10 }`
- Mapping (spec decision 4): listen-mode Choice → LISTENING; fill-in Choice ("Fill in the blank"), Build and grammar-point Choices (itemId starting `g-`) → GRAMMAR; other Choice, Match, TypeIn → READING; Write, Dictate → WRITING; Speak → SPEAKING; Teach/NoteCard/DialogueCard → null.
- `Progress.skillLog: Map<Skill, List<Boolean>>` (persisted, last WINDOW per skill) and `Progress.recordSkill(skill: Skill, correct: Boolean)`.
- `Exercises.forSmart(skill: Skill, items: List<Item>, grammar: List<GrammarPoint>, phrases: List<Phrase>): List<Exercise>` — ~12 exercises of that skill on the given (weakest) material.

- [ ] **Step 1: Failing tests** in SkillsTest: `mappingCoversEveryExerciseType`; `percentNeedsTenAnswers` (9 → null, 10 → value); `logKeepsLastFifty`; `pickChoosesLowestKnownSkill`; `pickFallsBackToLeastPractised` (empty logs → some skill not unavailable); `pickNeverReturnsUnavailable` (WRITING and SPEAKING unavailable → never returned); `forSmartMatchesSkill` (every graded exercise's `Skills.of` equals the requested skill).
- [ ] **Step 2:** Run — FAIL. **Step 3:** Implement; record skills in `advance` for graded answers only; Smart practice card shows "Training: <skill> · <percent>%" (or "Getting to know your strengths" before data) and starts a SessionKind.PRACTICE session with `next` (Keep going) rebuilt by the same selector; unavailable = WRITING when handwriting isn't READY/supported, SPEAKING when no recogniser; nothing learned → card disabled with "Finish a lesson first". Me gets a "Your skills" card: five rows with bar + percent or "–" under 10 answers.
- [ ] **Step 4:** Full suite + build — PASS. **Step 5:** Commit "Adaptive learning: Smart practice".

### Task 7: Native-speaker review sheet

**Files:** Create `tools/review_sheet.py`; Modify `docs/RELEASE.md` (one paragraph on using it).

- [ ] `python3 tools/review_sheet.py out.csv` writes UTF-8 CSV with header `id,where,japanese,romaji,english,correction` covering practice sentences, dialogue lines, phrases, stories, talks (turns and answers), exam questions (both papers) and kanji example words; ids stable (e.g. `course:<unit>:p<n>:s<i>`). Include an assert-based self-check at the bottom run with `--check` (row count > 3000, unique ids, no empty japanese). Run it; commit "Native-speaker review sheet".
