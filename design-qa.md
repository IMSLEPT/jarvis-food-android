# Jarvis Food - design QA

Final result: blocked - final Android captures and visual comparison pending.

## Source and scope

Approved image references supplied in the conversation and the complete 125-second video. The user's latest clarification takes priority: match the visual identity and composition, resize for actual Android usage. Existing application only; no rebuild, no changes to business logic or saved data.

Baseline: v1.0.224, commit `9158f43d881d5d626f77e41a54d34881c5ebbf93`.

## Findings and corrections

| Finding | Impact | Applied correction | Evidence required |
|---|---|---|---|
| Calendar title/date rows reserve excessive height | P2, density | Shared measured title area and scalable week strip | Same fixture and calendar bounds on Oggi/Piano |
| Large day-type icon/heading | P2, hierarchy | 40 dp illustration, 16/13 sp text | Normal and 130% captures |
| Nutrient values at 10 sp | P2, readability | 12 sp label/value/percent, live 32 dp progress ring | Dashboard values and semantics |
| Inconsistent buttons and radii | P2, consistency | 14 dp buttons, 20 dp cards, 24 dp banners; common Material typography | Main and secondary destinations |
| White text on bright orange | P2, contrast | Dark orange action gradient, measured contrast 4.62-5.50:1 | Selection state captures |
| Fragmented profile rows with repeated elevation | P2, grouping | Three bordered groups, 36 dp icon tiles and aligned dividers | Profile top/tools |
| Tall recipe photos and barcode heading | P2, proportions | 2:1 photo and smaller supporting actions | Recipe card capture and retained actions |
| Hand-drawn profile gear unclear | P2, icon quality | Material Person and Settings icons | Header close-up |

## First Android comparison - blocked

Opened combined before/current captures from run 41910900546 at 100% and inspected the 130% dashboard and Profile/settings boards. Main headers, recipe card density, grouped profile rows and type hierarchy improve. Two actionable issues remain:

- P2: 115% calendars differ by 2 dp; instrumented bounds detect 6 physical pixels. Fixed with shared measured name/date height and scalable calendar. Recheck pending.
- P2: wide nutrient layout lost its compact row treatment at 130%, making the card too tall. Restore ring/detail/percent rows for narrow screens. Recheck pending.
- P2: Kitchen category heading carries an excessive preceding gap and enlarged shortcuts have unnecessary height. Reduce heading top spacing and keep shortcut minimum 76 dp with natural content growth. Recheck pending.

- P2: selected-day marker touches the lower rounded border in the 130% capture. Narrow the marker and reserve 8 dp bottom padding; confirm in revised capture.

- P2: banner tabs leave unused horizontal space while approved references distribute them across the banner. Measure label widths, distribute spare space evenly, retain horizontal scrolling when labels require more width. Recheck pending.

Source-target comparison uses the approved reference assets and the final revised captures, with dimensions adapted for mobile use per user instruction.

## Validation plan and current evidence

- Actual Android 15 emulator: 1344x2992 at density 480, fonts 100% and 115%; 945x2048 at density 420, font 130%.
- Identical baseline/current fixtures captured by PremiumWholeAppTest.
- Five main tabs; profile, personal details, goals, training nutrition, diet, measurements, weight, smartwatch, AI, fasting, reminders, foods, backup, about.
- Existing interaction regressions, keyboard/insets, calendar continuity, meal actions, live nutrition progress, diet choices.
- Original signing certificate and signed in-place upgrade with profile/water/steps/chat retention.
- Original functional/data boundary checks pass: 187 original files unchanged and 31 previously authorized changes.
- Photography checks pass: 170 complete recipes, 821 mappings, 329 compatible shared uses.
- Build and unit tests passed in first Android build. Final build and rendering verdict pending.

No final 9/10 score is asserted before visual comparison. Desktop/tablet are outside this Android phone task; physical Pixel 9 Pro XL has not been connected.


## Second rendered comparison - blocked

Run 41915212910 passed all 164 unit tests and 44 current-version Android tests (plus 2 baseline captures). Visual comparison confirms the earlier calendar, macro, kitchen spacing and tab distribution corrections. However, the new bottom-of-form captures exposed a P1 issue: Backup's last button occupies the Android gesture-navigation area. This also affects secondary pages that lack their own bottom inset. Fix: ScreenHost owns and consumes navigationBarsPadding once; child inset-aware footers do not double-pad. Extend the whole-app test to assert visible clickable bounds stay above the actual system navigation inset in every captured secondary destination. A new signed build is required; v225 is an intermediate preview, not the final handoff.


## Third rendered comparison - one isolated typography finding

Run 41919566657 passed 164 unit tests and all 44 current-version Android tests, including secondary safe-area bounds. The actual 130% Backup screenshot confirms the system gesture area is now clear. Reviewing the remaining settings captures revealed a P2 word-break in the Watch connection diagram: Health Connect split Connect into Connec/t. Allocate each step at least its longest word's measured width, share remaining space, and scroll only if the complete diagram cannot fit. A new word-boundary assertion verifies all four labels. For this isolated Watch component change, rerun the whole-app/Profile capture suite at all three configurations (all five tabs, 13 destinations and safe-area checks), unit tests, asset checks, signing and release upgrade. The earlier full interaction suite remains evidence for unchanged code; final report distinguishes the executions.


The first isolated Watch run (41923560793) passed compilation, 164 unit tests, signing/assets and upgrade checks, but its new label-overflow assertion failed before completing Profile captures. The label now fills its measured column and uses an explicit matching 12/16 sp style and zero tracking in both measurement and rendering. The test checks actual line bounds with a one-physical-pixel rounding tolerance, plus whole-word boundaries, and captures the diagram before asserting. A new build remains required; no failed APK is handed off.
