#!/usr/bin/env bash
set -euo pipefail
./.ci-bin/adb shell settings put global device_provisioned 1
./.ci-bin/adb shell settings put secure user_setup_complete 1
./.ci-bin/adb shell pm disable-user --user 0 com.google.android.googlesdksetup || true
./.ci-bin/adb shell am force-stop com.google.android.googlesdksetup || true
./.ci-bin/adb shell pm disable-user --user 0 com.google.android.setupwizard || true
./.ci-bin/adb shell am force-stop com.google.android.apps.nexuslauncher || true
./.ci-bin/adb install -r -g app/build/outputs/apk/debug/app-debug.apk
./.ci-bin/adb install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
./.ci-bin/adb logcat -c
run_suite() {
  local name="$1" classes="$2" count="$3"
  ./.ci-bin/adb shell am instrument -w -e captureName "$name" -e class "$classes" com.jarvis.food.test/androidx.test.runner.AndroidJUnitRunner > "reference-$name.log" 2>&1
  cat "reference-$name.log"
  ./.ci-bin/adb exec-out run-as com.jarvis.food tar cf - files/shots > reference-shots.tar
  ./.ci-bin/adb logcat -d -s AndroidRuntime:E > reference-crash.txt
  grep -q "OK ($count tests)" "reference-$name.log"
}
./.ci-bin/adb shell wm size 1344x2992
./.ci-bin/adb shell wm density 480
./.ci-bin/adb shell settings put system font_scale 1.0
run_suite pixel9 'com.jarvis.food.ReferenceUiTest,com.jarvis.food.TodayDashboardTest,com.jarvis.food.TodayRefinementTest,com.jarvis.food.VisualConsistencyTest#planActionsRemainReadableAndKeepTheirBehaviour,com.jarvis.food.AuroraRegressionTest' 10
./.ci-bin/adb shell wm size 1080x2340
./.ci-bin/adb shell wm density 480
./.ci-bin/adb shell settings put system font_scale 1.3
run_suite large-text 'com.jarvis.food.ReferenceUiTest,com.jarvis.food.TodayRefinementTest' 4
./.ci-bin/adb shell wm size 1080x1920
./.ci-bin/adb shell wm density 480
./.ci-bin/adb shell settings put system font_scale 2.0
run_suite small-200 'com.jarvis.food.ReferenceUiTest' 2
./.ci-bin/adb exec-out run-as com.jarvis.food tar cf - files/shots > reference-shots.tar
./.ci-bin/adb shell settings put system font_scale 1.0
./.ci-bin/adb shell wm size reset
./.ci-bin/adb shell wm density reset
# Actual release installation, then an in-place upgrade with the original package/signature.
./.ci-bin/adb uninstall com.jarvis.food
./.ci-bin/adb install original/*.apk
./.ci-bin/adb install -r app/build/outputs/apk/release/app-release.apk
./.ci-bin/adb shell am start -W -n com.jarvis.food/.MainActivity
sleep 2
./.ci-bin/adb shell pidof com.jarvis.food
./.ci-bin/adb logcat -d -s AndroidRuntime:E > reference-crash.txt
if grep -q 'FATAL EXCEPTION' reference-crash.txt; then cat reference-crash.txt; exit 1; fi
