#!/usr/bin/env bash
# Captures d'écran de Sillon sur un émulateur (lancé par screenshots.yml).
# Les données affichées sont des exemples générés par seed.py, à une date de printemps fixée.
set -uo pipefail
cd "$(dirname "$0")"
OUT="${OUT:-$PWD/out}"
mkdir -p "$OUT"
APK="$1"
PKG=fr.pilmil.sillon
UI="python3 ui.py"

adb root >/dev/null; sleep 3; adb wait-for-device
adb shell setprop persist.sys.timezone Europe/Paris
# Téléphone entier en français (dates, jours de la semaine, paramètres Android).
adb shell setprop persist.sys.locale fr-FR
adb shell setprop ctl.restart zygote
sleep 10; adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
sleep 8
adb shell settings put global auto_time_zone 0
adb shell settings put global auto_time 0
adb shell settings put system screen_off_timeout 1800000
adb shell svc power stayon true
# Date fixée au 6 mai 2026, 10 h 30 à Paris (8 h 30 UTC) : après les dernières gelées du département,
# les tâches du moment sont des semis, des repiquages et des récoltes.
adb shell date -u 050608302026.00
# Barre d'état propre (mode démo).
adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
demo enter
demo clock -e hhmm 1030
demo battery -e level 82 -e plugged false
demo network -e wifi show -e level 4
demo network -e mobile show -e datatype none -e level 4
demo notifications -e visible false

adb install -r "$APK"
adb shell cmd locale set-app-locales $PKG --locales fr-FR || true
NOW=$(adb shell date +%s | tr -d '\r')
echo "Date de l'émulateur : $(adb shell date | tr -d '\r')"

python3 seed.py "$NOW" /tmp/sillon_garden.json /tmp/sillon_prefs.xml
adb shell am force-stop $PKG
adb shell mkdir -p /data/data/$PKG/files /data/data/$PKG/shared_prefs
adb push /tmp/sillon_garden.json /data/data/$PKG/files/sillon_garden.json >/dev/null
adb push /tmp/sillon_prefs.xml /data/data/$PKG/shared_prefs/sillon_prefs.xml >/dev/null
OWNER=$(adb shell stat -c %u /data/data/$PKG | tr -d '\r')
adb shell chown -R "$OWNER:$OWNER" /data/data/$PKG/files /data/data/$PKG/shared_prefs
adb shell chmod 660 /data/data/$PKG/files/sillon_garden.json /data/data/$PKG/shared_prefs/sillon_prefs.xml
adb shell restorecon -R /data/data/$PKG || true

shot() { sleep "${2:-2}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "capture $1"; }

adb shell input keyevent KEYCODE_HOME
adb shell am start -n $PKG/.MainActivity >/dev/null
shot 1-aujourdhui 8
$UI tap "=Parcelles" -1; shot 2-parcelles 3
$UI tap "=Plantes" -1; shot 3-plantes 3
$UI tap "=Tomate" 0; shot 4-fiche-plante 3
$UI tap "=Diagnostic" -1; shot 5-diagnostic 3
$UI tap "=Réglages" -1; shot 6-reglages 3
ls -l "$OUT"
