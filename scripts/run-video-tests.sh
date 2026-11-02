#!/usr/bin/env bash
set -euo pipefail
profile="${1:?Capture profile is required}"
case "$profile" in standard|large-text) ;; *) exit 2 ;; esac
combined="layout-video-$profile.log"
: > "$combined"
# Each case owns its app process, as with Android Test Orchestrator.
# No crashed or failing case is retried or accepted.
for method in \
  allThreeProgressCardsOpenReadOnlyDayDetails \
  brandedSlicesUseGramsAndSaveTheSelectedPieceCount \
  timerMovesImmediatelyAndResumesWithoutReset \
  dietSelectionNeedsConfirmationKeepsTargetsAndRejectsConflict
do
  logfile="layout-video-$profile-$method.log"
  ./.ci-bin/adb shell am instrument -w -e captureName "$profile" \
    -e class "com.jarvis.food.VideoCorrectionsFlowTest#$method" \
    com.jarvis.food.test/androidx.test.runner.AndroidJUnitRunner > "$logfile" 2>&1
  cat "$logfile" | tee -a "$combined"
  ./.ci-bin/adb logcat -b crash -d > layout-video-crash.log
  if ! grep -q "OK (1 test)" "$logfile"; then
    cat layout-video-crash.log
    ./.ci-bin/adb logcat -d > "layout-video-$profile-failure.log"
    ./.ci-bin/adb shell dumpsys meminfo > "layout-video-$profile-memory.log"
    exit 1
  fi
done
