#!/usr/bin/env bash
# UI failures must fail CI; retain screenshots and logs even when a test fails.
set -euo pipefail
A=./.ci-bin/adb
failures=0
collect() {
  $A exec-out run-as com.jarvis.food tar cf - files/shots > redesign-shots.tar || true
  $A logcat -d -s AndroidRuntime:E > redesign-crash.txt || true
  $A shell settings put system font_scale 1.0 || true
  $A shell wm size reset || true
  $A shell wm density reset || true
}
trap collect EXIT
$A shell settings put global device_provisioned 1
$A shell settings put secure user_setup_complete 1
$A shell pm disable-user --user 0 com.google.android.googlesdksetup || true
$A shell pm disable-user --user 0 com.google.android.setupwizard || true
$A shell am force-stop com.google.android.apps.nexuslauncher || true
$A install -r -g baseline/app-debug.apk
$A install -r -g baseline/app-debug-androidTest.apk
$A logcat -c
$A shell settings put secure show_ime_with_hard_keyboard 1
run_suite() {
  local name="$1" classes="$2" status=0
  $A shell am instrument -w -e captureName "$name" -e class "$classes" com.jarvis.food.test/androidx.test.runner.AndroidJUnitRunner > "redesign-$name.log" 2>&1 || status=$?
  tail -50 "redesign-$name.log"
  $A exec-out run-as com.jarvis.food tar cf - files/shots > redesign-shots.tar || status=1
  if [ "$status" -ne 0 ] || ! grep -Eq '^OK \([1-9][0-9]* tests?\)' "redesign-$name.log" || grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|shortMsg=' "redesign-$name.log"; then
    echo "::error::Android suite failed: $name"
    failures=$((failures + 1))
  fi
}
$A shell wm size 1344x2992
$A shell wm density 480
$A shell settings put system font_scale 1.0
run_suite before 'com.jarvis.food.PremiumWholeAppTest,com.jarvis.food.RedesignNovemberTest'
$A install -r -g app/build/outputs/apk/debug/app-debug.apk
$A install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
if [ "${QA_SCREEN_SCOPE:-full}" = general ]; then
  run_suite pixel9 'com.jarvis.food.PremiumWholeAppTest,com.jarvis.food.RedesignNovemberTest,com.jarvis.food.PremiumVideoLayoutTest,com.jarvis.food.VisualConsistencyTest'
  run_suite diary-plan 'com.jarvis.food.ReferenceUiTest,com.jarvis.food.TodayDashboardTest,com.jarvis.food.TodayRefinementTest,com.jarvis.food.TrainingDiaryFlowTest,com.jarvis.food.MealOptionsFlowTest,com.jarvis.food.DietSelectionFollowupFlowTest'
  run_suite app-controls 'com.jarvis.food.AuroraRegressionTest,com.jarvis.food.FoodFollowupFlowTest,com.jarvis.food.FoodReviewFlowTest,com.jarvis.food.PhotoAiFlowTest,com.jarvis.food.ProfileFormsFlowTest,com.jarvis.food.NutritionAdvisorFlowTest,com.jarvis.food.BottomActionInsetsTest,com.jarvis.food.VideoNovemberFlowTest'
  $A shell settings put system font_scale 1.15
  run_suite pixel9-text115 'com.jarvis.food.PremiumVideoLayoutTest'
  $A shell wm size 945x2048
  $A shell wm density 420
  $A shell settings put system font_scale 1.3
  run_suite large-text 'com.jarvis.food.PremiumVideoLayoutTest,com.jarvis.food.VisualConsistencyTest'
elif [ "${QA_SCREEN_SCOPE:-full}" = references ]; then
  run_suite pixel9 'com.jarvis.food.RedesignNovemberTest,com.jarvis.food.PremiumVideoLayoutTest,com.jarvis.food.VisualConsistencyTest'
  $A shell settings put system font_scale 1.15
  run_suite pixel9-text115 'com.jarvis.food.PremiumVideoLayoutTest'
  $A shell wm size 945x2048
  $A shell wm density 420
  $A shell settings put system font_scale 1.3
  run_suite large-text 'com.jarvis.food.PremiumVideoLayoutTest,com.jarvis.food.VisualConsistencyTest'
elif [ "${QA_SCREEN_SCOPE:-full}" = profile ]; then
  # Watch-only typography change: all five tabs and all 13 Profile destinations on three configurations.
  run_suite pixel9 'com.jarvis.food.PremiumWholeAppTest'
  $A shell settings put system font_scale 1.15
  run_suite pixel9-text115 'com.jarvis.food.PremiumWholeAppTest'
  $A shell wm size 945x2048
  $A shell wm density 420
  $A shell settings put system font_scale 1.3
  run_suite large-text 'com.jarvis.food.PremiumWholeAppTest'
else
run_suite pixel9 'com.jarvis.food.PremiumWholeAppTest,com.jarvis.food.RedesignNovemberTest,com.jarvis.food.PremiumVideoLayoutTest,com.jarvis.food.VisualConsistencyTest'
run_suite regression 'com.jarvis.food.ReferenceUiTest,com.jarvis.food.TodayDashboardTest,com.jarvis.food.TodayRefinementTest,com.jarvis.food.AuroraRegressionTest,com.jarvis.food.TrainingDiaryFlowTest,com.jarvis.food.DietSelectionFollowupFlowTest'
$A shell settings put system font_scale 1.15
run_suite pixel9-text115 'com.jarvis.food.PremiumWholeAppTest,com.jarvis.food.RedesignNovemberTest,com.jarvis.food.PremiumVideoLayoutTest'
$A shell wm size 945x2048
$A shell wm density 420
$A shell settings put system font_scale 1.3
run_suite large-text 'com.jarvis.food.PremiumWholeAppTest,com.jarvis.food.RedesignNovemberTest,com.jarvis.food.PremiumVideoLayoutTest,com.jarvis.food.VisualConsistencyTest'
fi
$A logcat -d -s AndroidRuntime:E > redesign-crash.txt
if grep -q 'FATAL EXCEPTION' redesign-crash.txt; then failures=$((failures + 1)); cat redesign-crash.txt; fi
collect
trap - EXIT
# Root is available on the Google APIs emulator, allowing a controlled release-state fixture.
$A root
$A wait-for-device
python3 scripts/verify-release-upgrade.py
printf 'Failed Android suites: %s\n' "$failures" > redesign-summary.txt
[ "$failures" -eq 0 ]
