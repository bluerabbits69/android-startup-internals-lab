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

（自分の仮説を確定させてから、data-model.md の参考仮説と比べて書く）

## 実験環境

[environment.md](./environment.md) を参照。この実験だけの差分: なし

## 実験手順

各 Trial で次の順に行う（research R7）。

1. C-ENV-3 … `0` であることを確かめる。`0` 以外なら C-ENV-4 を実行し、そのことを記録して、この試行をやり直す
2. C-OP-1（起動要求）… step=`launch`
3. C-PS-1 … step=`after-launch`
4. C-LOG-1（`adb logcat -c`）… step=`start`
5. C-OP-3（`am force-stop`）… step=`force-stop`
6. C-PS-1 … step=`after-force-stop`（空のはず）
7. C-OP-1（同じ起動要求で再起動）… step=`relaunch`
8. C-PS-1 / C-PS-2 … step=`after-relaunch`
9. C-LOG-3 … step=`collect`

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
| 1. force-stop でプロセスは消える | | |
| 2. force-stop で onDestroy が呼ばれる（確信度は低い） | | |
| 3. 再起動するとプロセスが新しく作られる | | |
| 4. Application.onCreate → Activity.onCreate の順で呼ばれる | | |

## 確認できなかったこと・新しく出てきた疑問

（実験後に書く）

## 参考資料

（実験後に書く）
