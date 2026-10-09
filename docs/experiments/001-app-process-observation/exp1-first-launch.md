# EXP-1: 初回起動

## 調査目的

- Q1: アプリ起動時に、プロセスはどのような状態になるのか？
- Q2: Activityの起動とプロセスの生成は同じ出来事なのか？
- Q5: ApplicationとActivityのライフサイクルは、どのような順序・関係で発生するのか？

インストール直後で一度も起動していない状態から起動したときに、プロセスが新しく作られるか、
Application と Activity の onCreate がどの順で呼ばれるかを確かめる。

## 仮説

### 自分の仮説

（2026-10-09、実験の前に書いた）

1. 起動前にはプロセスはなく、起動すると**プロセスが新しく作られる**。
2. `Application.onCreate` は**プロセスが新しく作られたとき**に呼ばれる。
   根拠: [資料] どこかで読んだ記憶（出どころは未確認）。
3. したがって初回起動では `Application.onCreate` が呼ばれ、その後に `Activity.onCreate` が呼ばれる。
   順番は Application → MainActivity。
   根拠: [環境で確認] quickstart S1（プロセスがない状態から起動、`LaunchState: COLD`）で、
   同じ PID の中で `Application onCreate` が `Activity onCreate` より約100ms先に出た（1回だけ確認）。
4. `onCreate()` などのコールバックを**呼び出しているのは OS**。自分のコードの中には呼び出す行がない。
   根拠: なんとなく（確信度は低い）。誰がどう呼んでいるかは、AOSP を読んで確かめる（T021, T027）。

前提にしている理解（T018）: Application はアプリの入口で、一番最初に実行されるもの。Activity は画面。

### 参考仮説との違い

（2026-10-09、自分の仮説を確定させた後に data-model.md「状態遷移」と比べた）

- プロセス: 違いなし（どちらも、起動要求でプロセスが新しく作られる）
- Activity: 参考仮説は `onCreate → onStart → onResume` まで予想している。自分の仮説は `onCreate` だけ
  （onStart と onResume は quickstart S1 で一度出ている）
- 参考仮説には、`Application.onCreate`（仮説2・3）と、誰が呼び出しているか（仮説4）についての記述がない

## 実験環境

[environment.md](./environment.md) を参照。この実験だけの差分: なし

## 実験手順

各 Trial で次の順に行う（research R7）。

1. C-INS-2（`adb uninstall com.example.startuplab`）… step=`uninstall`
2. C-INS-1（`adb install -r app/build/outputs/apk/debug/app-debug.apk`）… step=`install`
3. C-ENV-3 … `0` であることを確かめる。`0` 以外なら C-ENV-4 を実行し、そのことを記録して、この試行をやり直す
4. C-LOG-1（`adb logcat -b main,system,events -c`）… step=`start`
5. C-PS-1 … step=`before`（空のはず）
6. C-OP-1（ランチャーと同等の起動要求）… step=`launch`
7. C-PS-1 / C-PS-2 … step=`after-launch`
8. C-LOG-3 … step=`collect`

## 観測結果

時刻はホスト（Mac）の時計。ログの時刻はエミュレータの時計で、ホストより約15〜18秒遅れていた。

### Trial 1 （2026-10-09 22:35）
| step | pidof | PPID（親プロセス名） | 備考 |
|------|-------|---------------------|------|
| uninstall | — | — | `Success` |
| install | — | — | `Success` |
| C-ENV-3 | — | — | `0` |
| before | （空） | — | |
| launch | — | — | `Status: ok`、`LaunchState: COLD` |
| after-launch | 5492 | 464（zygote64） | USER `u0_a221` |

**本アプリのログ**（log-format.md の照合ルールを通ったもの）
```
10-09 22:34:51.858  5492  5492 I StartupLab: source=Application event=onCreate pid=5492 instance=ab43fa3
10-09 22:34:51.912  5492  5492 I StartupLab: source=Activity event=onCreate pid=5492 instance=2937a2a
10-09 22:34:52.014  5492  5492 I StartupLab: source=Activity event=onStart pid=5492 instance=2937a2a
10-09 22:34:52.042  5492  5492 I StartupLab: source=Activity event=onResume pid=5492 instance=2937a2a
```

**OS のプロセス起動記録**
- Start proc: 
  ```
  10-09 22:34:51.474   681   741 I ActivityManager: Start proc 5492:com.example.startuplab/u0a221 for next-top-activity {com.example.startuplab/com.example.startuplab.MainActivity}
  ```
- am_proc_start: 
  ```
  10-09 22:34:51.474   681   741 I am_proc_start: [0,5492,10221,com.example.startuplab,next-top-activity,{com.example.startuplab/com.example.startuplab.MainActivity}]
  ```
- LaunchState: `COLD`
- PID の照合: 一致（logcat の PID 列、`pid=`、`pidof` がすべて同じ値）

**照合ルールで除いた行**: なし

**手順どおりにできなかったこと**: なし

### Trial 2 （2026-10-09 22:35）
| step | pidof | PPID（親プロセス名） | 備考 |
|------|-------|---------------------|------|
| uninstall | — | — | `Success` |
| install | — | — | `Success` |
| C-ENV-3 | — | — | `0` |
| before | （空） | — | |
| launch | — | — | `Status: ok`、`LaunchState: COLD` |
| after-launch | 5580 | 464（zygote64） | USER `u0_a222` |

**本アプリのログ**（log-format.md の照合ルールを通ったもの）
```
10-09 22:34:54.024  5580  5580 I StartupLab: source=Application event=onCreate pid=5580 instance=ab43fa3
10-09 22:34:54.052  5580  5580 I StartupLab: source=Activity event=onCreate pid=5580 instance=2937a2a
10-09 22:34:54.104  5580  5580 I StartupLab: source=Activity event=onStart pid=5580 instance=2937a2a
10-09 22:34:54.115  5580  5580 I StartupLab: source=Activity event=onResume pid=5580 instance=2937a2a
```

**OS のプロセス起動記録**
- Start proc: 
  ```
  10-09 22:34:53.725   681   741 I ActivityManager: Start proc 5580:com.example.startuplab/u0a222 for next-top-activity {com.example.startuplab/com.example.startuplab.MainActivity}
  ```
- am_proc_start: 
  ```
  10-09 22:34:53.725   681   741 I am_proc_start: [0,5580,10222,com.example.startuplab,next-top-activity,{com.example.startuplab/com.example.startuplab.MainActivity}]
  ```
- LaunchState: `COLD`
- PID の照合: 一致（logcat の PID 列、`pid=`、`pidof` がすべて同じ値）

**照合ルールで除いた行**: なし

**手順どおりにできなかったこと**: なし

### Trial 3 （2026-10-09 22:35）
| step | pidof | PPID（親プロセス名） | 備考 |
|------|-------|---------------------|------|
| uninstall | — | — | `Success` |
| install | — | — | `Success` |
| C-ENV-3 | — | — | `0` |
| before | （空） | — | |
| launch | — | — | `Status: ok`、`LaunchState: COLD` |
| after-launch | 5652 | 464（zygote64） | USER `u0_a223` |

**本アプリのログ**（log-format.md の照合ルールを通ったもの）
```
10-09 22:34:55.719  5652  5652 I StartupLab: source=Application event=onCreate pid=5652 instance=ab43fa3
10-09 22:34:55.754  5652  5652 I StartupLab: source=Activity event=onCreate pid=5652 instance=2937a2a
10-09 22:34:55.792  5652  5652 I StartupLab: source=Activity event=onStart pid=5652 instance=2937a2a
10-09 22:34:55.803  5652  5652 I StartupLab: source=Activity event=onResume pid=5652 instance=2937a2a
```

**OS のプロセス起動記録**
- Start proc: 
  ```
  10-09 22:34:55.502   681   741 I ActivityManager: Start proc 5652:com.example.startuplab/u0a223 for next-top-activity {com.example.startuplab/com.example.startuplab.MainActivity}
  ```
- am_proc_start: 
  ```
  10-09 22:34:55.502   681   741 I am_proc_start: [0,5652,10223,com.example.startuplab,next-top-activity,{com.example.startuplab/com.example.startuplab.MainActivity}]
  ```
- LaunchState: `COLD`
- PID の照合: 一致（logcat の PID 列、`pid=`、`pidof` がすべて同じ値）

**照合ルールで除いた行**: なし

**手順どおりにできなかったこと**: なし

### 試行間の差

- PID は試行ごとに違った（5492 / 5580 / 5652）。
- UID も試行ごとに違った（10221 / 10222 / 10223。`ps` の USER は `u0_a221` / `u0_a222` / `u0_a223`）。原因は未検証。
- それ以外（ログの種類と順番、`LaunchState: COLD`、親プロセス `zygote64`（PID 464）、`instance` の値）は3回とも同じだった。

## ソースコードで確認した事実

- [AOSPで確認] `am_proc_start` と `Start proc` は、`system_server` の `ProcessList.handleProcessStartedLocked()` が、
  プロセスの PID が決まった直後に、この順で出力している（frameworks/base `android-16.0.0_r1`、
  [ProcessList.java#2864](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/services/core/java/com/android/server/am/ProcessList.java#2864)、[#2881](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/services/core/java/com/android/server/am/ProcessList.java#2881)。詳しくは research.md R6）。

## 考察

- 初回起動では、OS（`system_server`）がプロセスを新しく作り、その PID の中で `Application.onCreate` → `Activity.onCreate`
  → `onStart` → `onResume` の順に呼ばれたと考えられる（根拠: Trial 1〜3。`am_proc_start` / `Start proc` の PID と、
  本アプリのログの `pid=`、`pidof` が一致。`Start proc` から `Application.onCreate` まで約0.1〜0.4秒）。
- `LaunchState: COLD` は、プロセスを新しく作った起動で出た（根拠: Trial 1〜3）。[資料] [アプリの起動時間](https://developer.android.com/topic/performance/issues/launch-time?hl=ja) の
  「コールド スタートとは、アプリをゼロからスタートさせること」という定義と合っている。
- コールバックを呼び出しているのが OS かどうかは、ログだけでは判断できない。ログから言えるのは、
  OS がプロセスを作った後に、同じ PID の中でコールバックが呼ばれたことまで。

結論: API 36 で確認。他のバージョンは未確認。

## 仮説との照合

| 仮説 | 結果 | 根拠 |
|------|------|------|
| 1. 起動するとプロセスが新しく作られる | 一致 | Trial 1〜3: before の `pidof` が空、after-launch で新しい PID。`am_proc_start` が出た |
| 2. Application.onCreate はプロセスが新しく作られたときに呼ばれる | 一致 | Trial 1〜3: `Start proc` の後、同じ PID で `Application onCreate` が出た |
| 3. Application.onCreate → Activity.onCreate の順で呼ばれる | 一致 | Trial 1〜3 の本アプリのログ |
| 4. コールバックを呼び出しているのは OS | 判定できない | ログには「誰が呼んだか」が出ない。AOSP の `ActivityThread` などを読む必要がある（後続 Feature） |

## 確認できなかったこと・新しく出てきた疑問

- コールバックを呼び出しているのは誰か（仮説4）。AOSP を読んで確かめる。
- アンインストールして入れ直すたびに、UID が変わった（10221 → 10222 → 10223）。なぜか未確認。
- `instance` の値が、プロセスが違っても毎回同じ（Application `ab43fa3`、Activity `2937a2a`）。なぜか未確認。
  プロセスをまたいで `instance` を比べることはできない。
- `LaunchState` の WARM はどんなときに出るか。[資料] [アプリの起動時間](https://developer.android.com/topic/performance/issues/launch-time?hl=ja) によると、ウォームスタートは
  「プロセスはまだ実行中の可能性があるが、アクティビティを `onCreate` で再作成する」場合や、「システムがメモリからアプリを削除した後に再起動し、
  保存済みインスタンスの状態バンドルを利用できる」場合。このページの分類と `am start -W` の `LaunchState` が同じ基準かどうかは未確認。

## 参考資料

- [ProcessList.java（android-16.0.0_r1）](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/services/core/java/com/android/server/am/ProcessList.java#2864): `am_proc_start` と `Start proc` を出力している箇所
- [EventLogTags.logtags（android-16.0.0_r1）](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/services/core/java/com/android/server/am/EventLogTags.logtags#25): `am_proc_start` の形式
- [Launcher3 AppInfo.java（android-16.0.0_r1）](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/tags/android-16.0.0_r1/src/com/android/launcher3/model/data/AppInfo.java#167): ランチャーが起動に使う Intent
- [アプリの起動時間](https://developer.android.com/topic/performance/issues/launch-time?hl=ja)（「アプリのさまざまな起動状態の理解」: コールド / ウォーム / ホットスタートの定義。学習者が見つけた資料）
