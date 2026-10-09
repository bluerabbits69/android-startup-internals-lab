# ADB コマンドの記録（001-app-process-observation）

使ったコマンドごとの記録（US2 受け入れシナリオ4、data-model.md §4）。
コマンドの一覧と ID は [contracts/adb-commands.md](../../../specs/001-app-process-observation/contracts/adb-commands.md) に従う。

パッケージ名: `PKG=com.example.startuplab` / 環境: [environment.md](./environment.md)

- purpose 欄は学習者の言葉（T024、2026-10-09）。C-OP-1 と C-OP-3 は学習者が答えたもの、それ以外は AI の下書きを学習者が確認して承認したもの。
- 出力は 2026-10-09 に実行したときの原文。PID などの値は実行ごとに変わる。
- 「help」は、端末上の各コマンドのヘルプ（`adb help`、`adb logcat --help`、`adb shell am help`、
  `adb shell settings help`、`adb shell input`、`adb shell pidof --help`、`adb shell ps --help`）を指す。

## 環境の確認

### C-ENV-1

| 欄 | 内容 |
|----|------|
| command | `adb devices -l` |
| purpose | パソコンにつながっている端末の一覧を見る |
| options | `-l`: 詳しい情報（product, model, device, transport_id）も表示する |
| output | `emulator-5554 device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64 device:emu64a transport_id:1` |
| effect | 端末の変化はない（ホスト側で接続中の端末を一覧にするだけ） |
| source | `adb help`（`devices [-l]  list connected devices (-l for long output)`） |

### C-ENV-2

| 欄 | 内容 |
|----|------|
| command | `adb shell getprop ro.build.version.release` / `ro.build.version.sdk` / `ro.build.fingerprint` / `ro.build.type` / `ro.product.model` / `ro.product.cpu.abi` |
| purpose | 端末の Android のバージョンや機種などの情報を読む |
| options | 引数はシステムプロパティの名前。`adb shell` は端末上でコマンドを実行する |
| output | `16` / `36` / `google/sdk_gphone64_arm64/emu64a:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys` / `userdebug` / `sdk_gphone64_arm64` / `arm64-v8a` |
| effect | 端末の変化はない（値を読むだけ） |
| source | `adb help`（`shell`） |

### C-ENV-3

| 欄 | 内容 |
|----|------|
| command | `adb shell settings get global always_finish_activities` |
| purpose | 「アクティビティを保持しない」設定がオフかどうかを確かめる |
| options | `get NAMESPACE KEY`: 名前空間 `global` のキー `always_finish_activities` の値を読む |
| output | エミュレータ起動直後は `null`（キーがまだない）。C-ENV-4 の後は `0` |
| effect | 端末の変化はない（値を読むだけ） |
| source | `adb shell settings help`（`get [--user <USER_ID> \| current] NAMESPACE KEY`） |

### C-ENV-4

| 欄 | 内容 |
|----|------|
| command | `adb shell settings put global always_finish_activities 0` |
| purpose | 「アクティビティを保持しない」設定をオフにする |
| options | `put NAMESPACE KEY VALUE`: 名前空間 `global` のキーに値 `0` を書く |
| output | 出力なし |
| effect | C-ENV-3 の値が `null` から `0` になった（environment.md の補足を参照） |
| source | `adb shell settings help`（`put [--user <USER_ID> \| current] NAMESPACE KEY VALUE ...`） |

## インストール

### C-INS-1

| 欄 | 内容 |
|----|------|
| command | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| purpose | アプリを端末にインストールする（入っていれば上書きする） |
| options | `-r`: すでに入っているアプリを置き換える |
| output | `Performing Streamed Install` → `Success` |
| effect | `pm list packages com.example.startuplab` に `package:com.example.startuplab` が出るようになった。インストール直後の `dumpsys package` は `stopped=true` |
| source | `adb help`（`install [-lrtsdg] [--instant] PACKAGE`、`-r: replace existing application`） |

### C-INS-2

| 欄 | 内容 |
|----|------|
| command | `adb uninstall $PKG` |
| purpose | アプリを端末から削除する |
| options | オプションなし（`-k` を付けるとデータとキャッシュを残すが、使わない） |
| output | `Success` |
| effect | `pm list packages com.example.startuplab` の出力が空になった |
| source | `adb help`（`uninstall [-k] PACKAGE`） |

## 操作

### C-OP-1

| 欄 | 内容 |
|----|------|
| command | `adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n $PKG/.MainActivity` |
| purpose | アプリを起動するためのコマンド |
| options | `-W`: 起動の完了（最初の表示）まで待つ。`-a`: Intent の action。`-c`: Intent の category。`-n`: 起動するコンポーネント名。`-f`: Intent のフラグ（数値）。`0x10200000` = `0x10000000`（`FLAG_ACTIVITY_NEW_TASK`）\| `0x00200000`（`FLAG_ACTIVITY_RESET_TASK_IF_NEEDED`） |
| output | 下の「C-OP-1 の出力」を参照 |
| effect | プロセスがないとき: 新しいプロセスができ、`dumpsys activity activities` の `topResumedActivity` が `com.example.startuplab/.MainActivity` になった。前面にあるとき: PID も前面の Activity も変わらなかった |
| source | `adb shell am help`（`-W: wait for launch to complete (initial display)`、`<INTENT> specifications`）。フラグの値: [Intent（API リファレンス）](https://developer.android.com/reference/android/content/Intent)。ランチャーと同じ Intent かどうか: [AOSPで確認] Launcher3 `android-16.0.0_r1`、`src/com/android/launcher3/model/data/AppInfo.java`、`makeLaunchIntent(ComponentName)`（[ソース](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/tags/android-16.0.0_r1/src/com/android/launcher3/model/data/AppInfo.java#167)）が、同じ action・category・2つのフラグで Intent を作っている。エミュレータの `NexusLauncher` が同じかどうかと、起動までにフラグが足されるかどうかは未確認（research.md R5） |

#### C-OP-1 の出力

**A. プロセスがない状態**（直前に C-OP-3、`pidof` は空）

```
Starting: Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10200000 cmp=com.example.startuplab/.MainActivity }
Status: ok
LaunchState: COLD
Activity: com.example.startuplab/.MainActivity
TotalTime: 1458
WaitTime: 1464
Complete
```

実行後の `pidof` は `4847`。

**B. プロセスがあり、MainActivity が前面にある状態**（A の直後）

```
Starting: Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10200000 cmp=com.example.startuplab/.MainActivity }
Warning: Activity not started, intent has been delivered to currently running top-most instance.
Status: ok
LaunchState: UNKNOWN (0)
Activity: com.example.startuplab/.MainActivity
TotalTime: 0
WaitTime: 16
Complete
```

実行後の `pidof` は `4847` のまま。`StartupLab` のログは1行も出なかった。

### C-OP-2

| 欄 | 内容 |
|----|------|
| command | `adb shell input keyevent KEYCODE_HOME` |
| purpose | HOME ボタンを押して、ホーム画面に戻る |
| options | `keyevent <key code number or name>`: キー入力を送る。`KEYCODE_HOME` は HOME キー |
| output | 出力なし（終了コード 0） |
| effect | MainActivity が前面のときに実行すると、`topResumedActivity` がランチャー（`com.google.android.apps.nexuslauncher/.NexusLauncherActivity`）に変わった。`pidof` は `4847` のまま |
| source | `adb shell input`（`keyevent [...] <key code number or name> ...`） |

### C-OP-3

| 欄 | 内容 |
|----|------|
| command | `adb shell am force-stop $PKG` |
| purpose | 強制終了させるコマンド |
| options | 引数はパッケージ名 |
| output | 出力なし（終了コード 0） |
| effect | アプリがバックグラウンドにあるとき（C-OP-2 の後）に実行した。直後の C-PS-1 で `pidof` が空になった（プロセスが消えた）。`dumpsys package` は `stopped=true` になった |
| source | `adb shell am help`（`force-stop ... <PACKAGE>  Completely stop the given application package.`） |

## プロセスの観測

### C-PS-1

| 欄 | 内容 |
|----|------|
| command | `adb shell pidof $PKG` |
| purpose | アプリのプロセスが動いているかと、その PID を調べる |
| options | 引数はプロセス名。オプションなし |
| output | プロセスがあるとき: `4847` のように PID だけ。ないとき: 何も出ない |
| effect | 端末の変化はない（見るだけ） |
| source | `adb shell pidof --help`（`usage: pidof [-s] [-o omitpid[,omitpid...]] [NAME...]`、Toybox 0.8.12-android） |

### C-PS-2

| 欄 | 内容 |
|----|------|
| command | `adb shell ps -A -o PID,PPID,USER,NAME` から本アプリの行と、その PPID の行を抜き出す |
| purpose | プロセスの一覧から、アプリのプロセスとその親プロセスを調べる |
| options | `-A`: すべてのプロセス。`-o`: 表示する列を指定する |
| output | （T025 で書く） |
| effect | 端末の変化はない（見るだけ） |
| source | `adb shell ps --help`（`-A  All`、`-o  Output FIELDs instead of defaults`） |

## ログ

### C-LOG-1

| 欄 | 内容 |
|----|------|
| command | `adb logcat -b main,system,events -c`（2026-10-09 に `adb logcat -c` から変更。理由は下の表） |
| purpose | 実験を始める前に、ログを空にする |
| options | `-b main,system,events`: 消すバッファを指定する。`-c`: ログを消して終了する |
| output | 出力なし（終了コード 0） |
| effect | 3つのバッファがすべて 0 行になった。変更前の `adb logcat -c` では **events バッファは消えなかった**（下の表） |
| source | `adb logcat --help`（`-c, --clear  Clear (flush) the entire log and exit.`） |

#### C-LOG-1 で消えるバッファ

| 操作 | events | main | system |
|------|--------|------|--------|
| 実行前 | 2761行 | 796行 | 140行 |
| `adb logcat -c` の後 | 2761行 | 17行 | 0行 |
| `adb logcat -b main,system,events -c` の後 | 0行 | 0行 | 0行 |

`-c` だけでは events バッファが消えず、エミュレータ起動直後からの `am_proc_start` などが残っていた。
C-LOG-3 は events バッファも読むので、このままでは前の試行の記録が混ざる。
そのため、C-LOG-1 を `-b main,system,events -c` に変えた（contracts/adb-commands.md も変更済み）。

### C-LOG-2

| 欄 | 内容 |
|----|------|
| command | `adb logcat -d -v threadtime -s StartupLab:I` |
| purpose | 自分のアプリが出したログだけを見る |
| options | `-d`: 今あるログを出力して終了する（待ち続けない）。`-v threadtime`: 日付・時刻・PID・TID・優先度・タグを付ける。`-s`: 指定しないタグは出さない。`StartupLab:I`: タグ `StartupLab` の INFO 以上を出す |
| output | 下を参照 |
| effect | 端末の変化はない（読むだけ） |
| source | `adb logcat --help`（`-d`、`-v, --format`、`-s  Set default filter to silent`、`threadtime  Show the date, invocation time, priority, tag, PID, and TID`） |

```
--------- beginning of main
10-09 22:18:29.366  5131  5131 I StartupLab: source=Application event=onCreate pid=5131 instance=ab43fa3
10-09 22:18:29.412  5131  5131 I StartupLab: source=Activity event=onCreate pid=5131 instance=2937a2a
10-09 22:18:29.541  5131  5131 I StartupLab: source=Activity event=onStart pid=5131 instance=2937a2a
10-09 22:18:29.584  5131  5131 I StartupLab: source=Activity event=onResume pid=5131 instance=2937a2a
```

### C-LOG-3

| 欄 | 内容 |
|----|------|
| command | `adb logcat -d -v threadtime -b main,system,events -s StartupLab:I ActivityManager:I am_proc_start:I am_proc_died:I am_kill:I` |
| purpose | 自分のアプリのログと、OS のプロセス起動・終了の記録を、まとめて時間順に見る |
| options | `-b main,system,events`: 読むバッファを指定する（選べるのは `main system radio events crash default all`）。ほかは C-LOG-2 と同じ。タグを並べると、そのタグだけを出す |
| output | 下を参照（本アプリに関係する行だけを抜き出した） |
| effect | 端末の変化はない（読むだけ） |
| source | `adb logcat --help`（`-b BUFFER, --buffer=BUFFER  Request alternate ring buffer(s).`） |

C-INS-2 → C-INS-1 → C-LOG-1 → C-OP-1 の後に実行した。他のアプリのプロセスの行も大量に出る（ここでは省いた）。

```
10-09 22:18:28.971   681   741 I am_proc_start: [0,5131,10220,com.example.startuplab,next-top-activity,{com.example.startuplab/com.example.startuplab.MainActivity}]
10-09 22:18:28.971   681   741 I ActivityManager: Start proc 5131:com.example.startuplab/u0a220 for next-top-activity {com.example.startuplab/com.example.startuplab.MainActivity}
10-09 22:18:29.366  5131  5131 I StartupLab: source=Application event=onCreate pid=5131 instance=ab43fa3
10-09 22:18:29.412  5131  5131 I StartupLab: source=Activity event=onCreate pid=5131 instance=2937a2a
10-09 22:18:29.541  5131  5131 I StartupLab: source=Activity event=onStart pid=5131 instance=2937a2a
10-09 22:18:29.584  5131  5131 I StartupLab: source=Activity event=onResume pid=5131 instance=2937a2a
```

## 補助で使ったコマンド（contracts の一覧にないもの）

端末の変化（effect）を確かめるためだけに使った。実験の手順には入れない。

| コマンド | 目的 |
|----------|------|
| `adb emu avd name` | 接続しているエミュレータの AVD 名を確かめる（environment.md の avdName） |
| `adb shell dumpsys activity activities \| grep -m1 topResumedActivity` | 前面にある Activity を確かめる |
| `adb shell dumpsys package $PKG \| grep -m1 -o "stopped=[a-z]*"` | パッケージが停止状態（stopped state）かどうかを確かめる |
| `adb shell pm list packages $PKG` | アプリが入っているかを確かめる |

## 気づいたこと

- `instance` の値は、プロセスが違っても同じになることがある。PID 3908（S1）、4847、5131 のどれでも、
  Application は `ab43fa3`、Activity は `2937a2a` だった。`instance` で比べられるのは、同じ PID の中だけ。
  なぜ同じ値になるのかは未確認。
