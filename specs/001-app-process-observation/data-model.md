# Data Model: Androidアプリの起動とプロセスの観察

**Feature**: [spec.md](./spec.md) | **Date**: 2026-10-09

本 Feature にはデータベースも永続化もない。ここでいう「データ」は、
**ログ行**と、**実験記録（Markdown）に書く観測値**のことである。
spec の Key Entities を、記録するときのフィールドと検証ルールに落とし込む。

---

## 1. LifecycleEvent（ライフサイクルイベント）

アプリが Logcat に出力する観察の単位。書式は [contracts/log-format.md](./contracts/log-format.md) で定める。

| フィールド | 取得元 | 内容 |
|-----------|--------|------|
| timestamp | logcat `-v threadtime` | 日付と時刻（ミリ秒まで） |
| logcatPid | logcat `-v threadtime` の PID 列 | ログを書き込んだプロセスの PID |
| source | メッセージの `source=` | `Application` または `Activity` |
| event | メッセージの `event=` | 下の一覧のどれか |
| pid | メッセージの `pid=` | アプリが自分で取得した PID |
| instance | メッセージの `instance=` | オブジェクトを識別する値（16進） |

**event の値**

- source=Application: `onCreate`
- source=Activity: `onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onRestart`, `onDestroy`

**検証ルール**

- V-LE-1: `logcatPid == pid` でなければならない。一致しない行は本アプリのログとして扱わず、
  その事実を記録する（FR-005）。
- V-LE-2: 同じ時点で `pidof` から得た PID と一致しない行は、別のプロセス
  （前の試行の本アプリ、または他のアプリ）のログとして扱う（SC-003）。

## 2. ProcessObservation（プロセス観測値）

ADB で取得した、ある時点でのプロセスの状態。

| フィールド | 取得元 | 内容 |
|-----------|--------|------|
| step | 手順書 | どの手順の時点で取得したか（例: `after-launch`, `after-home+30s`） |
| exists | `pidof` の出力 | 出力があれば true、空なら false |
| pid | `pidof` | プロセスがなければ `—` |
| ppid | `ps -A -o PID,PPID,USER,NAME` | 親プロセスの PID |
| parentName | `ps` で PPID を引いた結果 | 親プロセスの名前（記録するだけ） |
| user | `ps` の USER 列 | アプリの UID 名（例: `u0_aNNN`） |

**検証ルール**

- V-PO-1: `pidof` が複数の PID を返した場合は、すべて記録する。想定外のケースとして
  扱い、原因の調査は後続 Feature の疑問にする（spec Edge Cases）。

## 3. OsProcessStartRecord（OSプロセス起動記録）

FR-017。OS が出力する、本アプリのプロセスを起動したことを示す記録。

| フィールド | 取得元 | 内容 |
|-----------|--------|------|
| startProcLine | system バッファ `ActivityManager` | `Start proc ...` 行の原文。出なければ「出力なし」 |
| amProcStart | events バッファ `am_proc_start` | イベント行の原文。出なければ「出力なし」 |
| pidInRecord | 上の2行から読み取る | 記録に含まれていた PID |
| matchesAppPid | 照合 | `pidInRecord` と ProcessObservation.pid が一致するか |
| launchState | `am start -W` の出力 | `LaunchState:` の値。出なければ「出力なし」（R5 の仮説） |

**検証ルール**

- V-OS-1: 原文は手を加えずに貼り付ける。解釈は「考察」の欄にだけ書く（FR-014, FR-017）。

## 4. AdbCommandRecord（ADBコマンド記録）

US2 の受け入れシナリオ4。`adb-commands.md` に、使ったコマンドごとに1件ずつ書く。

| フィールド | 内容 |
|-----------|------|
| command | 実行したコマンドを、そのまま |
| purpose | OS に何を頼んだか（学習者の言葉で） |
| options | 主なオプションの意味（例: `-W`, `-f 0x10200000`） |
| output | 代表的な出力 |
| effect | 端末で起きた変化 |
| source | 根拠にした資料（`adb help`、公式ドキュメント、AOSP）。AOSP を参照したときはタグ・ファイル・メソッド・ソースコードの URL も書く |

## 5. ExperimentEnvironment（実験環境）

FR-013。`environment.md` に1件だけ書き、全実験で共有する。

| フィールド | 取得コマンド |
|-----------|-------------|
| avdName | （AVD 名。例: `Pixel_5_API_36`） |
| androidRelease | `adb shell getprop ro.build.version.release` |
| sdkInt | `adb shell getprop ro.build.version.sdk` |
| fingerprint | `adb shell getprop ro.build.fingerprint` |
| buildType | `adb shell getprop ro.build.type` |
| model | `adb shell getprop ro.product.model` |
| abi | `adb shell getprop ro.product.cpu.abi` |
| alwaysFinishActivities | `adb shell settings get global always_finish_activities`（`0` でなければならない） |
| hostTools | `adb version`、JDK のバージョン、AGP と Gradle のバージョン |
| aospRef | ソースを読むときに参照した AOSP のタグ |

## 6. ExperimentRecord / Trial（実験記録と試行）

ExperimentRecord は実験1つ分。その中に Trial（試行1回分）が3つ以上ある。

**ExperimentRecord**: 実験ID（EXP-1〜3）、調査目的、仮説、実験環境
（`environment.md` へのリンク）、手順、Trial の一覧、考察、参考資料、未確認事項。
FR-012 の7項目は必ず含める。

**Trial**

| フィールド | 内容 |
|-----------|------|
| trialNo | 1, 2, 3, … |
| executedAt | 実行した日時 |
| observations | 手順ごとの ProcessObservation の一覧 |
| events | 本アプリの LifecycleEvent の列（V-LE-1/2 で絞り込んだもの） |
| osRecord | OsProcessStartRecord |
| deviation | 手順どおりにできなかったことや、他の試行との違い（SC-004） |

**検証ルール**

- V-ER-1: 「観測結果」「ソースコードで確認した事実」「考察」は別の見出しに分けて書く（Constitution II）。
- V-ER-2: 結論には「API 36 で確認。他のバージョンは未確認」と書き添える（FR-015）。

## 7. ComparisonSummary（比較表）

FR-016。`comparison.md` に書く。

| 列 | 内容 |
|----|------|
| 実験 | EXP-1 / EXP-2 / EXP-3 |
| 操作前の PID → 操作後の PID | 変わったかどうか |
| Application.onCreate | 出たか、出なかったか |
| Activity のイベント列 | 復帰／起動のときに出たイベントの順序 |
| Activity の instance | 前と同じか、違うか |
| OS のプロセス起動記録 | あったか、なかったか（＋ LaunchState） |
| 試行間の差 | なし、または内容 |

表の下に、Q1〜Q5 への回答（または「未確認」）と、その根拠になった実験と試行の番号を書く（SC-006）。

---

## 状態遷移（観測で確かめる仮説）

> ここに書いているのは AI が作った **参考仮説** で、未検証（[仮説]）。
> 実験記録には書き写さない。学習者が実験記録の「自分の仮説」を先に書き、
> そのあとでこの参考仮説と比べて、違いを「参考仮説との違い」に書く（Constitution II, VII）。

**プロセス**

```
[なし] --起動要求--> [あり(PID=A)] --HOME--> [あり(PID=A), バックグラウンド]
   ^                                             |
   |                                             +--起動要求--> [あり(PID=A), 前面]
   +----------------force-stop-------------------+
```

**Activity**（「アクティビティを保持しない」がオフのとき）

```
起動:   onCreate → onStart → onResume
HOME:   onPause → onStop
復帰:   onRestart → onStart → onResume   （instance は同じ）
停止:   force-stop のときは onDestroy が呼ばれないかもしれない（未確認）
```
