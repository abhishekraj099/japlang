# Native-speaker audio

Every Japanese word, sentence and character is spoken by the phone's text-to-speech engine unless a recording
of it ships with the app. Recordings always win, so they can be added a few at a time.

## Adding recordings

1. Record each line as a short mono file (`.m4a` AAC at 64–96 kbps, or `.ogg`/`.opus`). Trim silence at both ends.
2. Put the files in `app/src/main/assets/audio/`.
3. List them in `app/src/main/assets/audio/index.json`, mapping the exact Japanese text the app speaks to the file:

```json
{
  "おはようございます": "ohayou-gozaimasu.m4a",
  "学生": "gakusei.m4a",
  "あ": "h-a.m4a"
}
```

Spaces in the text are ignored when matching. The text to use is what the lesson speaks: an item's `say`, else its
`reading`, else its `jp`; a sentence's tokens joined together; a dialogue line without spaces.

To get the full list of lines to record:

```sh
python3 tools/audio_lines.py > lines.tsv   # every spoken text, with the lesson it is in
```

Audio files are stored uncompressed in the APK (`noCompress` in `app/build.gradle.kts`) so they play instantly.
The course has about 1,600 lines; as short clips at 64 kbps they add roughly 12–16 MB.

## Licensing

Only ship recordings you own or have a licence for, and credit the voice actors in `assets/legal/licenses.md`.
