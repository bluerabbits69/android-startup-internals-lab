# EXP-3: 強制停止後の再起動

## 調査目的

- Q2: Activityの起動とプロセスの生成は同じ出来事なのか？
- Q4: アプリを強制停止して再起動すると、何が変わるのか？
- Q5: ApplicationとActivityのライフサイクルは、どのような順序・関係で発生するのか？

前面に出したアプリを `am force-stop` で強制停止し、同じ起動要求でもう一度起動したときに、
プロセス・Application・Activity がどう変わるかを確かめる。

## 仮説

### 自分の仮説

（2026-10-09、実験の前に書いた）

1. `am force-stop` を実行すると**プロセスは消える**（`pidof` が空になる）。
2. 強制停止したときに **`onDestroy` は呼ばれる**。
   ただし確信度は低い（「わからないけど、呼ばれると思う」）。
3. 再起動すると**プロセスが新しく作られる**。
4. プロセスが新しく作られるので、**`Application.onCreate` が呼ばれ**、その後に
   **`Activity.onCreate` が呼ばれる**（EXP-1 の仮説2・3から導いた）。

### 参考仮説との違い

（2026-10-09、自分の仮説を確定させた後に data-model.md「状態遷移」と比べた）

- プロセス: 違いなし（どちらも force-stop でプロセスがなくなる）
- **onDestroy: 食い違っている**。自分の仮説は「呼ばれる」（確信度は低い）、参考仮説は「呼ばれないかもしれない（未確認）」。
  この実験でいちばん注目する点。`onDestroy` が出なかった場合に「呼ばれなかった」のか「呼ばれる前にプロセスが終了した」のかを
  区別するため、`am_kill` / `am_proc_died` の記録と合わせて見る
- 参考仮説には、再起動時の `Application.onCreate` と `Activity.onCreate`（仮説4）についての記述がない

## 実験環境

[environment.md](./environment.md) を参照。この実験だけの差分: なし

## 実験手順

各 Trial で次の順に行う（research R7）。

1. C-ENV-3 … `0` であることを確かめる。`0` 以外なら C-ENV-4 を実行し、そのことを記録して、この試行をやり直す
2. C-OP-1（起動要求）… step=`launch`
3. C-PS-1 … step=`after-launch`
4. C-LOG-1（`adb logcat -b main,system,events -c`）… step=`start`
5. C-OP-3（`am force-stop`）… step=`force-stop`
6. C-PS-1 … step=`after-force-stop`（空のはず）
7. C-OP-1（同じ起動要求で再起動）… step=`relaunch`
8. C-PS-1 / C-PS-2 … step=`after-relaunch`
9. C-LOG-3 … step=`collect`

## 観測結果

時刻はホスト（Mac）の時計。ログの時刻はエミュレータの時計で、ホストより約15〜18秒遅れていた。

### Trial 1 （2026-10-09 22:37）
| step | pidof | PPID（親プロセス名） | 備考 |
|------|-------|---------------------|------|
| C-ENV-3 | — | — | `0` |
| launch | — | — | `Warning: Activity not started, intent has been delivered to currently running top-most instance.`、`LaunchState: UNKNOWN (0)` |
| after-launch | 5838 | — | 前の手順で起動したプロセスがそのまま前面にあった |
| force-stop | — | — | 出力なし |
| after-force-stop | （空） | — | |
| relaunch | — | — | `LaunchState: COLD` |
| after-relaunch | 5919 | 464（zygote64） | |

**本アプリのログ**（log-format.md の照合ルールを通ったもの）
```
10-09 22:36:57.313  5919  5919 I StartupLab: source=Application event=onCreate pid=5919 instance=ab43fa3
10-09 22:36:57.331  5919  5919 I StartupLab: source=Activity event=onCreate pid=5919 instance=2937a2a
10-09 22:36:57.358  5919  5919 I StartupLab: source=Activity event=onStart pid=5919 instance=2937a2a
10-09 22:36:57.370  5919  5919 I StartupLab: source=Activity event=onResume pid=5919 instance=2937a2a
```

**OS のプロセス起動記録**
- Start proc: 
  ```
  10-09 22:36:57.154   681   741 I ActivityManager: Start proc 5919:com.example.startuplab/u0a223 for next-top-activity {com.example.startuplab/com.example.startuplab.MainActivity}
  ```
- am_proc_start: 
  ```
  10-09 22:36:57.154   681   741 I am_proc_start: [0,5919,10223,com.example.startuplab,next-top-activity,{com.example.startuplab/com.example.startuplab.MainActivity}]
  ```
- LaunchState: 再起動（relaunch）のとき `COLD`
- PID の照合: 一致（logcat の PID 列、`pid=`、`pidof` がすべて同じ値）
- プロセス終了の記録（force-stop のとき）:
  ```
  10-09 22:36:56.963   681   697 I ActivityManager: Force stopping com.example.startuplab appid=10223 user=0: from pid 5908
  10-09 22:36:56.967   681   697 I ActivityManager: Killing 5838:com.example.startuplab/u0a223 (adj 0): stop com.example.startuplab due to from pid 5908
  10-09 22:36:56.967   681   697 I am_kill : [0,5838,com.example.startuplab,0,stop com.example.startuplab due to from pid 5908,131460]
  ```
- am_proc_died: 出力なし
- `onPause`・`onStop`・`onDestroy`: 本アプリのログに出なかった

**照合ルールで除いた行**: なし

**手順どおりにできなかったこと**: なし。ただし、手順に開始前のリセットがないため、step=`launch` は毎回、前面にある既存のプロセスに届いた（Trial 1 は EXP-2 Trial 3 のプロセス 5838）。

### Trial 2 （2026-10-09 22:37）
| step | pidof | PPID（親プロセス名） | 備考 |
|------|-------|---------------------|------|
| C-ENV-3 | — | — | `0` |
| launch | — | — | `Warning: Activity not started, intent has been delivered to currently running top-most instance.`、`LaunchState: UNKNOWN (0)` |
| after-launch | 5919 | — | 前の手順で起動したプロセスがそのまま前面にあった |
| force-stop | — | — | 出力なし |
| after-force-stop | （空） | — | |
| relaunch | — | — | `LaunchState: COLD` |
| after-relaunch | 5979 | 464（zygote64） | |

**本アプリのログ**（log-format.md の照合ルールを通ったもの）
```
10-09 22:36:58.017  5979  5979 I StartupLab: source=Application event=onCreate pid=5979 instance=ab43fa3
10-09 22:36:58.033  5979  5979 I StartupLab: source=Activity event=onCreate pid=5979 instance=2937a2a
10-09 22:36:58.066  5979  5979 I StartupLab: source=Activity event=onStart pid=5979 instance=2937a2a
10-09 22:36:58.072  5979  5979 I StartupLab: source=Activity event=onResume pid=5979 instance=2937a2a
```

**OS のプロセス起動記録**
- Start proc: 
  ```
  10-09 22:36:57.875   681   741 I ActivityManager: Start proc 5979:com.example.startuplab/u0a223 for next-top-activity {com.example.startuplab/com.example.startuplab.MainActivity}
  ```
- am_proc_start: 
  ```
  10-09 22:36:57.875   681   741 I am_proc_start: [0,5979,10223,com.example.startuplab,next-top-activity,{com.example.startuplab/com.example.startuplab.MainActivity}]
  ```
- LaunchState: 再起動（relaunch）のとき `COLD`
- PID の照合: 一致（logcat の PID 列、`pid=`、`pidof` がすべて同じ値）
- プロセス終了の記録（force-stop のとき）:
  ```
  10-09 22:36:57.772   681   995 I ActivityManager: Force stopping com.example.startuplab appid=10223 user=0: from pid 5968
  10-09 22:36:57.773   681   995 I ActivityManager: Killing 5919:com.example.startuplab/u0a223 (adj 0): stop com.example.startuplab due to from pid 5968
  10-09 22:36:57.776   681   995 I am_kill : [0,5919,com.example.startuplab,0,stop com.example.startuplab due to from pid 5968,130728]
  ```
- am_proc_died: 出力なし
- `onPause`・`onStop`・`onDestroy`: 本アプリのログに出なかった

**照合ルールで除いた行**: なし

**手順どおりにできなかったこと**: なし。ただし、手順に開始前のリセットがないため、step=`launch` は毎回、前面にある既存のプロセスに届いた（Trial 1 は EXP-2 Trial 3 のプロセス 5838）。

### Trial 3 （2026-10-09 22:37）
| step | pidof | PPID（親プロセス名） | 備考 |
|------|-------|---------------------|------|
| C-ENV-3 | — | — | `0` |
| launch | — | — | `Warning: Activity not started, intent has been delivered to currently running top-most instance.`、`LaunchState: UNKNOWN (0)` |
| after-launch | 5979 | — | 前の手順で起動したプロセスがそのまま前面にあった |
| force-stop | — | — | 出力なし |
| after-force-stop | （空） | — | |
| relaunch | — | — | `LaunchState: COLD` |
| after-relaunch | 6039 | 464（zygote64） | |

**本アプリのログ**（log-format.md の照合ルールを通ったもの）
```
10-09 22:36:58.763  6039  6039 I StartupLab: source=Application event=onCreate pid=6039 instance=ab43fa3
10-09 22:36:58.781  6039  6039 I StartupLab: source=Activity event=onCreate pid=6039 instance=2937a2a
10-09 22:36:58.808  6039  6039 I StartupLab: source=Activity event=onStart pid=6039 instance=2937a2a
10-09 22:36:58.814  6039  6039 I StartupLab: source=Activity event=onResume pid=6039 instance=2937a2a
```

**OS のプロセス起動記録**
- Start proc: 
  ```
  10-09 22:36:58.633   681   741 I ActivityManager: Start proc 6039:com.example.startuplab/u0a223 for next-top-activity {com.example.startuplab/com.example.startuplab.MainActivity}
  ```
- am_proc_start: 
  ```
  10-09 22:36:58.633   681   741 I am_proc_start: [0,6039,10223,com.example.startuplab,next-top-activity,{com.example.startuplab/com.example.startuplab.MainActivity}]
  ```
- LaunchState: 再起動（relaunch）のとき `COLD`
- PID の照合: 一致（logcat の PID 列、`pid=`、`pidof` がすべて同じ値）
- プロセス終了の記録（force-stop のとき）:
  ```
  10-09 22:36:58.519   681  2217 I ActivityManager: Force stopping com.example.startuplab appid=10223 user=0: from pid 6027
  10-09 22:36:58.520   681  2217 I ActivityManager: Killing 5979:com.example.startuplab/u0a223 (adj 0): stop com.example.startuplab due to from pid 6027
  10-09 22:36:58.520   681  2217 I am_kill : [0,5979,com.example.startuplab,0,stop com.example.startuplab due to from pid 6027,136888]
  ```
- am_proc_died: 出力なし
- `onPause`・`onStop`・`onDestroy`: 本アプリのログに出なかった

**照合ルールで除いた行**: なし

**手順どおりにできなかったこと**: なし。ただし、手順に開始前のリセットがないため、step=`launch` は毎回、前面にある既存のプロセスに届いた（Trial 1 は EXP-2 Trial 3 のプロセス 5838）。

### 試行間の差

- PID は試行ごとに違った。force-stop の前後で、PID は 5838 → 5919、5919 → 5979、5979 → 6039 と変わった。
- `Force stopping ... from pid` の PID（5908 / 5968 / 6027）は試行ごとに違った。どのプロセスかは未確認。
- それ以外（ログの種類と順番、`am_kill` が出て `am_proc_died` が出ないこと、`instance` の値）は3回とも同じだった。

## ソースコードで確認した事実

（実験後に書く。読んでいなければ「なし」）

## 考察

（実験後に書く）

## 仮説との照合

| 仮説 | 結果 | 根拠 |
|------|------|------|
| 1. force-stop でプロセスは消える | | |
| 2. force-stop で onDestroy が呼ばれる（確信度は低い） | | |
| 3. 再起動するとプロセスが新しく作られる | | |
| 4. Application.onCreate → Activity.onCreate の順で呼ばれる | | |

## 確認できなかったこと・新しく出てきた疑問

（実験後に書く）

## 参考資料

（実験後に書く）
