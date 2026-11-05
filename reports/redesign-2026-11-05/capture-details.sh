#!/usr/bin/env bash
set -euo pipefail
mkdir -p .ci-bin
cp scripts/qa-adb.py .ci-bin/adb
chmod +x .ci-bin/adb
A=./.ci-bin/adb
$A shell settings put global device_provisioned 1
$A shell settings put secure user_setup_complete 1
$A shell pm disable-user --user 0 com.google.android.googlesdksetup || true
$A shell wm size 1344x2992
$A shell wm density 480
for side in before after; do
  root="/tmp/jarvis-detail-$side"
  $A install -r -g "$root/app/build/outputs/apk/debug/app-debug.apk"
  $A install -r -g "$root/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
  $A shell am instrument -w -e class com.jarvis.food.ReferenceDetailScreenshots com.jarvis.food.test/androidx.test.runner.AndroidJUnitRunner | tee "evidence/detail-$side.log"
  grep -Eq '^OK \(1 test\)' "evidence/detail-$side.log"
  mkdir -p "evidence/$side"
  for name in 04-vitamine 24c-progressi-calorie; do
    $A exec-out run-as com.jarvis.food cat "files/$name.png" > "evidence/$side/$name.png"
  done
done
