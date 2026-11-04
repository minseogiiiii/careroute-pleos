#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?Set ANDROID_HOME}"
: "${CAREROUTE_KEYSTORE:?Use the same private signing key as the installed release app}"
: "${CAREROUTE_STORE_PASSWORD:?Set signing password}"
tools="$ANDROID_HOME/build-tools/35.0.0"
platform="$ANDROID_HOME/platforms/android-35/android.jar"
classes="app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes"
if [ ! -d "$classes" ]; then classes="build/sdk-release/classes"; fi
out="build/visual-test"
mkdir -p "$out/classes" "$out/dex"
"$tools/aapt2" link -I "$platform" --manifest tests/android/AndroidManifest.xml -o "$out/resources.apk"
javac -encoding UTF-8 -source 17 -target 17 -classpath "$platform:$classes" -d "$out/classes" tests/visual/JudgeInstrumentation.java
find "$out/classes" -name '*.class' > "$out/classes.txt"
"$tools/d8" --release --min-api 28 --lib "$platform" --output "$out/dex" @"$out/classes.txt"
python3 - <<'PY'
from pathlib import Path
from zipfile import ZipFile
from shutil import copyfile
p=Path('build/visual-test');copyfile(p/'resources.apk',p/'unsigned.apk')
with ZipFile(p/'unsigned.apk','a') as z:
 for d in (p/'dex').glob('*.dex'):z.write(d,d.name)
PY
"$tools/zipalign" -f -p 4 "$out/unsigned.apk" "$out/aligned.apk"
export CAREROUTE_KEY_PASSWORD="${CAREROUTE_KEY_PASSWORD:-$CAREROUTE_STORE_PASSWORD}"
"$tools/apksigner" sign --ks "$CAREROUTE_KEYSTORE" --ks-key-alias "${CAREROUTE_KEY_ALIAS:-careroute-judge}" --ks-pass env:CAREROUTE_STORE_PASSWORD --key-pass env:CAREROUTE_KEY_PASSWORD --out "$out/CareRoute-ui-test.apk" "$out/aligned.apk"
"$tools/apksigner" verify "$out/CareRoute-ui-test.apk"
echo "$out/CareRoute-ui-test.apk"
