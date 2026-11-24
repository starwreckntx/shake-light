#!/usr/bin/env bash
# Builds ShakeLight.apk on a Mac. Run from this folder:  ./build-on-mac.sh
# Installs (via Homebrew, only if missing): Temurin JDK 17 and Android command-line tools,
# then the Android 35 platform + build tools. All free. Nothing is uploaded anywhere.
set -euo pipefail
cd "$(dirname "$0")"

command -v brew >/dev/null || { echo "Install Homebrew first: https://brew.sh"; exit 1; }

if ! /usr/libexec/java_home -v 17+ >/dev/null 2>&1; then
  brew install --cask temurin@17
fi
export JAVA_HOME="$(/usr/libexec/java_home -v 17+)"

brew list --cask android-commandlinetools >/dev/null 2>&1 || brew install --cask android-commandlinetools
export ANDROID_HOME="$(brew --prefix)/share/android-commandlinetools"

yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses >/dev/null || true
sdkmanager --sdk_root="$ANDROID_HOME" "platforms;android-35" "build-tools;35.0.0" "platform-tools"
echo "sdk.dir=$ANDROID_HOME" > local.properties

chmod +x gradlew
./gradlew --no-daemon assembleRelease

cp app/build/outputs/apk/release/app-release.apk ./ShakeLight.apk
shasum -a 256 ShakeLight.apk
echo
echo "Built: $(pwd)/ShakeLight.apk"

# If the phone is plugged in with USB debugging on, install it directly.
ADB="$ANDROID_HOME/platform-tools/adb"
if "$ADB" devices | awk 'NR>1 && $2=="device"' | grep -q .; then
  "$ADB" install -r ShakeLight.apk && echo "Installed on the connected phone."
fi
