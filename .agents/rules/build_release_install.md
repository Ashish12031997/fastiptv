# Build, Install & Release Policy

After every build or feature completion:
1. **ADB Installation**:
   - Install the built release APK (`app/build/outputs/apk/release/FastIPTV-release.apk`) directly to the connected Android TV device:
     ```bash
     ~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/release/FastIPTV-release.apk
     ```
   - Launch or restart the app:
     ```bash
     ~/Library/Android/sdk/platform-tools/adb shell am start -n com.fastiptv/.MainActivity
     ```

2. **GitHub Release**:
   - Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
   - Commit the changes (`git commit -m "feat(...): ... (vX.Y.Z)"`).
   - Create annotated tag `vX.Y.Z` (`git tag -a vX.Y.Z -m "Release FastIPTV vX.Y.Z"`).
   - Push commit and tag (`git push origin main && git push origin vX.Y.Z`) to trigger the GitHub Actions release workflow that builds release APK, checksums, and update-info.json for OTA updates.
