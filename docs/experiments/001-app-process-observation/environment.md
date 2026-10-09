# 実験環境（001-app-process-observation）

全実験で共有する環境の記録（FR-013、data-model.md §5）。記録日時: 2026-10-09 21:49

| フィールド | 値 | 取得コマンド |
|-----------|----|-------------|
| avdName | `Pixel_5_API_36` | `adb emu avd name` |
| androidRelease | `16` | C-ENV-2 `getprop ro.build.version.release` |
| sdkInt | `36` | C-ENV-2 `getprop ro.build.version.sdk` |
| fingerprint | `google/sdk_gphone64_arm64/emu64a:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys` | C-ENV-2 `getprop ro.build.fingerprint` |
| buildType | `userdebug` | C-ENV-2 `getprop ro.build.type` |
| model | `sdk_gphone64_arm64` | C-ENV-2 `getprop ro.product.model` |
| abi | `arm64-v8a` | C-ENV-2 `getprop ro.product.cpu.abi` |
| alwaysFinishActivities | `0`（下の補足を参照） | C-ENV-3 `settings get global always_finish_activities` |
| hostTools | 下の表を参照 | |
| aospRef | frameworks/base `android-16.0.0_r1`、packages/apps/Launcher3 `android-16.0.0_r1` | research.md R5・R6 |

## hostTools

| ツール | バージョン |
|--------|-----------|
| adb | 1.0.41（platform-tools 35.0.1-11580240） |
| JDK | openjdk 17.0.16（Homebrew `openjdk@17`） |
| Android Gradle Plugin | 9.4.0 |
| Gradle（Wrapper） | 9.6.0 |
| Kotlin | 2.2.10（AGP 9.4.0 の組み込み Kotlin が使うバージョン。`./gradlew buildEnvironment` で確認） |
| ホスト OS | macOS 26.6.2（arm64） |

## 補足

- C-ENV-1 の出力: `emulator-5554 device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64 device:emu64a transport_id:1`
- C-ENV-3 の最初の値は `null`（設定キーがまだない状態）だった。`0` ではないため、
  C-ENV-4（`settings put global always_finish_activities 0`）を実行し、もう一度 C-ENV-3 で `0` になったことを確かめた。
  この時点では実験の試行をまだ始めていないので、やり直した試行はない。
- R1 の [仮説]「ビルド種別は `userdebug`」は、上の buildType の値で確かめられた。
- aospRef のタグは、fingerprint のビルド `BE2A.250530.026`（2025-05-30）と日付が最も近い最初のリリースを選んだ。ビルドとタグが厳密に対応するかは未確認。
