# Manabi — Japanese Learning App

# ⚠️ THIS PROJECT IS NOT A COPY OF ANY OTHER APP. I AM NOT COPYING OR VIOLATING ANYTHING. I AM ONLY BUILDING THE GENERAL LEARNING FLOW FIRST, AND AFTER THAT I WILL CUSTOMIZE EVERYTHING BASED ON MY OWN NEEDS.

---

## What this means

- **Flow only.** Existing language-learning apps are used purely as a reference for the general flow: units → lessons → exercises → review. That kind of flow is a common pattern, not anyone's property.
- **No copied code.** All code in this repository is written for this project. Nothing is decompiled, extracted, or taken from another app.
- **No copied assets.** No logos, mascots, characters, illustrations, icons, sounds, fonts, or animations from any other app are used.
- **No copied content.** Lesson text, sentences, dialogues, and notes are written for this project. Hiragana, katakana, kanji, and Japanese vocabulary are part of the language itself.
- **No copied branding.** This app does not use the name, colours, or look of any other product, and does not claim to be connected to any other product or company.

## Plan

| Stage | Status | What happens |
| --- | --- | --- |
| 1. Build the base flow | In progress | Get units, lessons, and exercises working end to end |
| 2. Customize | Next | Replace the generic parts with my own design, content, features, and identity |
| 3. Final app | Later | A finished product that is fully my own |

Stage 1 is a starting point, not the final product. The app will change a lot in stage 2.

## About the app

An Android app for learning Japanese, written in Kotlin with Jetpack Compose.

**What it teaches**

- Hiragana and katakana
- Vocabulary
- Kanji
- Grammar notes with example sentences
- Short dialogues

**Exercise types**

- Teaching cards for new items
- Multiple choice, including listening questions
- Matching pairs
- Building a sentence from word tiles
- Typing the answer in romaji
- Writing practice on a drawing pad
- Listen and write: hear a character, write it by hand, and the handwriting is checked
- Speaking practice checked by the phone's speech recognition

**Getting started flow**

- Splash screen and app icon: a red sun rising over seigaiha waves on indigo
- Sign-in with email and password (local only for now: any password is accepted and nothing is sent anywhere)
- Setup: name, daily goal, and how much Japanese you already know
- Adaptive placement test for learners with some knowledge: it starts easy, moves up a level after two correct answers in a row, and lets you skip the units you already know

**App layout**

- **Select course** (tap the top-left button on Learn): Basic with Alphabet / Beginner / Basic / Intermediate levels, plus Travel, Anime, Job interviews, Cuisine and Events & Festivals courses; the choice is remembered
- **Learn**: two levels. Level 1 starts with the Alphabet (Hiragana / Katakana switch: Introduction, each row, Test) and then 19 topic units; Level 2 has 19 more (Colors, Appearance, Clothes, Describing things, Adjectives & adverbs, Taste, Ability, Traveling, Reasons, Location, Counting things, Counting time, Past tense of adjectives, Comparison, Verb ます forms, Wants, Purpose, Verb groups, て-form); Level 3 has 19 grammar units (Continuous, Permission, Connecting, Sequence, ない form, Obligation, Capability 1, Hobby, た-form, Listing actions, Indirect speech, Describing with verbs, When (～時), Give-take, Even if, Explaining (～んです), Suggest, Capability 2, Conditional sentence). Each unit is Theory → Practice 1-3 → Speaking → Writing → Review; tap a unit to see what every lesson covers, or Test out to skip it. Unit conversations live in the Talk tab
- **Alphabet**: Hiragana / Katakana / Combined tabs; a winding road through a Japanese landscape (torii, temple, castle, Mt Fuji) with one stop per character, a test at the end of each row, stops unlocking in order and a certificate at the end (the learner sets the name once, then can view and share the certificate as an image); or a plain table
- **Lesson+**: search everyday situations (self-introduction, visiting a friend's house, convenience store, restaurant, train station, doctor, weather small talk, phone calls); each opens a page of swipeable parts with key vocabulary and sentence patterns, a progress ring and a "Report a problem" sheet. Scenarios live in `assets/scenarios.json` and are searched through `ScenarioSource`, so a server can replace the local file
- **Video**: 43 real Japanese clips (anime, daily life, conversation, education, food, travel, podcasts), including vertical **Shorts** and a row for each **JLPT level N5 to N1**, played with YouTube's embedded player inside the app. YouTube's own controls are hidden and every tap goes to the app's play/pause, so nothing opens the YouTube app. A recommended carousel, a "For beginners" row and one row per topic, search, history, and "See more" with topic and level filters. Each video has a **Subtitle** tab (lines follow playback with the current one highlighted, tap a line to jump, furigana / romaji / English toggles) and a **Speaking** tab (say each line, get a 0-100 score sound by sound, a pronunciation detail sheet that names missing, extra or wrong sounds, and best scores saved per line). Report an error with a reason, and a leave sheet if speaking isn't finished. Clips live in `assets/videos.json` and are read through `VideoSource`
- **Lesson player**: pause / leave sheet, an explain bulb with a note on the word, and display settings (show kanji, reading, romaji, question title, lesson audio, text size)
- **Role-play** (on the Me tab): role-play the conversations: the app speaks the other part, you say yours and speech recognition checks it
- **Me**: profile, shortcuts, this week's XP chart, rank ladder (入門 to 師範), trophies in progress, ranking, learning tips

**Streak and rewards**

- Streak page with a sky header, a sakura tree that blooms as the streak grows, sakura-marked study days on the calendar, streak goals with drop rewards (sakura icon shows the streak)
- Water drops earned from every session, exchanged for omamori that protect the streak for a missed day
- Daily challenge and three daily quests, generated from the date
- Weekly ranking (sample learners until online accounts exist) and trophies

**Other features**

- Spaced-repetition review, word list with search, flashcards
- Speed round, JLPT N5-style mock test with a timer and explanations
- XP, daily goal and an optional daily reminder
- Light and dark theme
- Works offline after the first run; progress is stored on the device

Audio uses the text-to-speech voice installed on the phone, so a Japanese voice must be available. Handwriting checking downloads a Japanese language pack the first time it is used, which needs an internet connection; after that it works offline.

## Where the content lives

Nothing that is taught, and none of the rules, is written in code. It is all JSON in `app/src/main/assets/`:

| File | Holds |
| --- | --- |
| `course.json` | Units, levels, lessons, words, kanji, sentences, grammar notes, dialogues, speaking phrases, topics, curated lessons, learning tips, the kana chart, unit colours |
| `scenarios.json` | Everyday situations for Lesson+: parts, vocabulary with explanations, sentence patterns |
| `videos.json` | Video clips: YouTube id, start and end, topic, level, and timed lines with furigana, kana, romaji and English; topics, home sections, recommended picks, report reasons |
| `exam.json` | Mock test title, time limit, pass mark and questions |
| `config.json` | Daily goals, XP and drop rewards, streak goals, omamori price, ranks, quest and trophy rules, placement levels, review intervals, reminder hour, sample ranking names |

To add a lesson, add it to `course.json`; no code change is needed. `data/Content.kt` is the single place that reads these files, so switching to a server later means changing only that reader.

Screen labels (button text, headings) are still written in the screens themselves.

`videos.json` is generated by `tools/videos/`: `dl.py` downloads a video's Japanese subtitles (written by the uploader), `clips.py`, `clips_more.py` (Shorts and more videos) and `clips_jlpt.py` (JLPT clips, podcasts and each clip's JLPT level) list the chosen clip windows with cleaned lines and English translations, and `build.py` adds furigana, kana and romaji with MeCab (`pip install yt-dlp fugashi unidic-lite`). Videos are embedded, never downloaded, and each subtitle list credits the original video and channel.

## Build and run

Requirements: Android Studio, JDK 11 or newer, Android SDK 37. Minimum supported Android version is 7.0 (API 24).

```
gradlew.bat assembleDebug
```

Or open the folder in Android Studio and press **Run**.

## Project layout

```
app/src/main/java/com/example/jap/
├── JapApplication.kt      Loads the JSON content at start
├── MainActivity.kt        App entry point
├── Reminder.kt            Daily reminder notification
├── audio/Speaker.kt       Text-to-speech
├── ink/Handwriting.kt     Handwriting recognition
├── data/
│   ├── Models.kt          Course, lesson and exercise models
│   ├── Content.kt         Reads the JSON assets
│   ├── Config.kt          Rules and tuning values from config.json
│   ├── Course.kt          The course from course.json
│   ├── Exercises.kt       Turns a lesson into a list of exercises
│   ├── Exam.kt            Mock test from exam.json
│   ├── Placement.kt       Adaptive placement test
│   ├── Daily.kt           Daily challenge and quests
│   ├── League.kt          Weekly points table
│   ├── Videos.kt          Video clips from videos.json
│   ├── Pronunciation.kt   Scores a spoken line sound by sound
│   └── Progress.kt        Account, settings, XP, streak, review schedule
└── ui/
    ├── App.kt             Launch flow, tabs and navigation
    ├── Brand.kt           Wave pattern, emblem, wordmark, splash, seal stamp
    ├── Illustrations.kt   Bonsai, koban and omamori drawings
    ├── Path.kt            Winding path stops used by Learn and Alphabet
    ├── AuthScreen.kt      Sign-in
    ├── OnboardingScreen.kt  First-run setup
    ├── PlacementScreen.kt Placement test
    ├── StreakScreen.kt    Streak intro, calendar, goals and wallet
    ├── LearnScreen.kt     Learn tab
    ├── AlphabetScreen.kt  Alphabet tab
    ├── TopicsScreen.kt    Lesson+ tab
    ├── TalkScreen.kt      Role-play
    ├── VideoScreen.kt     Video tab, See more, history
    ├── VideoPlayerScreen.kt  Clip, subtitles, speaking practice
    ├── YouTubeClip.kt     Embedded YouTube player for one clip
    ├── SpeechInput.kt     In-app speech recognition
    ├── MeScreen.kt        Me tab, trophies, tips, settings
    ├── LessonScreen.kt    Lesson player and every exercise type
    ├── PracticeScreen.kt  Review and practice activities
    ├── WordsScreen.kt     Word list and flashcards
    ├── GameScreen.kt      Speed round
    ├── ExamScreen.kt      Mock test
    ├── LeagueScreen.kt    Ranking
    ├── Components.kt      Shared buttons and cards
    └── theme/             Colours, typography, theme
```

## Disclaimer

This is an independent project. It is not affiliated with, endorsed by, or connected to any other language-learning app or company. Any product names mentioned anywhere in this project belong to their respective owners.
