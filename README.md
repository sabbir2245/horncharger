# HornCharger

A tiny, no-nonsense Android app that plays a sound when your **charger is plugged in** or **headphones are connected**. Bring your own audio clip, set the volume, done.

Built to be lightweight: a single Activity, no AndroidX, no third-party libraries, ~430 KB APK (most of which is the bundled sample sound).

## Features

- **Charger plug-in sound** — plays a chosen clip when the charger is connected.
- **Headphone connect sound** — plays when headphones are connected (USB-C or 3.5 mm).
- **Independent toggles** — charger and headphone playback can each be turned on/off.
- **Import your own audio** — pick any local audio file; it is copied into the app's private storage.
- **Volume control** — slider to adjust playback volume.
- **Plays with the app closed** — a small foreground service keeps the charger/headset listener alive.
- **Auto-start on boot** — the listener comes back after a reboot.

## How it works

- A foreground service (`PlayService`) registers a runtime `BroadcastReceiver` for:
  - `android.intent.action.ACTION_POWER_CONNECTED`
  - `android.intent.action.HEADSET_PLUG`
  - `android.intent.action.USB_HEADSET_PLUG`
- When an event fires, it plays the selected audio (or the bundled default) for at most **5 seconds**, at the chosen volume.
- `Prefs` stores the toggles and volume in `SharedPreferences`; the chosen audio is copied into the app's internal files directory.
- `BootReceiver` restarts the service after reboot if anything is enabled.

## Requirements

- Android 6.0 (API 23) or newer.
- A device/emulator with Android SDK Platform 34 and Build-Tools 34.0.0.

## Build

```bash
# Debug
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk

# Release (signed if keystore.properties is present)
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

Point Gradle at your SDK via `ANDROID_HOME` or a `local.properties` file (`sdk.dir=/path/to/Android/Sdk`).

### Release signing

Create `keystore.properties` in the project root (it is git-ignored):

```properties
storeFile=release.keystore
storePassword=yourpassword
keyAlias=horncharger
keyPassword=yourpassword
```

Generate a keystore if you don't have one:

```bash
keytool -genkeypair -v -keystore release.keystore -alias horncharger \
  -keyalg RSA -keysize 2048 -validity 10000
```

If `keystore.properties` is absent, the release build falls back to the debug key so it still installs.

## Install

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Or download the APK from the [Releases](../../releases) page and open it on your phone (allow installing from unknown sources).

## Usage

1. Open the app.
2. Tap **Import audio file** to pick your clip (optional — a default sound is bundled).
3. Use **Preview** to test and the slider to set the volume.
4. Enable **Play on charger plug-in** and/or **Play on headphone connect**.
5. Plug in. It plays.

A low-priority "HornCharger" notification stays visible while enabled — this is required by Android to keep the listener running in the background.

## Permissions

| Permission | Why |
| --- | --- |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keep the charger/headset listener alive. |
| `POST_NOTIFICATIONS` | Show the required foreground-service notification. |
| `RECEIVE_BOOT_COMPLETED` | Restart the listener after reboot. |
| `WAKE_LOCK` | Keep the device awake briefly while a short clip plays. |

## Notes & limitations

- Fully stopping the app from Settings → App info stops the listener until you reopen the app or reboot (standard Android behavior).
- USB-C headsets that use their own digital DAC may only emit a USB device-attach event; if yours doesn't trigger, open an issue.
- Playback is capped at the first 5 seconds by design.

## License

MIT
