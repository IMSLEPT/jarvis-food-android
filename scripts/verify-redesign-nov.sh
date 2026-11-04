#!/usr/bin/env bash
set -uo pipefail
A=./.ci-bin/adb
$A shell settings put global device_provisioned 1
$A shell settings put secure user_setup_complete 1
$A shell pm disable-user --user 0 com.google.android.googlesdksetup || true
$A shell pm disable-user --user 0 com.google.android.setupwizard || true
$A shell am force-stop com.google.android.apps.nexuslauncher || true
$A install -r -g app/build/outputs/apk/debug/app-debug.apk
$A install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
$A logcat -c
run_suite() {
  local name="$1" classes="$2"
  $A shell am instrument -w -e captureName "$name" -e class "$classes" com.jarvis.food.test/androidx.test.runner.AndroidJUnitRunner > "redesign-$name.log" 2>&1
  tail -40 "redesign-$name.log"
  $A exec-out run-as com.jarvis.food tar cf - files/shots > redesign-shots.tar
}
# Pixel 9 Pro XL: 1344x2992, 480 dpi
$A shell wm size 1344x2992
$A shell wm density 480
$A shell settings put system font_scale 1.0
run_suite pixel9 'com.jarvis.food.RedesignNovemberTest'
run_suite regression 'com.jarvis.food.ReferenceUiTest,com.jarvis.food.TodayDashboardTest,com.jarvis.food.TodayRefinementTest,com.jarvis.food.AuroraRegressionTest,com.jarvis.food.TrainingDiaryFlowTest,com.jarvis.food.DietSelectionFollowupFlowTest'
# Pixel 9 Pro XL con carattere leggermente ingrandito, come nel video di Alessandro
$A shell settings put system font_scale 1.15
run_suite pixel9-text115 'com.jarvis.food.RedesignNovemberTest'
# Telefono più stretto e testo ingrandito
$A shell wm size 1080x2340
$A shell wm density 480
$A shell settings put system font_scale 1.3
run_suite large-text 'com.jarvis.food.RedesignNovemberTest'
$A shell settings put system font_scale 1.0
$A shell wm size reset
$A shell wm density reset
# Aggiornamento sopra l'APK originale, stessa firma
$A uninstall com.jarvis.food
$A install original/*.apk
$A install -r app/build/outputs/apk/release/app-release.apk > redesign-upgrade.log 2>&1
cat redesign-upgrade.log
$A shell am start -W -n com.jarvis.food/.MainActivity >> redesign-upgrade.log 2>&1
sleep 3
$A shell pidof com.jarvis.food >> redesign-upgrade.log 2>&1
$A logcat -d -s AndroidRuntime:E > redesign-crash.txt
exit 0
