#!/usr/bin/env bash
# Drives the app on the test emulator for smoke tests.
#
#   ui.sh dump                  list the texts and content descriptions on screen
#   ui.sh tap "<text>"          tap the element with this text or content description
#   ui.sh shot <file.png>       save a screenshot
#
# Always talks to the emulator, never to a physical phone that may be connected as well:
# taps on a real device would act on somebody's real photos.

export ANDROID_SERIAL="${EMULATOR_SERIAL:-emulator-5554}"
export MSYS_NO_PATHCONV=1 # stops Git Bash on Windows from rewriting /sdcard/... paths

ADB="${ADB:-adb}"

dump() {
    "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
    "$ADB" exec-out cat /sdcard/ui.xml
}

case "$1" in
dump)
    dump | grep -oE '(text|content-desc)="[^"]+"' | sort -u | tr '\n' ' '
    echo
    ;;
tap)
    xy=$(dump | python -c "
import re, sys
xml, label = sys.stdin.read(), sys.argv[1]
node = re.search(r'<node[^>]*(?:text|content-desc)=\"' + re.escape(label)
                 + r'\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml)
print((int(node[1]) + int(node[3])) // 2, (int(node[2]) + int(node[4])) // 2) if node else print('')
" "$2")
    if [ -n "$xy" ]; then
        "$ADB" shell input tap $xy && echo "tapped '$2' at $xy"
    else
        echo "NOT FOUND: $2"
        exit 1
    fi
    ;;
shot)
    "$ADB" exec-out screencap -p >"$2" && echo "saved $2"
    ;;
*)
    sed -n '2,6p' "$0"
    exit 2
    ;;
esac
