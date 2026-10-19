#!/usr/bin/env bash
# Usage: build-apk.sh <mobile|laptop>   (run from repo root; needs SITE_URL)
set -euo pipefail
VARIANT="${1:?mobile|laptop}"
SITE_URL="${SITE_URL:?SITE_URL not set}"
ROOT="$(pwd)"
SRC="$ROOT/android-app"
OUT="$ROOT/apk-out"; mkdir -p "$OUT"

if [ "$VARIANT" = "laptop" ]; then
  APP_ID="com.sujaytumu.nomads.laptop"; APP_NAME="NOMAD Laptop"; LAPTOP=true
else
  APP_ID="com.sujaytumu.nomads"; APP_NAME="NOMAD"; LAPTOP=false
fi

WORK="$(mktemp -d)"; cd "$WORK"
npm init -y >/dev/null
npm i --no-audit --no-fund @capacitor/core@6 @capacitor/cli@6 @capacitor/android@6
mkdir www && echo '<!doctype html><meta charset="utf-8"><title>NOMAD</title><body style="font-family:sans-serif;padding:2rem">Loading NOMAD…</body>' > www/index.html

cat > capacitor.config.json <<JSON
{
  "appId": "$APP_ID",
  "appName": "$APP_NAME",
  "webDir": "www",
  "server": {
    "url": "$SITE_URL",
    "cleartext": false,
    "allowNavigation": ["*"]
  },
  "android": { "allowMixedContent": false, "backgroundColor": "#FBF8F3" }
}
JSON

npx cap add android

# --- icons (use the site's own 512px icon) ---
for d in android/app/src/main/res/mipmap-*; do
  case "$d" in *anydpi*) rm -rf "$d"; continue;; esac
  for n in ic_launcher ic_launcher_round ic_launcher_foreground; do
    cp "$ROOT/frontend/public/icon-512.png" "$d/$n.png"
  done
done

# --- native activity (downloads + laptop mode) ---
PKG_DIR="android/app/src/main/java/$(echo "$APP_ID" | tr . /)"
mkdir -p "$PKG_DIR"
sed -e "s/__APP_ID__/$APP_ID/" -e "s/__LAPTOP__/$LAPTOP/" "$SRC/MainActivity.java.tpl" > "$PKG_DIR/MainActivity.java"

MANIFEST=android/app/src/main/AndroidManifest.xml
# permissions: location (maps/pickup), legacy storage for Android 9 and below
sed -i 's#<uses-permission android:name="android.permission.INTERNET" />#<uses-permission android:name="android.permission.INTERNET" />\n    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />\n    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />\n    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />#' "$MANIFEST"
if [ "$LAPTOP" = true ]; then
  sed -i 's#<activity#<activity android:screenOrientation="sensorLandscape"#' "$MANIFEST"
fi

# --- signing with the repo keystore so updates install over older builds ---
cat >> android/app/build.gradle <<GRADLE

android {
    signingConfigs {
        nomads {
            storeFile file("$SRC/nomads.keystore")
            storePassword "nomads123"
            keyAlias "nomads"
            keyPassword "nomads123"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.nomads
            minifyEnabled false
        }
    }
}
GRADLE

cd android
chmod +x gradlew
./gradlew --no-daemon assembleRelease
cp app/build/outputs/apk/release/app-release.apk "$OUT/nomads-$VARIANT.apk"
ls -la "$OUT"
