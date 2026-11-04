#!/usr/bin/env bash
# Dependency-free build using the official Android SDK. Same native sources as Gradle.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?Set ANDROID_HOME to an Android SDK containing platform 35 and build-tools 35.0.0}"
: "${CAREROUTE_KEYSTORE:?Set CAREROUTE_KEYSTORE to your private release keystore}"
: "${CAREROUTE_STORE_PASSWORD:?Set CAREROUTE_STORE_PASSWORD}"
tools="$ANDROID_HOME/build-tools/35.0.0"
platform="$ANDROID_HOME/platforms/android-35/android.jar"
out="build/sdk-release"
mkdir -p "$out/classes" "$out/dex" "$out/gen" apk
python3 - <<'PY'
from pathlib import Path
s=Path('app/src/main/AndroidManifest.xml').read_text()
s=s.replace('<manifest ', '<manifest package="com.careroute.firstround" ',1)
s=s.replace('<application ', '<application android:debuggable="false" ',1)
Path('build/sdk-release/AndroidManifest.xml').write_text(s)
PY
"$tools/aapt2" compile --dir app/src/main/res -o "$out/resources.zip"
"$tools/aapt2" link -I "$platform" --manifest "$out/AndroidManifest.xml" --java "$out/gen" --min-sdk-version 28 --target-sdk-version 35 --version-code 102 --version-name 1.0.2-demo -o "$out/resources.apk" "$out/resources.zip"
find app/src/main/java "$out/gen" -name '*.java' > "$out/java-sources.txt"
javac -encoding UTF-8 -source 17 -target 17 -classpath "$platform" -d "$out/classes" @"$out/java-sources.txt"
find "$out/classes" -name '*.class' > "$out/class-sources.txt"
"$tools/d8" --release --min-api 28 --lib "$platform" --output "$out/dex" @"$out/class-sources.txt"
python3 - <<'PY'
from pathlib import Path
from zipfile import ZipFile
from shutil import copyfile
p=Path('build/sdk-release');copyfile(p/'resources.apk',p/'unsigned.apk')
with ZipFile(p/'unsigned.apk','a') as z:
 for d in sorted((p/'dex').glob('*.dex')):z.write(d,d.name)
PY
"$tools/zipalign" -f -p 4 "$out/unsigned.apk" "$out/aligned.apk"
export CAREROUTE_KEY_PASSWORD="${CAREROUTE_KEY_PASSWORD:-$CAREROUTE_STORE_PASSWORD}"
"$tools/apksigner" sign --ks "$CAREROUTE_KEYSTORE" --ks-key-alias "${CAREROUTE_KEY_ALIAS:-careroute-judge}" --ks-pass env:CAREROUTE_STORE_PASSWORD --key-pass "env:CAREROUTE_KEY_PASSWORD" --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out apk/CareRoute_CareRoute_release.apk "$out/aligned.apk"
"$tools/apksigner" verify --verbose --print-certs apk/CareRoute_CareRoute_release.apk
"$tools/zipalign" -c -p 4 apk/CareRoute_CareRoute_release.apk
echo 'Created apk/CareRoute_CareRoute_release.apk'
