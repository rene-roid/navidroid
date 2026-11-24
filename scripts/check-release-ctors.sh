#!/bin/sh
# Usage: scripts/check-release-ctors.sh app/build/outputs/apk/github/release/app-github-release.apk
# Fails when R8 stripped the constructor of a class that Room or WorkManager creates by reflection,
# which crashes release builds while debug builds work. Rules live in app/keep-rules.pro.
set -e
apk=$(realpath "$1")
bt=$(ls -d "${ANDROID_HOME:-$HOME/Android/Sdk}"/build-tools/* | tail -1)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
(cd "$tmp" && unzip -q "$apk" 'classes*.dex')
for d in "$tmp"/classes*.dex; do "$bt/dexdump" "$d" 2>/dev/null; done | awk '
/Class descriptor/ { if (cls != "" && reflect && !init) { print "missing <init>: " cls; bad = 1 } cls = $4; reflect = (cls ~ /InputMerger;|Worker;|_Impl;/); init = 0; if (reflect) n++ }
/Superclass/ && /InputMerger|Worker;|RoomDatabase|WorkDatabase/ { if (!reflect) n++; reflect = 1 }
/name +: .<init>./ { init = 1 }
END { if (cls != "" && reflect && !init) { print "missing <init>: " cls; bad = 1 } print n " reflection classes checked"; exit bad }'
