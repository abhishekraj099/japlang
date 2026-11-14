# Close the gaps with the big Japanese apps — design

Status: approved by the owner on 2026-11-14 ("go for it and also think something out of the box").

## Intent

A competitor comparison (Duolingo, Bunpro, WaniKani, Busuu, LingoDeer, Renshuu) showed Manabi's engine is competitive
(FSRS reviews, handwriting checks, stroke order, offline, guide character) but it is thin and shallow: few sentences
per lesson outside the first 10 units, grammar is never reviewed, no graded reading, kanji stop at N5, and the JLPT
mock test has 20 questions. The owner wants every gap that can be built without outside help closed, so the app feels
as deep as the big players, plus one original feature that sets it apart.

## Goals

1. **Content depth** — every practice set in all 59 Basic units has 6 sentences (≥2 fill-in-the-blank); every Basic
   unit has a conversation of ≥6 lines. Same rules as the first-10-units work (plan 2026-11-14-content-and-endless-practice).
2. **Grammar review** — each grammar point (practice-set `note`) becomes a reviewable item with its own memory
   (FSRS), reviewed with its example sentences (fill-in-the-blank / build / comprehension). Due grammar joins the
   normal revision queue (Practice → Start revision) and the "Keep going" loop.
3. **Graded reading** — short stories (≈8–12 lines) written only with words and grammar the learner has met by a
   given unit, unlocked when that unit is finished, read with tap-to-hear lines, toggles for romaji/English, and 3–4
   comprehension questions at the end. About one story per 3 Basic units to start.
4. **N4 kanji** — a "Kanji N4" level of ~170 kanji (in the existing Kanji course track, after N5), same item shape as
   N5 (meaning, on/kun, 2 example words, stroke order via tools/strokes.py).
5. **Full JLPT N5 mock test** — grow exam.json to a realistic timed N5 mock (vocabulary, grammar, reading, kanji
   sections; ~60–70 questions; keep the existing format).
6. **Talk with Hana (original feature)** — a spoken mini-conversation with the chosen guide: the guide asks a short
   question in Japanese (TTS, character voice), the learner answers by speaking, the answer is matched offline against
   accepted answers (reusing the pronunciation/`heard` matching), and the script branches on the answer. Scripts are
   JSON, gated by unit progress, each 4–8 turns, with a hint button (shows romaji/English of a good answer).

## Non-goals

Native audio recordings and online accounts/cloud sync (need people, money or a server — owner's decision later).
AI/LLM-generated live conversation (would need a backend and per-use cost). Units beyond the existing Basic course.

## Anti-goals

Japanese that is unnatural or wrong (worse than less content); content that uses words the learner hasn't met;
anything that breaks the offline, ad-free, local-only privacy promise (no new network calls, no new permissions
beyond the microphone already used).

## Constraints

All content in JSON assets; course.json edits via load/dump scripts (indent 2, ensure_ascii False). Existing tests
stay green; count assertions may move to new true values only. Kotlin style matches the codebase. Every new feature
works in light and dark themes and with the guide off. Privacy policy stays accurate.

## Approaches considered

- **One big pass over everything** — fastest on paper, but reviews of 2,000+ new lines at once would be shallow.
- **Separate work items in dependency order (chosen)** — content first (stories and Talk-with-Hana reuse its words),
  then engine features, each with its own plan, per-task review and tests.
- **Engine features first, content later** — stories and grammar review would launch on thin content.

## Order

1. Content depth (Basic units 11–59) 2. Grammar review 3. Graded reading 4. Talk with Hana 5. N4 kanji 6. N5 mock test.

## Left to the builder

Story and script topics, exact unlock points, UI layout within existing components, how grammar items are shown in
the Words list, question mix in the mock test. Calls made by the controller are recorded in each plan's ledger.

## Decisions after the builder check (controller's calls; the owner can overrule)

1. **Talk with the chosen guide.** The feature uses whichever guide the learner picked; its title is "Talk with
   {name}" and scripts use the existing `{name}` placeholder (Guide.kt). The Japanese lines are the same for both
   guides; the voice is the guide's character voice. "Guide tips off" only hides tours — the feature stays available.
2. **Grammar items get stable ids.** Each practice-set `note` gains an explicit `"id"` (e.g. `"g-b2-3"`), written once
   by a script and never derived from text or position at runtime. Memory is stored under that id like any item.
   A grammar review uses that practice set's sentences (blank → fill-in, else comprehension/build). Sets without a
   note have no grammar item.
3. **Mock test format.** Questions keep the existing shape and use the optional `passage` field (already in
   `Question`/exam.json loading) for reading items; questions stay grouped by `section` in file order. One total time
   limit set by `minutes` (set to fit the new length, e.g. 60); pass mark stays `passPercent`.
4. **"Met" material and its check.** Known = items, sentences, phrases and dialogue lines of Basic units up to the
   unlock unit (course order), plus particles/copula already used there. Kanji only when a known word is spelled with
   it. Stories and talk scripts are stored as token lists (like sentences) so a test can check every token against a
   known-token set built from that content (with a short reviewed allowlist for conjugated forms). The reviewer still
   reads every line.
5. **Placement and rewards.** Stories open from the Practice screen (a "Stories" entry listing unlocked stories) and
   from a finished unit's sheet; Talk with {name} opens from the Practice screen. Both finish through the normal
   session/XP path (SessionKind.PRACTICE, practice XP), so streaks and daily XP work unchanged; no new quest types.
6. **Speech fallback.** Talk reuses SpeechInput. If the microphone or recogniser is unavailable, or after two misses,
   the learner can tap one of the accepted answers instead (shown as choices). Matching normalises like
   Pronunciation (kana/kanji via known words, ignoring spaces and punctuation) and accepts any listed answer or `heard`
   variant. No-match → "try again" with the hint button.
7. **N4 kanji.** Stroke data comes from KanjiVG through tools/strokes.py (downloads at build time, as for N5). The
   count follows the standard N4 list (~170, flexible). Example words may be new words; each shows its kana reading.
   The N4 level follows N5 in the Kanji track (normal path unlocking).
8. **Quality rule.** Never ship a sentence or line the writer is unsure of; replace it with a simpler correct one.
   Automated checks enforce counts and token rules; reviewers check naturalness.
9. **Saved progress.** The app is pre-release (no store installs), so no migration is needed; existing item ids
   stay unchanged anyway.
10. **Privacy.** Talk uses the same system speech recogniser as the existing speaking practice, already disclosed
    in the privacy policy; no new network use or permission, so no policy change.
