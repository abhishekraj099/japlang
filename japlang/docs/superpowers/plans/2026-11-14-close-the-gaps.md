# Close the Gaps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deepen Manabi to the level of the big Japanese apps: full content depth for all Basic units, grammar review, graded reading stories, "Talk with {name}", N4 kanji and a full N5 mock test.

**Architecture:** Content stays in JSON assets loaded by `data/` singletons (Course, Exam, plus new Stories and Talks); exercises are built in `data/Exercises.kt`; sessions run through `ui/LessonScreen.kt` and finish through `Progress.finish(...)`. New screens follow the existing SubScreen/Panel/PushButton components.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, org.json, JUnit 4 (JVM unit tests loading assets via `ContentTest.asset(name)`).

**Spec:** `docs/superpowers/specs/2026-11-14-close-the-gaps-design.md` (read its "Decisions after the builder check").

## Global Constraints

- Project root `/Users/abhishekraj/Abhishek/Android/japlang`; git root `/Users/abhishekraj/Abhishek/Android`; branch `kanji-voices-privacy`.
- Test/build: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" sh ./gradlew -q :app:testDebugUnitTest :app:assembleDebug` (results `app/build/test-results/testDebugUnitTest/*.xml`; compile errors print as `e:` lines).
- JSON assets are edited by a Python script that loads and dumps (indent 2, `ensure_ascii=False`); never hand-splice. Existing tests stay green; a count assertion may move only to its new true value.
- Sentence shape: `{"tokens": [...], "romaji": "...", "en": "...", "blank": i, "distract": [...]}`; `blank`/`distract` optional together; 2–3 distractors, none equal to `tokens[blank]` or also correct given `en`. Romaji style: `ou`/`uu`/`ei` long vowels, particles `wa`/`o`/`e`, words space-separated.
- Dialogue shape: as unit `b1`'s `dialogue` (`title`, `lines[]` with `speaker`, `jp` (words space-separated), `romaji`, `en`, optional `heard`).
- "Known material" for a unit = items, sentences, phrases, dialogue lines and grammar notes of Basic units up to and including it (order: `courses[0].levels[*].units`), kana forms of those words, particles/copula already used there.
- Japanese must be natural, polite (です/ます), correct, and match its English; never ship a line you are unsure of — replace it with a simpler correct one. English meanings within a unit must be distinct (they are each other's wrong options).
- Kotlin matches surrounding style: short KDoc on why, no new libraries, no new permissions, no new network calls. New screens work in light and dark themes.
- Commits: `git -c user.name="Abhishek Raj" -c user.email="abhisraj099@gmail.com" commit`, message ending with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

- A unit whose practice set has no `note`: grammar review must skip it, not crash (test in Task 4).
- Speech recogniser returns kanji, digits or extra spaces for a correct spoken answer: Talk matching must still accept it (test in Task 6).
- Microphone permission denied / no recogniser: Talk must stay completable by tapping an answer (manual check listed in Task 6).
- A story or talk unlocked by a unit the learner skipped via test-out: unlock must count test-out completion (`<unit>-check` in `progress.done`) (test in Task 5).
- Grammar ids already in a learner's memory after content edits: ids must never change once written (test in Task 4 asserts ids are explicit in course.json, not computed).

---

### Task 1: Content depth — Basic units 11–26

Units: `time2, dir, traf, days, life1, rel1, sport, life2, rel2, colors, look, clothes, comp2, adj, taste, ability`.

**Files:** Modify `app/src/main/assets/course.json`; Test `app/src/test/java/com/example/jap/ContentTest.kt`.

- [ ] **Step 1:** Generalise the existing test `firstTenBasicUnitsHaveSixSentencesPerSet` and `firstTenBasicUnitsHaveAConversation` into `basicUnitsHaveSixSentencesPerSet` / `basicUnitsHaveAConversation` driven by a list `contentReadyUnits` (first ten + this task's 16). Each set: ≥6 sentences, ≥2 with `blank >= 0`; each unit: dialogue lesson `"$id-talk"` with ≥6 lines.
- [ ] **Step 2:** Run the tests — expect FAIL on the new units.
- [ ] **Step 3:** Add sentences to every practice set of these units up to exactly 6 (keep existing ones unchanged) and give each unit without a dialogue one of 6–8 lines; extend existing dialogues shorter than 6 lines to 6 (original lines unchanged, in order, speakers alternating). Sentences practise the set's grammar note.
- [ ] **Step 4:** Run the full suite — expect PASS.
- [ ] **Step 5:** Commit "Content depth: Basic units 11–26".

### Task 2: Content depth — Basic units 27–42

Units: `travel, reason, location, countobj, counttime, pastadj, compare, verbs, desire, purpose, groups, teform, cont, perm, conn, seq`. Same steps as Task 1, appending these ids to `contentReadyUnits`. Commit "Content depth: Basic units 27–42".

### Task 3: Content depth — Basic units 43–57

Units: `nai, oblig, cap1, hobby, ta, listing, indirect, nounmod, when, givetake, even, ndesu, advice, cap2, cond`. Same steps as Task 1; after this task `contentReadyUnits` is every Basic unit, so replace the list with the Basic course's unit list. Commit "Content depth: Basic units 43–57".

### Task 4: Grammar review

**Files:** Modify `app/src/main/assets/course.json` (add `"id"` to every practice `note`), `data/Models.kt`, `data/Course.kt`, `data/Exercises.kt`, `data/Review.kt`, `ui/App.kt`; Create test `app/src/test/java/com/example/jap/GrammarReviewTest.kt`.

**Interfaces:**
- Produces: `Note(title, pattern, body, id: String = "")` (id last so existing positional calls still compile); `data class GrammarPoint(val id: String, val note: Note, val sentences: List<Sentence>, val unitId: String)` in Models.kt; `Course.grammar: List<GrammarPoint>`; `Course.grammarPoint(id: String): GrammarPoint?`; `Exercises.forGrammar(points: List<GrammarPoint>): List<Exercise>`; `Review.dueGrammar(srs: Map<String, Srs>, now: Long = System.currentTimeMillis()): List<GrammarPoint>` (due ≤ now, most overdue first).

- [ ] **Step 1: Write failing tests** in GrammarReviewTest:
  - `everyNoteHasAStableId`: every practice `note` in course.json (raw JSON) has a non-empty `"id"` matching `g-<unitId>-<n>` (n = 1-based set index) and ids are unique.
  - `practiceLessonsTeachTheirGrammarPoint`: for a practice lesson with a note, at least one exercise from `Exercises.forLesson` has `itemId == note.id`.
  - `forGrammarAsksEachPointOnce`: `Exercises.forGrammar(points)` returns `points.size` exercises, each a `Choice` whose `itemId` is its point's id; a fill-in when the point has a blank sentence.
  - `dueGrammarSkipsItemsAndUnknownIds`: srs with an item id, an unknown id and a due grammar id → only the grammar point returned; a not-yet-due grammar id excluded.
- [ ] **Step 2:** Run — expect FAIL (no ids / functions).
- [ ] **Step 3:** Script-add `"id": "g-<unitId>-<n>"` to every practice note (all units with practices); parse it in `Course.note`; build `Course.grammar` from practice lessons with a note (sentences = that set's sentences).
- [ ] **Step 4:** In `Exercises.practice`, set `itemId = note.id` on the lesson's fill-in-the-blank and comprehension Choices (so finishing the lesson creates the grammar memory through the normal `results`). Implement `forGrammar` (prefer a random blank sentence → `fillBlank`, else `translate` against the point's unit sentences). Implement `Review.dueGrammar`.
- [ ] **Step 5:** In `ui/App.kt` `revision(due, items)`: when `due`, append `Exercises.forGrammar(Review.dueGrammar(progress.srs).take(GRAMMAR_PER_SESSION))` with `GRAMMAR_PER_SESSION = 3`; keep "Keep going" behaviour (it calls `revision` again). The Practice "Start revision" button must also be enabled when only grammar is due.
- [ ] **Step 6:** Run the full suite and build — expect PASS.
- [ ] **Step 7:** Commit "Grammar review: grammar points have their own memory".

### Task 5: Graded reading stories

**Files:** Create `app/src/main/assets/stories.json`, `data/Stories.kt`, `ui/StoryScreen.kt`, test `app/src/test/java/com/example/jap/StoriesTest.kt`; Modify `data/Content.kt` (load), `ui/App.kt` (pages), `ui/PracticeScreen.kt` (entry), `ui/LearnScreen.kt` (unit sheet link when the unit's story is unlocked).

**Interfaces:**
- Produces: `data class Story(val id: String, val title: String, val unlockAfter: String, val lines: List<StoryLine>, val questions: List<Question>)`, `data class StoryLine(val speaker: String, val tokens: List<String>, val romaji: String, val en: String)`; `object Stories { val all: List<Story>; fun load(json: JSONObject); fun unlocked(done: Set<String>): List<Story> }` — unlocked when `"${unlockAfter}-check"` ∈ done.
- JSON: `{"stories": [{"id": "st-<unit>", "title", "unlockAfter": "<unitId>", "lines": [{"speaker", "tokens", "romaji", "en"}], "questions": [{"prompt", "options", "explain"}]}]}` — first option correct, as in exam.json.

- [ ] **Step 1: Write failing tests**: `storiesLoad` (≥19 stories, one per 3 Basic units: unlockAfter = Basic units #3, #6, …, #57); `everyStoryIsReadable` (8–12 lines, 3–4 questions, ≥3 distinct options each); `storiesUseOnlyKnownWords` (every token, ignoring punctuation-only tokens, is in the known-token set for its unlock unit — sentence tokens, dialogue words (jp split on spaces), item jp and readings of Basic units up to it — or in an explicit `allowedForms` set in the test that lists conjugated forms, each with a comment naming the known base word); `unlockCountsTestOut` (done = {"b2-check"} unlocks a story with unlockAfter "b2").
- [ ] **Step 2:** Run — expect FAIL.
- [ ] **Step 3:** Write stories.json (19 stories), `Stories`, load it in `Content.load`.
- [ ] **Step 4:** `StoryScreen(story, onQuiz, onBack)`: lines as Panels (tap → speak joined tokens), top toggles for romaji and English (default English hidden), "Check your understanding" → `onQuiz`. In App.kt add `Page.Stories` (list of unlocked stories, locked ones greyed with "Finish <unit title>") and `Page.Story(story)`; the quiz runs as `Session(story.title, story.id, color, Config.practiceXp, SessionKind.PRACTICE, questions as Choice)`, so finishing marks `story.id` done and awards XP. Practice screen gets a "Stories" card; the unit sheet shows "Read the story" when that unit's story is unlocked.
- [ ] **Step 5:** Run the full suite and build — expect PASS.
- [ ] **Step 6:** Commit "Graded reading stories".

### Task 6: Talk with {name}

**Files:** Create `app/src/main/assets/talks.json`, `data/Talks.kt`, `ui/TalkWithGuideScreen.kt`, test `app/src/test/java/com/example/jap/TalksTest.kt`; Modify `data/Content.kt`, `ui/App.kt`, `ui/PracticeScreen.kt`.

**Interfaces:**
- Produces: `data class TalkAnswer(val jp: String, val romaji: String, val en: String, val heard: List<String>, val next: String?)`, `data class TalkTurn(val id: String, val jp: String, val romaji: String, val en: String, val answers: List<TalkAnswer>)`, `data class GuideTalk(val id: String, val title: String, val unlockAfter: String, val start: String, val turns: List<TalkTurn>)`; `object Talks { val all: List<GuideTalk>; fun load(json: JSONObject); fun unlocked(done: Set<String>): List<GuideTalk>; fun match(said: List<String>, answers: List<TalkAnswer>): TalkAnswer? }`.
- `match` normalises both sides (NFKC, drop whitespace and 、。？！?!, full-width digits → ASCII) and accepts an answer when any recogniser alternative equals its `jp` or any `heard` variant after normalising; first matching answer wins.

- [ ] **Step 1: Write failing tests**: `talksAreWellFormed` (≥8 talks, unlock spread across Basic units, 4–8 turns reachable from `start`, every `next` is null or an existing turn id, every turn has ≥2 answers); `matchAcceptsVariants` (spaces/punctuation/full-width digits ignored; a `heard` kanji variant matches; an unrelated sentence returns null); `talksUseOnlyKnownWords` (same known-token rule as Task 5, applied to answer jp split on spaces and turn jp split on spaces).
- [ ] **Step 2:** Run — expect FAIL.
- [ ] **Step 3:** Write talks.json (≥8 talks), `Talks`, load it.
- [ ] **Step 4:** `TalkWithGuideScreen(talk, progress, onDone)`: GuideCharacter on one side with a speech bubble showing the turn (romaji/English under it, toggle), the guide says the turn with the Japanese `Speaker` set to the chosen guide's character voice (`Guide.voiceStyle(Guide.CHARACTER_VOICE + Guide.current.id, "ja", null)`; restore `speaker.useJapaneseVoice(progress)` on dispose); a mic button using `rememberSpeechInput`; on a match show the learner's line as their bubble and go to `next` (null → finish); on no match show "Try again" and a hint button (romaji + English of the first answer); after 2 misses, or when speech is unavailable/denied, show the answers as tappable choices. Finishing calls `progress.finish(talk.id, Config.practiceXp, emptyMap(), SessionKind.PRACTICE)` and returns. Title "Talk with ${Guide.name}". Practice screen gets a "Talk with ${Guide.name}" card listing unlocked talks.
- [ ] **Step 5:** Run the full suite and build — expect PASS. Manual check (record in report): install on emulator-5554, deny microphone, complete a talk by tapping answers.
- [ ] **Step 6:** Commit "Talk with your guide: spoken mini-conversations".

### Task 7: Kanji N4

**Files:** Modify `app/src/main/assets/course.json` (units `kj11`…, a second level "Kanji N4" in course `kanji`), `app/src/main/assets/strokes.json` (via `python3 tools/strokes.py <cache-dir>`); Test `WritingTest.kt` / `ContentTest.kt`.

- [ ] **Step 1: Write failing test** `kanjiN4IsComplete`: the `kanji` course has a level named "Kanji N4" with units of 10 kanji (last may be shorter), ≥160 kanji in total, none duplicating N5 or each other, each with meaning, on or kun, ≥2 `words` (jp, reading, romaji, en) and stroke data (`Strokes.of`).
- [ ] **Step 2:** Run — expect FAIL.
- [ ] **Step 3:** Add the standard JLPT N4 kanji (~170) in themed units of 10 with the same item shape and lesson structure as N5 units (two KANJI lessons of 5, explicit ids `j-<char>`, unit titles/subtitles/colours), then run `tools/strokes.py`.
- [ ] **Step 4:** Update count assertions that moved (KANJI item count, written count, revision count, kanji lesson count) to their new true values; run the full suite — expect PASS.
- [ ] **Step 5:** Commit "Kanji N4".

### Task 8: Full JLPT N5 mock test

**Files:** Modify `app/src/main/assets/exam.json`; Test `ContentCheckTest.kt`.

- [ ] **Step 1: Write failing test** `mockTestIsAFullN5Paper`: ≥60 questions; per section at least Vocabulary 20, Grammar 20, Kanji 10, Reading 10; every Reading question has a non-empty `passage`; `minutes` = 60; each question's first option appears exactly once.
- [ ] **Step 2:** Run — expect FAIL.
- [ ] **Step 3:** Keep the existing 20 questions; add questions in the JLPT N5 style (Reading questions share 3–4 short passages), grouped by section in file order; set `minutes` to 60.
- [ ] **Step 4:** Run the full suite — expect PASS.
- [ ] **Step 5:** Commit "Full JLPT N5 mock test".
