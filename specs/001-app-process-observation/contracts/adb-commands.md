# Contract: 実験で使う ADB コマンド

**対応要件**: FR-007〜FR-010, FR-013, FR-017, US2 受け入れシナリオ4 | **関連**: [research.md R5〜R7](../research.md)

実験手順で使ってよいコマンドの一覧。ここにないコマンドを使った場合は、
その目的を `docs/experiments/.../adb-commands.md` に書き足すこと。

「目的」の欄は実装前の予想（[資料]／[仮説]）。実際の出力と端末の変化は
実験記録に書き、ここに書いた予想と照らし合わせる。

パッケージ名: `PKG=com.example.startuplab` / Activity: `$PKG/.MainActivity`

## 環境の確認

| ID | コマンド | 目的 |
|----|----------|------|
| C-ENV-1 | `adb devices -l` | 接続している端末（エミュレータ）を特定する |
| C-ENV-2 | `adb shell getprop ro.build.version.release` / `ro.build.version.sdk` / `ro.build.fingerprint` / `ro.build.type` / `ro.product.model` / `ro.product.cpu.abi` | 実験環境を記録する（FR-013） |
| C-ENV-3 | `adb shell settings get global always_finish_activities` | 「アクティビティを保持しない」がオフ（`0`）であることを確かめる |
| C-ENV-4 | `adb shell settings put global always_finish_activities 0` | 設定をオフに戻す。C-ENV-3 の値が `0` 以外だったときだけ使い、使ったことを記録してその試行をやり直す |

## インストール

| ID | コマンド | 目的 |
|----|----------|------|
| C-INS-1 | `adb install -r app/build/outputs/apk/debug/app-debug.apk` | アプリをインストールする |
| C-INS-2 | `adb uninstall $PKG` | アプリを削除する。Experiment 1 の開始状態を作るのに使う |

## 操作

| ID | コマンド | 目的 |
|----|----------|------|
| C-OP-1 | `adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n $PKG/.MainActivity` | ランチャーと同等の起動要求を送る（FR-007, FR-009）。`-W` で起動の完了を待ち、結果を表示する |
| C-OP-2 | `adb shell input keyevent KEYCODE_HOME` | HOME キーを押したのと同じ入力を送り、ホーム画面へ移動する |
| C-OP-3 | `adb shell am force-stop $PKG` | アプリを強制停止する（FR-008） |

## プロセスの観測

| ID | コマンド | 目的 |
|----|----------|------|
| C-PS-1 | `adb shell pidof $PKG` | 本アプリのプロセスがあるかと、その PID を確認する（FR-010） |
| C-PS-2 | `adb shell ps -A -o PID,PPID,USER,NAME` から本アプリの行と、その PPID の行を抜き出す | 親プロセスと UID を記録する |

## ログ

| ID | コマンド | 目的 |
|----|----------|------|
| C-LOG-1 | `adb logcat -b main,system,events -c` | 試行を始める前にログバッファを空にする（試行の区切り）。C-LOG-3 で読む3つのバッファを指定する。`-c` だけでは events バッファが消えないため（2026-10-09 に確認。docs/experiments/001-app-process-observation/adb-commands.md の C-LOG-1） |
| C-LOG-2 | `adb logcat -d -v threadtime -s StartupLab:I` | 本アプリのログを出力する |
| C-LOG-3 | `adb logcat -d -v threadtime -b main,system,events -s StartupLab:I ActivityManager:I am_proc_start:I am_proc_died:I am_kill:I` | 本アプリのログと、OS のプロセス起動・終了の記録を時系列で出力する（FR-017） |

## 使わないコマンド（今回は対象外）

- `am start -n ...`（ACTION と CATEGORY を付けない起動）、`monkey`: 起動要求の形が
  変わるため（Clarifications Q4）
- `am kill`、`kill <pid>`: プロセスを終了するだけの操作は、後続 Feature で比べる
- `settings put global always_finish_activities 1`: オンの状態は対象外（Clarifications Q3）
