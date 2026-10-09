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
4. C-LOG-1（`adb logcat -c`）… step=`start`
5. C-PS-1 … step=`before`（空のはず）
6. C-OP-1（ランチャーと同等の起動要求）… step=`launch`
7. C-PS-1 / C-PS-2 … step=`after-launch`
8. C-LOG-3 … step=`collect`

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
| 1. 起動するとプロセスが新しく作られる | | |
| 2. Application.onCreate はプロセスが新しく作られたときに呼ばれる | | |
| 3. Application.onCreate → Activity.onCreate の順で呼ばれる | | |
| 4. コールバックを呼び出しているのは OS | | |

## 確認できなかったこと・新しく出てきた疑問

（実験後に書く）

## 参考資料

（実験後に書く）
