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

（自分の仮説を確定させてから、data-model.md の参考仮説と比べて書く）

## 実験環境

[environment.md](./environment.md) を参照。この実験だけの差分: なし

## 実験手順

各 Trial で次の順に行う（research R7）。

1. C-OP-3（`am force-stop`）… step=`reset`（前の試行の状態を消す）
2. C-ENV-3 … `0` であることを確かめる。`0` 以外なら C-ENV-4 を実行し、そのことを記録して、この試行をやり直す
3. C-OP-1（起動要求）… step=`launch`
4. C-PS-1 … step=`after-launch`
5. C-LOG-1（`adb logcat -c`）… step=`start`
6. C-OP-2（HOME）… step=`home`
7. C-PS-1 … step=`after-home`
8. **30秒待つ**
9. C-PS-1 … step=`after-home+30s`
10. C-OP-1（同じ起動要求で復帰）… step=`return`
11. C-PS-1 / C-PS-2 … step=`after-return`
12. C-LOG-3 … step=`collect`

## 観測結果

### Trial 1 （YYYY-MM-DD HH:MM）
| step | pidof | PPID（親プロセス名） | 備考 |
|------|-------|---------------------|------|

**本アプリのログ**（log-format.md の照合ルールを通ったもの）
```
（ログをそのまま貼る）
```

**OS のプロセス起動記録**
- Start proc: （原文 / 出力なし）
- am_proc_start: （原文 / 出力なし）
- LaunchState: （値 / 出力なし）
- PID の照合: （一致 / 不一致 / 判定できない）

**照合ルールで除いた行**: （なし / 内容）

**手順どおりにできなかったこと**: （なし / 内容）

### Trial 2 （YYYY-MM-DD HH:MM）

（Trial 1 と同じ形式で書く）

### Trial 3 （YYYY-MM-DD HH:MM）

（Trial 1 と同じ形式で書く）

### 試行間の差

（実験後に書く）

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
