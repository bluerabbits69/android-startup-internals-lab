# EXP-2: バックグラウンドからの復帰

## 調査目的

- Q2: Activityの起動とプロセスの生成は同じ出来事なのか？
- Q3: アプリをバックグラウンドに移動すると、プロセスは終了するのか？
- Q5: ApplicationとActivityのライフサイクルは、どのような順序・関係で発生するのか？

前面に出したアプリをホームへ移動し、30秒待ってから同じ起動要求で戻したときに、
プロセスと Activity が作り直されるかどうかを確かめる。

## 仮説

### 自分の仮説

（2026-10-09、実験の前に書いた）

1. ホームへ移動しても、30秒待つ程度なら**プロセスは生き残る**（PID は変わらない）。
   ただし、メモリが逼迫していれば OS がプロセスを終了させる可能性はある。
   その場合は spec の Edge Cases に従って「別のケース」として記録する。
2. プロセスが新しく作られないので、復帰しても **`Application.onCreate` は呼ばれない**
   （EXP-1 の仮説2「Application.onCreate はプロセスが新しく作られたときに呼ばれる」から導いた）。
3. 復帰しても **`Activity.onCreate` は呼ばれない**。代わりに
   **`onRestart` → `onStart` → `onResume`** の順で呼ばれる。
   根拠: [資料] [アクティビティのライフサイクル](https://developer.android.com/guide/components/activities/activity-lifecycle?hl=ja)
   （onStop の後に再表示されると、この順で呼ばれる）
4. 復帰の前後で Activity の **`instance` は同じ値**になる（同じインスタンスが使われ続ける）。

ホームへ移動したときに `onPause` → `onStop` が出ることは、T017 で確認済み（[環境で確認]、1回だけ）。

### 参考仮説との違い

（2026-10-09、自分の仮説を確定させた後に data-model.md「状態遷移」と比べた）

- プロセス: 違いなし（どちらも PID は変わらない）。自分の仮説には、メモリが逼迫したら OS が終了させる可能性も含めている
- Activity: 違いなし（どちらも `onRestart → onStart → onResume`、instance は同じ）
- 参考仮説には、`Application.onCreate` が呼ばれるかどうか（仮説2）についての記述がない

## 実験環境

[environment.md](./environment.md) を参照。この実験だけの差分: なし

## 実験手順

各 Trial で次の順に行う（research R7）。

1. C-OP-3（`am force-stop`）… step=`reset`（前の試行の状態を消す）
2. C-ENV-3 … `0` であることを確かめる。`0` 以外なら C-ENV-4 を実行し、そのことを記録して、この試行をやり直す
3. C-OP-1（起動要求）… step=`launch`
4. C-PS-1 … step=`after-launch`
5. C-LOG-1（`adb logcat -b main,system,events -c`）… step=`start`
6. C-OP-2（HOME）… step=`home`
7. C-PS-1 … step=`after-home`
8. **30秒待つ**
9. C-PS-1 … step=`after-home+30s`
10. C-OP-1（同じ起動要求で復帰）… step=`return`
11. C-PS-1 / C-PS-2 … step=`after-return`
12. C-LOG-3 … step=`collect`

## 観測結果

時刻はホスト（Mac）の時計。ログの時刻はエミュレータの時計で、ホストより約15〜18秒遅れていた。
C-LOG-1 は手順5（after-launch の後）で実行したので、最初の起動のログは記録の範囲に入っていない。

### Trial 1 （2026-10-09 22:35）
| step | pidof | PPID（親プロセス名） | 備考 |
|------|-------|---------------------|------|
| reset | — | — | C-OP-3 |
| C-ENV-3 | — | — | `0` |
| launch | — | — | `LaunchState: COLD` |
| after-launch | 5701 | — | |
| after-home | 5701 | — | HOME の約1秒後 |
| after-home+30s | 5701 | — | |
| return | — | — | `Warning: Activity not started, its current task has been brought to the front`、`LaunchState: HOT` |
| after-return | 5701 | 464（zygote64） | |

**本アプリのログ**（log-format.md の照合ルールを通ったもの）
```
10-09 22:35:09.433  5701  5701 I StartupLab: source=Activity event=onPause pid=5701 instance=2937a2a
10-09 22:35:10.528  5701  5701 I StartupLab: source=Activity event=onStop pid=5701 instance=2937a2a
10-09 22:35:40.883  5701  5701 I StartupLab: source=Activity event=onRestart pid=5701 instance=2937a2a
10-09 22:35:40.884  5701  5701 I StartupLab: source=Activity event=onStart pid=5701 instance=2937a2a
10-09 22:35:40.885  5701  5701 I StartupLab: source=Activity event=onResume pid=5701 instance=2937a2a
```

**OS のプロセス起動記録**
- Start proc: 出力なし
- am_proc_start: 出力なし
- LaunchState: 復帰（return）のとき `HOT`
- PID の照合: 一致（logcat の PID 列、`pid=`、`pidof` がすべて同じ値）

**照合ルールで除いた行**: なし

**手順どおりにできなかったこと**: なし

### Trial 2 （2026-10-09 22:35）
| step | pidof | PPID（親プロセス名） | 備考 |
|------|-------|---------------------|------|
| reset | — | — | C-OP-3 |
| C-ENV-3 | — | — | `0` |
| launch | — | — | `LaunchState: COLD` |
| after-launch | 5770 | — | |
| after-home | 5770 | — | HOME の約1秒後 |
| after-home+30s | 5770 | — | |
| return | — | — | `Warning: Activity not started, its current task has been brought to the front`、`LaunchState: HOT` |
| after-return | 5770 | 464（zygote64） | |

**本アプリのログ**（log-format.md の照合ルールを通ったもの）
```
10-09 22:35:42.165  5770  5770 I StartupLab: source=Activity event=onPause pid=5770 instance=2937a2a
10-09 22:35:43.276  5770  5770 I StartupLab: source=Activity event=onStop pid=5770 instance=2937a2a
10-09 22:36:13.641  5770  5770 I StartupLab: source=Activity event=onRestart pid=5770 instance=2937a2a
10-09 22:36:13.641  5770  5770 I StartupLab: source=Activity event=onStart pid=5770 instance=2937a2a
10-09 22:36:13.646  5770  5770 I StartupLab: source=Activity event=onResume pid=5770 instance=2937a2a
```

**OS のプロセス起動記録**
- Start proc: 出力なし
- am_proc_start: 出力なし
- LaunchState: 復帰（return）のとき `HOT`
- PID の照合: 一致（logcat の PID 列、`pid=`、`pidof` がすべて同じ値）

**照合ルールで除いた行**: なし

**手順どおりにできなかったこと**: なし

### Trial 3 （2026-10-09 22:36）
| step | pidof | PPID（親プロセス名） | 備考 |
|------|-------|---------------------|------|
| reset | — | — | C-OP-3 |
| C-ENV-3 | — | — | `0` |
| launch | — | — | `LaunchState: COLD` |
| after-launch | 5838 | — | |
| after-home | 5838 | — | HOME の約1秒後 |
| after-home+30s | 5838 | — | |
| return | — | — | `Warning: Activity not started, its current task has been brought to the front`、`LaunchState: HOT` |
| after-return | 5838 | 464（zygote64） | |

**本アプリのログ**（log-format.md の照合ルールを通ったもの）
```
10-09 22:36:14.638  5838  5838 I StartupLab: source=Activity event=onPause pid=5838 instance=2937a2a
10-09 22:36:15.703  5838  5838 I StartupLab: source=Activity event=onStop pid=5838 instance=2937a2a
10-09 22:36:45.873  5838  5838 I StartupLab: source=Activity event=onRestart pid=5838 instance=2937a2a
10-09 22:36:45.874  5838  5838 I StartupLab: source=Activity event=onStart pid=5838 instance=2937a2a
10-09 22:36:45.878  5838  5838 I StartupLab: source=Activity event=onResume pid=5838 instance=2937a2a
```

**OS のプロセス起動記録**
- Start proc: 出力なし
- am_proc_start: 出力なし
- LaunchState: 復帰（return）のとき `HOT`
- PID の照合: 一致（logcat の PID 列、`pid=`、`pidof` がすべて同じ値）

**照合ルールで除いた行**: なし

**手順どおりにできなかったこと**: なし

### 試行間の差

- PID は試行ごとに違った（5701 / 5770 / 5838）が、どの試行でも1つの試行の中では最後まで同じ PID だった。
- それ以外（ログの種類と順番、`LaunchState`、`instance` の値）は3回とも同じだった。
- 3回とも、ホームにいる30秒の間にプロセスは消えなかった（spec Edge Cases の「別のケース」は起きなかった）。

## ソースコードで確認した事実

（実験後に書く。読んでいなければ「なし」）

## 考察

（実験後に書く）

## 仮説との照合

| 仮説 | 結果 | 根拠 |
|------|------|------|
| 1. 30秒ではプロセスは生き残る（PID は変わらない） | | |
| 2. 復帰しても Application.onCreate は呼ばれない | | |
| 3. Activity.onCreate は呼ばれず、onRestart → onStart → onResume の順で呼ばれる | | |
| 4. 復帰の前後で Activity の instance は同じ | | |

## 確認できなかったこと・新しく出てきた疑問

（実験後に書く）

## 参考資料

- [アクティビティのライフサイクル](https://developer.android.com/guide/components/activities/activity-lifecycle?hl=ja)（仮説3の根拠）
