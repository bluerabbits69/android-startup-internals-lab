# Research: Androidアプリの起動とプロセスの観察

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-10-09

本書は Technical Context の未決事項を解消するための調査・判断の記録である。
Constitution II に従い、各判断の根拠を以下の4種類に区別して示す。

- **[環境で確認]**: 開発環境・実験で実際に観測した事実
- **[AOSPで確認]**: AOSP のソースコードで確認した事実（タグ・ファイル・メソッド・URL を併記）
- **[資料]**: 公式ドキュメント／AOSP に基づくが、まだ自分で確認していない（実験前に再確認する）
- **[仮説]**: 未検証。実験で確認し、結果を記録する対象

---

## R1. 実験環境（Androidバージョンと端末）

- **Decision**: 既存 AVD `Pixel_5_API_36`（`system-images/android-36/google_apis/arm64-v8a`、
  Play Store なし）を唯一の実験環境とする。対象は **API 36（Android 16）**。
- **Rationale**:
  - [環境で確認] 開発マシンに導入済みで、追加ダウンロードが不要。
  - [資料] `google_apis` イメージは `adb root` が使えるため、後続 Feature で
    `/proc` やシステム側の情報を深く観察するときに同じ環境を使い続けられる。
    Play Store 付きイメージは `user` ビルドで root が取れない。
  - [環境で確認] API 36 のイメージは導入済みの中で最も新しい。AOSP の最新ソースと
    比べやすい。
  - [仮説] ビルド種別（`ro.build.type`）は `userdebug` を想定する。実験前に
    `getprop` で確認し、記録する。
- **Alternatives considered**:
  - `Pixel_7a`（API 36、Play Store あり）: ビルド種別が `user` のため、後続 Feature で
    観察できる範囲が狭くなる。
  - `Pixel_8`（API 35）: 1世代古い。Clarifications でバージョンは1つに固定すると
    決めたので、比較用としても今回は使わない。
  - 実機: 対象外（spec の Assumptions）。
- **AOSP 参照方針**: 実験時に `ro.build.fingerprint` を記録し、ソースを読むときは
  Android 16 系のリリースタグ（`android-16.0.0_rN`）を参照する。どのタグを使ったかは
  その都度記録する（Constitution III）。

## R2. 実装言語と依存関係

- **Decision**: Kotlin で書く。**AndroidX を含む外部ライブラリは使わない**。
  Activity はフレームワークの `android.app.Activity` を直接継承する。
- **Rationale**:
  - [資料] AndroidX の一部（`androidx.startup` など）は ContentProvider を使って
    `Application.onCreate` より前に初期化処理を走らせる。そうすると、観察したい
    起動の流れに自分で書いていない処理が混ざってしまう。依存がゼロなら、
    ログに出てくるのは自分で書いたイベントだけになる（Constitution I, V）。
  - `AppCompatActivity` や Compose を使わなくても、ライフサイクルの観察には困らない。
    UI の装飾は対象外（FR-006）。
  - Kotlin を選ぶ理由: 開発者が普段使っている言語で、理解の負担が小さい。
    実行時に追加されるのは Kotlin 標準ライブラリだけで、起動時の初期化処理は持たない。
- **Alternatives considered**:
  - Java: 依存は完全にゼロになり、AOSP（Java）と同じ言語で読める。ただ、Kotlin 標準
    ライブラリが起動の流れに入り込まないので、学習効率を優先して Kotlin にした。
  - Jetpack Compose / AppCompat: AndroidX の初期化処理が入り、Constitution V に反する。

## R3. ビルド環境

- **Decision**: Gradle Wrapper ＋ Android Gradle Plugin（AGP）＋ Kotlin DSL で構成する。
  `compileSdk = 36`、`targetSdk = 36`、`minSdk = 36`。
  AGP・Gradle・Kotlin の具体的なバージョンは、実装時に公式の互換表で
  「compileSdk 36 と JDK 17 に対応する最新の安定版」を選び、plan.md に追記する。
- **Rationale**:
  - [環境で確認] JDK 17（`openjdk 17.0.16`）は導入済み。Gradle 本体は入っていない。
    → Wrapper を生成してリポジトリに含め、第三者が同じバージョンでビルドできる
    ようにする（Constitution IV）。
  - `minSdk = 36`: 検証するのは API 36 だけで、それ以外は未確認と明記する
    （Clarifications Q2）。実行できる環境を API 36 に限ることで、その方針を
    ビルド設定でも表す。バージョンを比べる Feature で見直す。
- **Alternatives considered**: Android Studio のテンプレートから作る方法。
  「Empty Views Activity」は AppCompat と Material を依存に含めるので、R2 の方針に
  合わない。手で最小構成を書く。

## R4. ログの設計（タグ・形式・PID照合）

- **Decision**:
  - 共通タグは `StartupLab`、ログレベルは INFO。
  - メッセージは `key=value` 形式で書く（詳細は [contracts/log-format.md](./contracts/log-format.md)）。
    `source`（Application / Activity）、`event`、`pid`（`Process.myPid()`）、
    `instance`（`System.identityHashCode(this)` の16進表記）を含める。
  - Application の初期化イベントは `Application.onCreate` だけにする。
  - Activity のイベントは spec で定めた7種類
    （onCreate, onStart, onResume, onPause, onStop, onRestart, onDestroy）。
- **Rationale**:
  - FR-004: イベント名・発生元・PID を1行で読み取れる。
  - FR-005 とレビュー指摘6: タグが一致するだけでは、他アプリのログを確実には
    除外できない。`logcat -v threadtime` の PID 列（logd が記録した、書き込んだ
    プロセスの PID）と、メッセージ内の `pid=`、`adb shell pidof` の3つを照合して、
    本アプリのログかどうかを判断する。
  - `instance` は spec に明記されていない追加項目。Experiment 2 で
    「Activity が作り直されたのか、同じインスタンスが再利用されたのか」を、
    onCreate が出たかどうかとは別の根拠で判断するために加えた（Q2, Q3）。
    1フィールドだけなので、Constitution V の範囲内と判断した。
  - `attachBaseContext` や ContentProvider の初期化順序はここでは扱わない。
    Application 内部の初期化順序は、後続 Feature の疑問として管理する。
- **Alternatives considered**:
  - `ActivityLifecycleCallbacks` で Application からまとめて記録する方法: Activity の
    メソッドを直接オーバーライドする方が、何が呼ばれているかが見たまま分かる。
  - Timber などのログライブラリ: 外部依存になるので使わない。

## R5. ADB操作（ランチャーと同等の起動要求）

- **Decision**: 起動と復帰には、どちらも次のコマンドを使う。

  ```
  adb shell am start -W -a android.intent.action.MAIN \
    -c android.intent.category.LAUNCHER -f 0x10200000 \
    -n com.example.startuplab/.MainActivity
  ```

  - `-f 0x10200000` = `FLAG_ACTIVITY_NEW_TASK (0x10000000)` |
    `FLAG_ACTIVITY_RESET_TASK_IF_NEEDED (0x00200000)`
  - ホームへの移動は `adb shell input keyevent KEYCODE_HOME`
  - 強制停止は `adb shell am force-stop com.example.startuplab`
  - プロセスの確認は `adb shell pidof com.example.startuplab` と
    `adb shell ps -A -o PID,PPID,USER,NAME`（本アプリの行を抜き出す）
- **Rationale**:
  - Clarifications Q4: ホーム画面のアイコンをタップしたときと同等の起動要求に揃える。
    [資料] ランチャー（Launcher3）は、アプリを起動するとき ACTION_MAIN、
    CATEGORY_LAUNCHER、コンポーネント名、上記2つのフラグを付けた Intent を送る。
    [仮説] 上のコマンドは、それと同じ Intent になる。実装時に Launcher3 のソースで
    フラグを確認し、参照したタグとファイルを記録する。
  - `-W` は起動の完了を待ち、結果を表示する。[仮説] Android 16 では出力に
    `LaunchState`（COLD / WARM / HOT）が含まれる。プロセスが新しく作られたかを示す、
    OS 側からのもう1つの証拠になる。起動時間（TotalTime など）は対象外なので、
    記録はするが評価しない。
  - `ps` の `PPID` 列を記録しておくと、親プロセス（zygote64 だと予想している）を
    観測事実として残せる。Zygote の内部解析は対象外なので、記録するだけにする。
  - 各コマンドの目的と結果を手順書に書くことで、US2 の受け入れシナリオ4
    （ADB の習得）を満たす。
- **Alternatives considered**:
  - `adb shell monkey -p <pkg> -c android.intent.category.LAUNCHER 1`: ランチャーと
    似た起動はできるが、どんな Intent を送ったのかが見えにくい。ADB の学習という
    目的に合わない。
  - `am start -n <pkg>/.MainActivity`（ACTION なし）: Clarifications Q4 で比較は
    後続 Feature に回したので、今回は使わない。

## R6. OS側のプロセス起動記録（FR-017）

- **Decision**: 2つのログを毎回記録する。
  1. system バッファの `ActivityManager` タグの `Start proc <pid>:<process>/<uid> for ...` 行
  2. events バッファの `am_proc_start` イベント
  参考として、`am_proc_died` と `am_kill` も記録する（強制停止したときの観察用）。
- **Rationale**:
  - [資料] どちらも `ActivityManagerService` 側から出力される。
    出力しているのは `frameworks/base/services/core/java/com/android/server/am/ProcessList.java`
    で、イベントタグの定義は同じディレクトリの `EventLogTags.logtags`。
    [仮説] Android 16 でも出力される。実験前の予備確認で実際に出ることを確かめる。
    出なかった場合は「出力されなかった」と記録し、推測で補わない（spec Edge Cases）。
  - 2つの経路で記録しておけば、片方が出なかったときにも判断の材料が残る。
  - ログを読むコマンド:
    `adb logcat -d -v threadtime -b main,system,events -s StartupLab:I ActivityManager:I am_proc_start:I am_proc_died:I am_kill:I`
    （`-d` を付けると、その時点までのログを出力して終了する。手順として再現しやすい）
- **Alternatives considered**: `dumpsys activity processes` の出力を読む方法。
  情報が多すぎて、記録するべき1行を決めにくい。後続 Feature で AMS を調べるときに使う。

## R7. 実験の開始状態とパラメータ

- **Decision**:
  - **Experiment 1（初回起動）**: 試行ごとに `adb uninstall` → `adb install` して、
    インストール直後で一度も起動していない状態から始める。
  - **Experiment 2（復帰）**: 起動して前面に出す → ホームへ移動 → **30秒待つ** →
    プロセスを確認 → 同じ起動コマンドで復帰。
  - **Experiment 3（強制停止後）**: 起動して前面に出す → `am force-stop` →
    プロセスがないことを確認 → 起動。
  - 各実験は3回ずつ試行する。各試行の前に `adb logcat -c` でログバッファを空にして、
    試行ごとのログの区切りにする。
  - 毎回、実験の前に `adb shell settings get global always_finish_activities` が
    `0` であることを確かめる（Clarifications Q3）。
- **Rationale**:
  - Experiment 1 の開始状態を強制停止で作ると、手順が Experiment 3 とまったく同じに
    なって、3つの実験を分ける意味がなくなる。再インストールなら、spec の名前どおり
    「初回起動」を毎回再現できる。
    [資料] インストール直後のアプリは「停止状態（stopped state）」にある。
    強制停止した後と、OS から見た状態が近いかもしれない。
    [仮説] それでも Experiment 1 と 3 で観測結果に違いが出るか、出ないかを記録する。
  - バックグラウンドで待つ時間を決めておかないと、試行ごとに条件がばらつく
    （clarify の Outstanding 項目を解消）。30秒は、操作の直後に起きる変化を
    見逃さない程度の長さとして選んだ。OS がプロセスを回収するかどうかを確かめる
    長時間の観察ではないので、それは後続 Feature で扱う。
  - `logcat -c` で区切れば、前の試行のログが混ざらない（spec Edge Cases）。
- **Alternatives considered**:
  - Experiment 1 の前に `am kill`（バックグラウンドのプロセスだけを終了する）:
    spec で比較を後続 Feature に回した「プロセスを終了するだけの操作」に当たるので、
    今回は使わない。

## R8. テストと検証の方法

- **Decision**: 自動テストは作らない。検証は次の2つで行う。
  1. `./gradlew assembleDebug` と `lint` が通ること。
  2. [quickstart.md](./quickstart.md) の手順を実際に行い、ログとプロセスを観測すること。
- **Rationale**: アプリにはロジックがなく、観察する対象は OS の挙動そのもの。
  単体テストで確かめられることはない。合格の基準は spec の SC-001〜SC-007 で、
  実験記録によって確認する（Constitution I, V）。
- **Alternatives considered**: Instrumentation テストで起動を自動化する方法。
  テストランナーが起動の流れに入り込んで観察を乱すし、Constitution VII が避けるべき
  とする「過度な自動化」にも当たる。

## R9. 実験記録の形式と配置

- **Decision**: Markdown で書き、リポジトリの
  `docs/experiments/001-app-process-observation/` に置く。
  - `environment.md`: 実験環境（1つのファイルを全実験で共有する）
  - `adb-commands.md`: 使ったコマンドの目的と結果（US2 の受け入れシナリオ4）
  - `exp1-first-launch.md` / `exp2-background-return.md` / `exp3-force-stop-relaunch.md`
  - `comparison.md`: 3つの実験の比較表と、Q1〜Q5 への回答
  書式は [contracts/experiment-record-template.md](./contracts/experiment-record-template.md)
  に従う。
- **Rationale**: Git で差分を追えて、第三者が読んで再現できる（Constitution IV）。
  ログの永続化は対象外なので、ログファイルは保存しない。必要な行だけを記録に
  貼り付ける。
- **Alternatives considered**: スクリプトでログをファイルに保存して自動で集計する方法。
  これは「ログの永続化」と「過度な自動化」に当たるので、今回はやらない。
