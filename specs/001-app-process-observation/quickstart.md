# Quickstart: Androidアプリの起動とプロセスの観察

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

実装が終わったあとに、この Feature が最後まで動くかを確かめる手順。
コマンドの意味は [contracts/adb-commands.md](./contracts/adb-commands.md)、
ログの読み方は [contracts/log-format.md](./contracts/log-format.md) を参照すること。

> ここに書く「期待される結果」は、**動作確認の合格基準**として使う。
> OS の挙動についての予想（仮説）は、実験記録の側で検証する。

## 0. 前提

- Android SDK（platform-tools, emulator）と JDK 17 が入っていること
- AVD `Pixel_5_API_36` があること
- 以降のコマンドは、プロジェクトルート（`android-startup-internals-lab/`。git のルートはその1つ上）で実行する

```sh
PKG=com.example.startuplab
```

## 1. ビルドとインストール

```sh
emulator -avd Pixel_5_API_36 &      # エミュレータを起動する（起動が終わるまで待つ）
adb devices -l                      # C-ENV-1
./gradlew assembleDebug lint
adb install -r app/build/outputs/apk/debug/app-debug.apk   # C-INS-1
```

**期待される結果**: ビルドと lint が成功する。`adb install` が `Success` を返す。

## 2. 事前確認

```sh
adb shell getprop ro.build.version.sdk                      # C-ENV-2
adb shell settings get global always_finish_activities      # C-ENV-3
```

**期待される結果**: `36` と `0`。`0` 以外なら C-ENV-4（`adb shell settings put global always_finish_activities 0`）を実行してから進む。

## 3. 動作確認シナリオ

### S1: ログが観察できる（US1 / SC-002）

```sh
adb shell am force-stop $PKG
adb logcat -b main,system,events -c
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n $PKG/.MainActivity
adb logcat -d -v threadtime -s StartupLab:I
```

**期待される結果**:
- `source=Application event=onCreate` が1行出て、その後に `source=Activity` の
  `onCreate` → `onStart` → `onResume` が続く
- すべての行で、logcat の PID 列と `pid=` が同じ値になっている

### S2: ADB だけで操作できる（US2 / SC-001）

```sh
adb shell input keyevent KEYCODE_HOME       # ホーム画面になる
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n $PKG/.MainActivity
adb shell am force-stop $PKG                # アプリが画面から消える
```

**期待される結果**: 端末に触らずに、表示 → ホーム → 再表示 → 停止 ができる。

### S3: PID を突き合わせられる（US3 / SC-003）

```sh
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n $PKG/.MainActivity
adb shell pidof $PKG                          # C-PS-1
adb logcat -d -v threadtime -s StartupLab:I | tail -n 3
adb shell am force-stop $PKG
adb shell pidof $PKG                          # 何も出ない
```

**期待される結果**: `pidof` の値と、直前のログの `pid=` が一致する。強制停止の後は、`pidof` が空になる。

### S4: OS 側の記録を取り出せる（FR-017）

```sh
adb logcat -b main,system,events -c
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n $PKG/.MainActivity
adb logcat -d -v threadtime -b main,system,events -s StartupLab:I ActivityManager:I am_proc_start:I am_proc_died:I am_kill:I
```

**期待される結果**: コマンドがエラーにならずに、本アプリのログと OS のログが時系列で並んで出る。
`Start proc` と `am_proc_start` が出るかどうかは、合否の基準にしない（[仮説]）。
出なかったときは、そのことを記録に残す。

### S5: 実験記録がそろっている（US4 / SC-004〜SC-007）

`docs/experiments/001-app-process-observation/` に、次のファイルがあることを確かめる。

- `environment.md`, `adb-commands.md`
- `exp1-first-launch.md`, `exp2-background-return.md`, `exp3-force-stop-relaunch.md`
  （それぞれ Trial が3つ以上あり、テンプレートの見出しが欠けていない）
- `comparison.md`（比較表と、Q1〜Q5 への回答または「未確認」）

**期待される結果**: spec の SC-005 と SC-006 を、記録だけを見て確認できる。
