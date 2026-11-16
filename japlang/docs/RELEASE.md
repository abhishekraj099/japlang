# Releasing Manabi

## 1. Create the release key (once)

Keep this file and its passwords safe and backed up. If you lose it you can no longer update the app on Google Play
(unless you use Play App Signing, which is recommended).

```sh
keytool -genkeypair -v -keystore manabi-release.jks -alias manabi \
  -keyalg RSA -keysize 4096 -validity 10000
```

Put `manabi-release.jks` in the project folder (next to `settings.gradle.kts`) and create `keystore.properties` beside it:

```properties
storeFile=manabi-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=manabi
keyPassword=YOUR_KEY_PASSWORD
```

Both files are listed in `.gitignore`. Without `keystore.properties` the release build is signed with the debug key,
which is fine for testing but cannot be uploaded to Google Play.

## 2. Fill in the legal details

In `app/src/main/assets/config.json`, under `legal`:

- `contactEmail`: the address people can write to about privacy. Google Play requires one.
- `privacyUrl`: the public web address of the privacy policy (see step 3).
- `effectiveDate`: change it whenever the policy text changes.

## 3. Publish the privacy policy

Google Play needs a public URL. `docs/privacy-policy.md` is the same text as the in-app policy
(`app/src/main/assets/legal/privacy.md`), with the date and contact address filled in when you run:

```sh
python3 tools/legal.py   # writes docs/privacy-policy.md from the asset and config.json
```

Host it anywhere public, for example GitHub Pages (Settings → Pages → deploy from the `docs` folder) or a Google Site,
and paste the URL into `privacyUrl` and the Play Console (App content → Privacy policy).

## 4. Build

```sh
./gradlew clean testDebugUnitTest
./gradlew bundleRelease     # app/build/outputs/bundle/release/app-release.aab, for Google Play
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk, for sideloading
```

Before every release, raise `versionCode` (and usually `versionName`) in `app/build.gradle.kts`.

Release builds are shrunk with R8. If a new library crashes only in release builds, add a keep rule to
`app/proguard-rules.pro`; the crash log (Settings → Privacy & data → Send a crash report) shows the stack trace.

## 5. Play Console

- App content → Data safety: answer as in `docs/play-data-safety.md`.
- App content → Target audience: 13 and over (the policy says the app is not directed at children).
- Store listing: include the KanjiVG attribution if you show stroke-order screenshots (CC BY-SA 3.0).

## Native-speaker review

`python3 tools/review_sheet.py review.csv` exports every lesson text (practice items and sentences, dialogues,
phrases, stories, talks, both mock exams, kanji example words) with a stable `id`. Give it to a native speaker: they
fill in the `correction` column only where something is wrong or unnatural, and you apply fixes by `id`.
`--check` self-tests the export.

## Still needed before a public launch

- A contact email and hosted policy URL (steps 2 and 3).
- Native-speaker recordings (optional but recommended): see `docs/AUDIO.md`.
- Online crash reports (optional): add Firebase Crashlytics, then update the privacy policy and data-safety form.
