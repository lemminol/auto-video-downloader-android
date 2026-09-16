#!/usr/bin/env bash
set -euo pipefail
gradle :app:assembleDebug
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
