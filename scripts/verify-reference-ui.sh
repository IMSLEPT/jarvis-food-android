#!/usr/bin/env bash
set -euo pipefail
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c
run_suite() {
  local name="$1" classes="$2" count="$3"
  adb shell am instrument -w -e captureName "$name" -e class "$classes" com.jarvis.food.test/androidx.test.runner.AndroidJUnitRunner > "reference-$name.log" 2>&1
  cat "reference-$name.log"
  grep -q "OK ($count tests)" "reference-$name.log"
}
adb shell wm size 1344x2992
adb shell wm density 480
adb shell settings put system font_scale 1.0
run_suite pixel9 'com.jarvis.food.ReferenceUiTest,com.jarvis.food.TodayDashboardTest,com.jarvis.food.TodayRefinementTest,com.jarvis.food.VisualConsistencyTest#planActionsRemainReadableAndKeepTheirBehaviour,com.jarvis.food.AuroraRegressionTest' 10
adb shell wm size 1080x2340
adb shell wm density 480
adb shell settings put system font_scale 1.3
run_suite large-text 'com.jarvis.food.ReferenceUiTest,com.jarvis.food.TodayRefinementTest' 4
adb shell wm size 1080x1920
adb shell wm density 480
adb shell settings put system font_scale 2.0
run_suite small-200 'com.jarvis.food.ReferenceUiTest' 2
adb exec-out run-as com.jarvis.food tar cf - files/shots > reference-shots.tar
adb shell settings put system font_scale 1.0
adb shell wm size reset
adb shell wm density reset
# Actual release installation, then an in-place upgrade with the original package/signature.
adb uninstall com.jarvis.food
adb install original/*.apk
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell monkey -p com.jarvis.food -c android.intent.category.LAUNCHER 1
adb logcat -d -s AndroidRuntime:E > reference-crash.txt
if grep -q 'FATAL EXCEPTION' reference-crash.txt; then cat reference-crash.txt; exit 1; fi
