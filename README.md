# Shake Light

Shake the phone twice, quickly, and the flashlight toggles. Runs in the background and with
the screen off through a foreground service (persistent notification with **Toggle light** and
**Stop** buttons). Gives a short buzz when a shake registers.

- Native Kotlin, framework APIs only (no AndroidX, no third-party libraries).
- No INTERNET permission, no analytics, no ads. Torch via `CameraManager.setTorchMode` (no camera permission needed).
- minSdk 26 (Android 8), targetSdk 35. Sensitivity: Gentle 2.0g / Normal 2.6g / Firm 3.2g, two peaks inside 0.8 s, 1.2 s cooldown.
- Signed with the local Android debug key (sideload only, not for the Play Store).

## Build (Mac, one command)

```
./build-on-mac.sh
```

Output: `ShakeLight.apk` in this folder. Needs Homebrew; the script adds JDK 17 and the Android SDK if missing.
Alternative: open the folder in Android Studio, then Build > Build App Bundle(s) / APK(s) > Build APK(s).

## Install on the Galaxy

1. Get `ShakeLight.apk` onto the phone (USB, Quick Share, Google Drive, or email to yourself).
2. Open it from **My Files** (or the app you used to open it).
3. When asked, tap **Settings** and turn on **Allow from this source** for that app, then go back and tap **Install**.
4. If Play Protect warns about an unknown developer, tap **More details > Install anyway**.
5. Open Shake Light, allow notifications, tap **Start shake detection**.
6. For reliable screen-off use: Settings > Apps > Shake Light > Battery > **Unrestricted**.

## Files

- `app/src/main/java/com/hueandlogic/shakelight/ShakeDetector.kt` — shake math
- `Torch.kt` — flashlight on/off, stays in sync with the quick-settings tile
- `ShakeService.kt` — foreground service, wake lock, notification
- `MainActivity.kt` — start/stop, test button, sensitivity, battery settings shortcut

## Undo

Long-press the app icon > Uninstall. Nothing else is changed on the phone.
