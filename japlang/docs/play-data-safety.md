# Google Play Data safety answers

These answers match what the app does in version 1.1. Update them, and the privacy policy, if that changes
(for example online accounts, cloud sync or Crashlytics).

## Data collection and security

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **Yes** (see below: audio is processed by Google's speech service; app-interaction data by the YouTube player) |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (Google services use HTTPS) |
| Do you provide a way for users to request that their data is deleted? | **Yes**: Settings → Privacy & data → Delete all my data, or uninstall |

Data that never leaves the phone (name, email, progress, usage log, crash log) is **not** "collected" in Play's
definition, so it is not declared.

## Data types

| Data type | Collected | Shared | Why | Optional |
| --- | --- | --- | --- | --- |
| Audio → Voice or sound recordings | Yes, processed ephemerally by the device speech recogniser (Google) | No | App functionality (pronunciation check) | Yes |
| App activity → App interactions | Yes, by the embedded YouTube player when a video is watched | No | App functionality | Yes |
| App info and performance → Diagnostics | Only if the learner chooses to email a crash report | No | App functionality | Yes |

Handwriting is recognised on the device by ML Kit; strokes are not uploaded. Google's ML Kit SDK may send
limited usage and diagnostics data to Google; see developers.google.com/ml-kit/android-data-disclosure and
reflect the current disclosure in the form.

## Permissions to explain

- `RECORD_AUDIO`: speaking practice.
- `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`: the optional daily reminder.
- `INTERNET`, `ACCESS_NETWORK_STATE`: YouTube videos and the one-time handwriting model download.
