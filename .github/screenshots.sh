#!/usr/bin/env bash
# Запускається всередині емулятора (reactivecircus/android-emulator-runner): ставить «Варту» і нешкідливу
# приманку, вмикає приманці права справжньої стежилки, розкидає сміття, проганяє перевірку й знімає екрани.
set -euo pipefail
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

# приманка com.systemservice: «встановлена з файлу», без значка, зі спецможливостями, сповіщеннями й адміністратором
SPY=testspy/build/outputs/apk/debug/testspy-debug.apk
P=com.systemservice
adb install -r -i com.google.android.packageinstaller "$SPY"
for perm in READ_SMS RECEIVE_SMS READ_CALL_LOG RECORD_AUDIO READ_CONTACTS ACCESS_FINE_LOCATION ACCESS_BACKGROUND_LOCATION; do
  adb shell pm grant $P android.permission.$perm || echo "grant $perm failed"
done
adb shell settings put secure enabled_accessibility_services $P/$P.Watch
adb shell settings put secure accessibility_enabled 1
adb shell cmd notification allow_listener $P/$P.Listen || true
adb shell dpm set-active-admin --user current $P/.Admin || true

# «Варті»: доступ до всіх файлів, статистика використання, сповіщення
adb shell appops set ua.x44to.varta MANAGE_EXTERNAL_STORAGE allow
adb shell appops set ua.x44to.varta GET_USAGE_STATS allow
adb shell pm grant ua.x44to.varta android.permission.POST_NOTIFICATIONS || true

# трохи сміття, як на справжньому телефоні
adb shell mkdir -p /sdcard/Download /sdcard/Movies /sdcard/Logs /sdcard/Documents/EmptyFolder /sdcard/Pictures/.thumbnails /sdcard/Android/media/com.removed.game
adb push "$SPY" /sdcard/Download/System_Service.apk
adb shell dd if=/dev/zero of=/sdcard/Download/upload.tmp bs=1024 count=2400
adb shell dd if=/dev/zero of=/sdcard/Logs/debug.log bs=1024 count=900
adb shell dd if=/dev/zero of=/sdcard/Pictures/.thumbnails/1700000000.jpg bs=1024 count=640
adb shell dd if=/dev/zero of=/sdcard/Android/media/com.removed.game/save.dat bs=1024 count=5200
adb shell dd if=/dev/zero of=/sdcard/Movies/trip.mp4 bs=1048576 count=120

# на повільному емуляторі CI системні застосунки буває «не відповідають» — це вікно лягало поверх скриншотів
adb shell settings put global hide_error_dialogs 1
adb shell settings put global anr_show_background 0
sleep 20
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null || true
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" > /dev/null; }
adb shell settings put global sysui_demo_allowed 1
demo enter
demo clock -e hhmm 0941
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e mobile show -e datatype none -e level 4
demo notifications -e visible false
adb logcat -c || true
set +e
adb shell am instrument -w -e class ua.x44to.varta.ScreenshotTest \
  ua.x44to.varta.test/androidx.test.runner.AndroidJUnitRunner | tee instrument.log
ok=0
grep -q "OK (1 test)" instrument.log && ok=1
mkdir -p docs/screens
adb exec-out run-as ua.x44to.varta tar -cf - files/screens | tar -xf - -C docs/screens --strip-components=2
ls -la docs/screens
adb logcat -d -s Varta:D > test.log || true
cat test.log
if [ "$ok" != 1 ]; then
  adb logcat -d -b crash > crash.log
  adb logcat -d -t 4000 > logcat.log
  exit 1
fi
