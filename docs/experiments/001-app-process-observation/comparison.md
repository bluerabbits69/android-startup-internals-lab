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

（T040 で書く）

## 後続 Feature の候補

（T040 で書く）

## 達成状況

（T039 で書く）
