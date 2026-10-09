# 実験の比較（001-app-process-observation）

3つの実験記録（FR-016、data-model.md §7）を並べて比べる。
環境は [environment.md](./environment.md)（API 36、`Pixel_5_API_36`）。各実験は3回ずつ行った。

- [EXP-1: 初回起動](./exp1-first-launch.md)
- [EXP-2: バックグラウンドからの復帰](./exp2-background-return.md)
- [EXP-3: 強制停止後の再起動](./exp3-force-stop-relaunch.md)

## 比較表

| 実験 | 操作前の PID → 操作後の PID | Application.onCreate | Activity のイベント列 | Activity の instance | OS のプロセス起動記録 | 試行間の差 |
|------|----------------------------|----------------------|----------------------|---------------------|----------------------|-----------|
| EXP-1 初回起動 | なし → 新しい PID（5492 / 5580 / 5652） | 出た | 起動: onCreate → onStart → onResume | 比較対象なし（操作前に Activity がない） | あり（`am_proc_start`・`Start proc`）。`LaunchState: COLD` | PID と UID（10221 / 10222 / 10223）が試行ごとに違った。それ以外は同じ |
| EXP-2 復帰 | 変わらない（5701 → 5701、5770 → 5770、5838 → 5838） | 出なかった | HOME: onPause → onStop／復帰: onRestart → onStart → onResume | 同じ（`2937a2a`。同じ PID の中なので比べられる） | なし。`LaunchState: HOT` | PID が試行ごとに違った。それ以外は同じ |
| EXP-3 強制停止後 | 変わった（5838 → 5919、5919 → 5979、5979 → 6039）。force-stop の後は一度なくなった | 出た（再起動のとき） | force-stop: 何も出なかった（onPause・onStop・onDestroy なし）／再起動: onCreate → onStart → onResume | 比較できない（値は同じ `2937a2a` だが、プロセスが違う。下の注を参照） | 再起動のとき、あり（`am_proc_start`・`Start proc`）。`LaunchState: COLD`。force-stop のとき `Killing` と `am_kill`（`am_proc_died` はなし） | PID が試行ごとに違った。それ以外は同じ |

**注（instance）**: `instance` の値は、プロセスが違っても毎回同じだった（Application `ab43fa3`、Activity `2937a2a`。
EXP-1〜3 と quickstart のすべての起動）。そのため、`instance` で「同じインスタンスかどうか」を判断できるのは、
同じ PID の中だけ。なぜ同じ値になるのかは未確認（adb-commands.md の「気づいたこと」）。

## Q1〜Q5 への回答

学習者の回答（2026-10-09、T036）。根拠の実験と試行の番号を付けた。結論はすべて API 36 で確認したもので、他のバージョンは未確認。

### Q1: アプリ起動時に、プロセスはどのような状態になるのか？

起動する前にプロセスがなければ、プロセスが新しく作られる（EXP-1 Trial 1〜3、EXP-3 Trial 1〜3：起動前の `pidof` が空、起動後に新しい PID、`am_proc_start` と `Start proc` が出た）。
起動する前にプロセスが残っていれば、新しくは作られず、同じプロセスの上で Activity が再開する（onRestart から）（EXP-2 Trial 1〜3：PID が変わらず、`am_proc_start` が出なかった）。

### Q2: Activityの起動とプロセスの生成は同じ出来事なのか？

同じ出来事ではない。プロセスがないときは、プロセスが生成されてから Activity が起動される
（EXP-1・EXP-3 Trial 1〜3：PID が新しくなり、`Start proc` → `Application.onCreate` → `Activity.onCreate` の順に出た）。
プロセスが残っているときは、プロセスは生成されず、同じプロセスの上で Activity のコールバックだけが呼ばれる
（EXP-2 Trial 1〜3：PID は変わらず、`Application.onCreate` は出ずに、onRestart → onStart → onResume が出た）。

SC-007 の根拠: PID の変化（EXP-1・EXP-3 では変わり、EXP-2 では変わらない）と、`Application.onCreate` が出たかどうか（EXP-1・EXP-3 では出て、EXP-2 では出ない）。
OS 側の `LaunchState` も、EXP-1・EXP-3 は `COLD`、EXP-2 は `HOT` だった。

### Q3: アプリをバックグラウンドに移動すると、プロセスは終了するのか？

少なくとも30秒では終了しなかった（EXP-2 Trial 1〜3：HOME の後も30秒後も PID は同じ）。
ただし、長い間使わずにいると終了する場合があると認識している（未確認。後続 Feature で確かめる）。

### Q4: アプリを強制停止して再起動すると、何が変わるのか？

強制停止するとプロセスが kill される（EXP-3 Trial 1〜3：`pidof` が空、`Killing` と `am_kill` が出た）。
そのとき、onPause・onStop・onDestroy は呼ばれなかったと考えられる（ログからの判断）。
再起動すると、プロセスが新しく作られて（PID が変わる）、`Application.onCreate` → `Activity.onCreate` の順に呼ばれる。
つまりコールドスタートになる（`LaunchState: COLD`）。

### Q5: ApplicationとActivityのライフサイクルは、どのような順序・関係で発生するのか？

プロセスが作られると、まず Application が作られ（`Application.onCreate`）、その後に Activity のライフサイクルが動く（EXP-1・EXP-3 Trial 1〜3）。
`Application.onCreate` はプロセスが作られたときに1回だけ呼ばれ、同じプロセスの中では、Activity のライフサイクルだけが何度も動く
（EXP-2 Trial 1〜3：HOME と復帰で onPause → onStop → onRestart → onStart → onResume が出たが、`Application.onCreate` は出なかった）。
Application の寿命はプロセスと一緒で、Activity はその中で生まれたり止まったりする。

## 振り返り

学習者の振り返り（2026-10-09、T040）。

- **コールドスタート・ホットスタート・ウォームスタートについて知れたのが良かった。**
  最初の仮説にはなかった観点。`am start -W` の `LaunchState`（COLD / HOT）を観測し、自分で見つけた資料
  （[アプリの起動時間](https://developer.android.com/topic/performance/issues/launch-time?hl=ja)）の定義と結びつけられた。
- **force-stop では、本当に即座にプロセスが kill されて、onPause や onStop も呼ばれていなかった。**
  唯一外れた仮説（onDestroy は呼ばれる）から得た学び。`Force stopping` から `Killing` まで 1〜4ms で、
  その間に本アプリのログが出なかった（ログからの判断。EXP-3 Trial 1〜3）。

残っていること: 学習者が自分の手で実験を再現すること（上の「達成状況」の SC-001）。

## 後続 Feature の候補

**spec と research で後回しにしたもの**

- 起動要求の形による違い（`am start -n` だけ、`monkey` など。Clarifications Q4）
- `am kill`（バックグラウンドのプロセスだけを終了する）と force-stop の比較
- 「アクティビティを保持しない」をオンにした場合（Clarifications Q3）。`LaunchState: WARM` を観測できるか
- Android のバージョン間の比較（今回は API 36 だけ）
- `attachBaseContext` と ContentProvider の初期化順序（Application の内部の初期化順序）
- AMS / ATMS / Zygote の内部（親プロセス `zygote64` は記録しただけ）

**実験で新しく出てきた疑問**

- コールバックを呼び出しているのは誰か（EXP-1 の仮説4。`ActivityThread` などを AOSP で読む）
- force-stop のとき、OS は本当にコールバックを呼ばずに kill しているのか（AOSP の force-stop の処理を読む）
- force-stop で `am_kill` は出るのに、`am_proc_died` が出ないのはなぜか
- `Force stopping ... from pid` の PID はどのプロセスか
- HOME の後 `onStop` が出る前に戻すと、`onPause` → `onResume` だけで、`LaunchState` が `UNKNOWN (0)` になるのはなぜか（T037 で発見）
- 再インストールのたびに UID が変わるのはなぜか（学習者が気になった点）
- `instance` の値が、プロセスをまたいでも同じになるのはなぜか
- バックグラウンドに長くいると、OS はいつプロセスを終了させるのか（Q3 の未確認部分）
- エミュレータのランチャー（NexusLauncher）が、AOSP の Launcher3 と同じ Intent で起動しているか

## 達成状況

spec の Success Criteria を一つずつ確かめた結果（2026-10-09、T039）。

| SC | 結果 | 根拠 |
|----|------|------|
| SC-001 端末に触れずにコマンドだけで起動と強制停止 | 一部達成 | コマンドだけで起動・強制停止できることは確認した（EXP-3 Trial 1〜3、quickstart S2）。ただし、コマンドを実行したのは AI で、**学習者が自分で3回行うのは未実施** |
| SC-002 ログで起動順を確認でき、復帰時に Application の初期化が出たか判別できる | 達成 | EXP-1・EXP-3 で Application → Activity の順。EXP-2 で Application.onCreate が出ないことを確認。すべての行で PID 照合が一致 |
| SC-003 同じ時点のログの PID とコマンドの PID が一致 | 達成 | quickstart S3、EXP-1〜3 の全 Trial で `pidof` とログの `pid=` が一致。force-stop の前後の PID の違いは観測結果として記録した（EXP-3 の試行間の差） |
| SC-004 手順書だけで各実験を3回以上再実施でき、試行間の差が記録されている | 達成 | 各実験を手順どおりに3回行った。試行間の差（PID、EXP-1 の UID）と、未検証であることを記録した |
| SC-005 3つの記録が7項目と実験環境を欠けなく含む | 達成 | 3つの記録すべてに、調査目的・仮説・実験環境・実験手順・観測結果・考察・参考資料の見出しがあり、未記入の欄がない。実験環境は environment.md を参照 |
| SC-006 Q1〜Q5 すべてに回答または「未確認」 | 達成 | 上の「Q1〜Q5 への回答」（学習者の言葉、実験と Trial の番号つき） |
| SC-007 「Activity の起動とプロセスの生成は同じではない」を PID と Application の初期化を根拠に説明 | 達成 | 上の Q2（EXP-2 では PID が変わらず Application.onCreate も出ないのに、Activity のコールバックが呼ばれた） |

**計画と違ったところ**: plan.md の Constitution Check VII では「実験は手でコマンドを打って行う」としていたが、
今回は AI がコマンドを1つずつ実行した（実験を自動化するスクリプトはリポジトリに入れていない）。
学習者が自分の手で実験を再現することは、SC-001 とあわせて残っている。
