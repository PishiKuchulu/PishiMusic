# Contributing to Tuned

Bug reports, fixes and features are welcome. Open an issue first for anything large, so we can agree on the approach before you build it.

## Setup

You need Android Studio with JDK 21, the Android SDK with platform 37 and the NDK, Python 3 and Git.

```bash
git clone https://github.com/PishiKuchulu/PishiMusic.git
cd Tuned
./gradlew assembleDebug
```

Put `sdk.dir` in `local.properties`. Optional build settings go in `.env`; copy `.env.example` to start.

## How the code is laid out

- `app/src/main/java/com/tuned/app/ui`: Compose screens, by feature, with the design tokens in `ui/theme`.
- `playback`: the player service, Media3 ExoPlayer and the bridge to the native engine in `app/src/main/cpp`.
- `data`: YouTube Music, Last.fm, lyrics providers, Room and DataStore.
- `widget`: the home-screen widgets.
- `audio/decent-usb-audio-driver`: the USB DAC driver.

Names of data saved on the phone are kept in `data/StorageNames.kt`. Don't rename them: users would lose what is stored under the old name.

## Pull requests

1. Branch from `main`: `feat/short-name` or `fix/short-name`.
2. Run the tests: `./gradlew testDebugUnitTest`.
3. Keep user-facing text in `res/values/strings.xml` so it can be translated.
4. Describe what changed and how you tested it. The Build workflow has to pass.

Keep secrets and API keys out of the source; they belong in `.env` or `local.properties`.
