#!/bin/bash
# cap3.sh "actions" : records; frames at 1fps tiled to /tmp/jf-shots/all_N.png (12 per sheet, 6x2), full frames in /tmp/jf-shots/f/
A=/Users/alessandrocalamai/Desktop/scrcpy/adb
cd /Users/alessandrocalamai/Desktop/scrcpy
rm -f /tmp/vd.mkv
./scrcpy --new-display=1008x2244/360 --start-app=+com.jarvis.food.dev --no-playback --no-audio --record=/tmp/vd.mkv > /tmp/scrcpy.log 2>&1 &
SP=$!
sleep 10
ID=$(grep -o 'id=[0-9]*' /tmp/scrcpy.log | head -1 | cut -d= -f2)
IFS=';' read -ra ACTS <<< "$1"
for a in "${ACTS[@]}"; do set -- $a
  case $1 in
   t) $A shell input -d $ID tap $2 $3; sleep 3;;
   s) $A shell input -d $ID swipe 500 1700 500 500 300; sleep 2.5;;
   b) $A shell input -d $ID keyevent KEYCODE_BACK; sleep 2.5;;
   w) sleep $2;;
  esac; done
sleep 2; kill -INT $SP; sleep 3
rm -rf /tmp/jf-shots/f /tmp/jf-shots/all_*.png; mkdir -p /tmp/jf-shots/f
~/.local/bin/ffmpeg -loglevel quiet -y -i /tmp/vd.mkv -vf "fps=1" /tmp/jf-shots/f/f_%03d.png
ls /tmp/jf-shots/f | wc -l
