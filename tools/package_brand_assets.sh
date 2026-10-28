#!/usr/bin/env bash
# Package the approved robot chef for adaptive Android launcher masks.
set -euo pipefail
project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
robot_asset="$project_root/app/src/main/res/drawable-nodpi/jarvis_logo.png"
for spec in mdpi:108 hdpi:162 xhdpi:216 xxhdpi:324 xxxhdpi:432; do
    density="${spec%%:*}"
    pixels="${spec##*:}"
    foreground=$(( pixels * 66 / 100 ))
    target="$project_root/app/src/main/res/mipmap-$density"
    convert "$robot_asset" -resize "${foreground}x${foreground}" -gravity center -background none \
        -extent "${pixels}x${pixels}" -strip -define png:exclude-chunks=date,time "$target/ic_launcher_fg.png"
    convert -size "${pixels}x${pixels}" xc:'#172653' -strip -define png:exclude-chunks=date,time "$target/ic_launcher_bg.png"
done
