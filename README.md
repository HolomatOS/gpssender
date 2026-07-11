# GPS Sender

Tracker app — reads this phone's live GPS location and pushes it to Firebase
Realtime Database under a pairing code, so the GPS Receiver app on another
phone can follow it live.

## One-time setup (before first build)

1. Go to the Firebase console and create a project (or reuse one — see note below).
2. Enable **Realtime Database** (start in test mode for now).
3. In Project Settings, add an Android app with package name `com.holomatos.gpssender`.
4. Download the generated `google-services.json` and place it at `app/google-services.json` in this repo.
5. Commit and push — GitHub Actions will build the APK automatically.

**Important:** GPS Sender and GPS Receiver should be two Android apps
registered inside the **same Firebase project**, so they share one
Realtime Database. Add both package names (`com.holomatos.gpssender` and
`com.holomatos.gpsreceiver`) under the same project, and use the matching
`google-services.json` in each repo.

## How it works

1. Enter any pairing code (e.g. a 6-digit number you make up) on this phone.
2. Tap "START SHARING" and grant location permission.
3. Location is pushed to `locations/{code}` in Firebase every few seconds.
4. Enter the same code in GPS Receiver on the other phone to watch it live.

## Build

GitHub Actions builds a debug APK on every push to `main`. Grab it from the
Actions tab → latest run → Artifacts.

## Next improvements (not yet implemented)

- Foreground service so tracking survives the app being backgrounded/killed
- Firebase Realtime Database security rules (currently wide open in test mode)
- Battery-friendly adaptive update interval
