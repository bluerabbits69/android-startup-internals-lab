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

（T036 で学習者が書く）

## 振り返り

（T040 で書く）

## 後続 Feature の候補

（T040 で書く）

## 達成状況

（T039 で書く）
