# Tasks: Androidアプリの起動とプロセスの観察

**Input**: Design documents from `/specs/001-app-process-observation/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: 自動テストは作らない（spec で求められておらず、plan の R8 で作らないと決めた）。
各ストーリーの検証は、quickstart.md のシナリオ（S1〜S5）を実際に行って確かめる。

**Organization**: ユーザーストーリーごとにフェーズを分ける。

**学習者が自分で行うタスク**: 説明に `【学習者】` と付いたタスクは、AI が下書きしてもよい。
ただし最後は学習者（Shun）が自分の言葉で書くか、内容を確認して承認すること（Constitution I, VII／US2 の受け入れシナリオ4）。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 並行して進められる（別のファイルで、終わっていないタスクに依存しない）
- **[Story]**: どのユーザーストーリーのタスクか（US1〜US4）
- 実機での実験は1台のエミュレータを使うので、実験を実行するタスクには [P] を付けない

## Path Conventions

- プロジェクトルート = `android-startup-internals-lab/`（`.specify/` や `specs/` があるディレクトリ。git のルートと同じ）
- アプリのソース: `app/src/main/java/com/example/startuplab/`
- 実験記録: `docs/experiments/001-app-process-observation/`
- パッケージ名: `com.example.startuplab`。ログのタグ: `StartupLab`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 外部依存のない Gradle プロジェクトの骨組みを作る（research R2, R3）

- [X] T001 公式の互換表（Android Gradle Plugin のリリースノート、Kotlin と AGP の互換表）を見て、「compileSdk 36 と JDK 17 に対応する最新の安定版」の AGP・Gradle・Kotlin のバージョンを決める。決めたバージョンと参照した URL を、specs/001-app-process-observation/plan.md の「実装時に決めて追記すること」に書き足す
- [X] T002 settings.gradle.kts を作る。中身: `rootProject.name = "android-startup-internals-lab"`、`include(":app")`、pluginManagement のリポジトリ（google, mavenCentral, gradlePluginPortal）、dependencyResolutionManagement のリポジトリ（google, mavenCentral）。`repositoriesMode` は FAIL_ON_PROJECT_REPOS にする
- [X] T003 ルートの build.gradle.kts を作る。T001 で決めたバージョンで、`com.android.application` と Kotlin Android プラグインを `apply false` で宣言する
- [X] T004 [P] gradle.properties を作る。`org.gradle.jvmargs` と `kotlin.code.style=official` だけを書く。AndroidX は使わないので、`android.useAndroidX` は書かない（R2）
- [X] T005 [P] .gitignore を作る。`.gradle/`、`build/`、`app/build/`、`local.properties`、`.idea/`、`*.iml`、`.DS_Store` を除外する
- [X] T006 Gradle Wrapper を生成して、gradlew、gradlew.bat、gradle/wrapper/gradle-wrapper.jar、gradle/wrapper/gradle-wrapper.properties をリポジトリに含める。`distributionUrl` は T001 で決めたバージョンにする。Gradle 本体は入っていないので、一時的な方法（Android Studio に付属の Gradle、または公式配布物を一時的に使う）で生成する。どの方法で生成したかを plan.md に書き足す
- [X] T007 app/build.gradle.kts を作る。`namespace` と `applicationId` は `com.example.startuplab`。`compileSdk = 36`、`minSdk = 36`、`targetSdk = 36`、`versionCode = 1`、`versionName = "0.1"`。Kotlin の `jvmTarget` は 17。**`dependencies {}` は空にする**（AndroidX を含む外部ライブラリは入れない）

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: ログを出さない状態で、ビルド・インストール・起動ができる最小限のアプリと、全実験で共有する環境の記録を用意する

**⚠️ CRITICAL**: このフェーズが終わるまで、ユーザーストーリーのタスクには進まない

- [X] T008 [P] app/src/main/java/com/example/startuplab/LifecycleLog.kt を作る。トップレベル関数 `logLifecycle(source: String, event: String, instance: Any)` を定義し、`android.util.Log.i("StartupLab", ...)` で contracts/log-format.md の形式どおりに出力する。形式: `source=<Application|Activity> event=<イベント名> pid=<10進> instance=<16進>`（pid は `android.os.Process.myPid()`、instance は `Integer.toHexString(System.identityHashCode(instance))`）。抽象化はこの関数1つだけにする（plan の Constitution Check V の補足）
- [X] T009 [P] app/src/main/java/com/example/startuplab/StartupLabApplication.kt を作る。`android.app.Application` を継承して、`onCreate()` で `super.onCreate()` だけを呼ぶ（ログを出す処理は T014 で入れる）
- [X] T010 [P] app/src/main/java/com/example/startuplab/MainActivity.kt を作る。`android.app.Activity` を直接継承する（AppCompatActivity は使わない）。`onCreate` では `super.onCreate()` を呼んだあと、コードで作った `TextView`（文字列は "StartupLab"）を `setContentView` に渡すだけにする。レイアウト XML とリソースは作らない（FR-006）
- [X] T011 app/src/main/AndroidManifest.xml を作る。`<application android:name=".StartupLabApplication" android:label="StartupLab">` の中に、`.MainActivity`（`android:exported="true"`、`MAIN` と `LAUNCHER` の intent-filter）だけを置く。provider、service、receiver は置かない（起動の流れに余計なものを混ぜないため。R2）
- [X] T012 `./gradlew assembleDebug lint` が成功することを確かめる。続けて `./gradlew :app:dependencies --configuration debugRuntimeClasspath` を実行し、実行時の依存が Kotlin 標準ライブラリ（とそれが引き込むもの）だけで、AndroidX がないことを確かめる。結果を plan.md の「実装時に決めて追記すること」に書く
- [X] T013 エミュレータ `Pixel_5_API_36` を起動して、C-ENV-1〜3（contracts/adb-commands.md）を実行し、docs/experiments/001-app-process-observation/environment.md を作る。data-model.md §5 の全フィールド（avdName, androidRelease, sdkInt, fingerprint, buildType, model, abi, alwaysFinishActivities, hostTools, aospRef）を埋める。`always_finish_activities` は `0` でなければならない。aospRef は「（ソースを読んだら書く）」として空けておく

**Checkpoint**: アプリがビルド・インストールでき、タップで起動する。実験環境が記録されている

---

## Phase 3: User Story 1 - アプリ起動時のイベントを観察する (Priority: P1) 🎯 MVP

**Goal**: Application と Activity のライフサイクルイベントを、PID 付きで Logcat から観察できるようにする（FR-002〜FR-005）

**Independent Test**: quickstart.md の S1 を実行し、`source=Application event=onCreate` に続いて Activity の `onCreate → onStart → onResume` が出ること、すべての行で logcat の PID 列と `pid=` が一致すること（V-LE-1）を確かめる

### Implementation for User Story 1

- [X] T014 [P] [US1] app/src/main/java/com/example/startuplab/StartupLabApplication.kt の `onCreate()` で、`super.onCreate()` を呼んだ直後に `logLifecycle("Application", "onCreate", this)` を呼ぶ
- [X] T015 [P] [US1] app/src/main/java/com/example/startuplab/MainActivity.kt で、`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onRestart`, `onDestroy` の7つをオーバーライドする。それぞれ `super` を呼んだ直後に `logLifecycle("Activity", "<メソッド名>", this)` を呼ぶ（contracts/log-format.md の「出力するタイミング」に従う）
- [X] T016 [US1] ビルドしてインストールし（C-INS-1）、quickstart.md の S1 を実行する。logcat の PID 列と `pid=` が一致すること（data-model.md の V-LE-1: 「`logcatPid == pid` でなければならない」）を確かめる。一致しなければ T008 を直す
- [X] T017 [US1] アプリを前面に出したまま C-OP-2（HOME）を実行し、`adb logcat -d -v threadtime -s StartupLab:I` で Activity の `onPause` と `onStop` が出ることを確かめる（US1 の受け入れシナリオ3）
- [ ] T018 [US1] 【学習者】app/src/main/java/com/example/startuplab/ の3つのファイルについて、役割と、各コールバックが「いつ・誰から」呼ばれると今の時点で考えているかを説明できることを確かめる（Constitution I）。その予想は、後で exp*.md の仮説の欄に使う

**Checkpoint**: Logcat でライフサイクルを観察できる。US1 はここで単体で完了する（MVP）

---

## Phase 4: User Story 2 - ADBからアプリを操作する (Priority: P1)

**Goal**: ADB だけで起動・ホームへの移動・再表示・強制停止ができる。そのうえで、使ったコマンドの目的と結果を説明できる（FR-007〜FR-009、US2 の受け入れシナリオ4）

**Independent Test**: quickstart.md の S2 を、端末に触らずに最後まで行える。docs/experiments/001-app-process-observation/adb-commands.md に、使ったすべてのコマンドの目的・オプション・出力・端末の変化が書かれている

### Implementation for User Story 2

- [ ] T019 [US2] docs/experiments/001-app-process-observation/adb-commands.md を作る。contracts/adb-commands.md のコマンド ID（C-ENV-1〜4, C-INS-1〜2, C-OP-1〜3, C-PS-1〜2, C-LOG-1〜3）ごとに見出しを立て、data-model.md §4 の欄（command, purpose, options, output, effect, source）を空欄で用意する
- [ ] T020 [US2] C-OP-1（`am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n com.example.startuplab/.MainActivity`）を、プロセスがない状態とある状態でそれぞれ実行する。出力（`Status`、`LaunchState`、`Activity` など）と端末の変化を、adb-commands.md の C-OP-1 に原文のまま書く
- [ ] T021 [US2] research.md R5 の [仮説]「ランチャー（Launcher3）は ACTION_MAIN、CATEGORY_LAUNCHER、`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_RESET_TASK_IF_NEEDED` を付けた Intent で起動する」を、AOSP の Launcher3（Android 16 系のタグ）のソースで確かめる。参照したタグ・ファイル・メソッド・ソースコードの URL（タグを含む固定リンク。cs.android.com または android.googlesource.com）を adb-commands.md の C-OP-1 の source 欄に書き、research.md R5 の該当箇所を [AOSPで確認] か「誤り（正しくは〜）」に書き換える（Constitution III）
- [ ] T022 [US2] C-OP-2（HOME）と C-OP-3（force-stop）を実行し、出力と端末の変化を adb-commands.md に書く。C-OP-3 の後には C-PS-1 でプロセスが消えたことを確かめて、それも書く
- [ ] T023 [US2] C-ENV-1〜4、C-INS-1〜2、C-LOG-1〜3 を実行し、出力の例と端末の変化を adb-commands.md に書く。各オプション（`-d`, `-v threadtime`, `-b`, `-s`, `-r` など）の意味は、`adb help` と `adb logcat --help` を根拠にする
- [ ] T024 [US2] 【学習者】adb-commands.md のすべてのコマンドの purpose 欄（OS に何を頼んだか）を自分の言葉で書く。AI の下書きを使った場合は、その内容を確かめてから承認する（US2 の受け入れシナリオ4、Constitution VII）

**Checkpoint**: ADB だけで操作でき、各コマンドを説明できる

---

## Phase 5: User Story 3 - プロセスの状態を調査する (Priority: P2)

**Goal**: ADB でプロセスの有無・PID・親プロセスを確かめ、ログの PID と突き合わせられるようにする。OS 側のプロセス起動記録も取り出せるようにする（FR-010, FR-017）

**Independent Test**: quickstart.md の S3 と S4 を行い、同じ時点の `pidof` の値とログの `pid=` が一致すること、強制停止の後に `pidof` が空になることを確かめる。OS 側の記録が出るかどうかを確かめた結果が書かれている

### Implementation for User Story 3

- [ ] T025 [US3] quickstart.md の S3 を実行する。C-PS-1（`pidof`）と C-PS-2（`ps -A -o PID,PPID,USER,NAME`）の出力を adb-commands.md に書く。C-PS-2 では本アプリの行に加えて、その PPID に当たる行（親プロセスの名前）も原文で書く。親プロセスの名前は記録するだけで、Zygote の解析はしない
- [ ] T026 [US3] quickstart.md の S4 を実行し、research.md R6 の [仮説]（API 36 で `ActivityManager` の `Start proc` 行と、events バッファの `am_proc_start` が出る）と R5 の [仮説]（`am start -W` の出力に `LaunchState` が含まれる）を予備確認する。結果（出た行の原文に [環境で確認] を付けたもの、または「出力なし」）を research.md R5・R6 と plan.md の「実装時に決めて追記すること」に書く
- [ ] T027 [US3] AOSP（Android 16 系のタグ）で、`Start proc` と `am_proc_start` を出力している箇所（`frameworks/base/services/core/java/com/android/server/am/ProcessList.java` と `EventLogTags.logtags` だと予想している）を確かめる。タグ・ファイル・メソッド名・ソースコードの URL（タグを含む固定リンク。cs.android.com または android.googlesource.com）を、[AOSPで確認] を付けて research.md R6 に書き、environment.md の aospRef を埋める。読むのは出力している箇所だけにして、AMS 全体の解析はしない（spec の対象外）

**Checkpoint**: PID を突き合わせる方法と、OS 側の記録が出るかどうかが確かめられている

---

## Phase 6: User Story 4 - 起動条件による挙動を比較する (Priority: P2)

**Goal**: Experiment 1〜3 をそれぞれ3回行い、記録を比べて Q1〜Q5 に答える（FR-011〜FR-016, SC-004〜SC-007）

**Independent Test**: 3つの実験記録に、テンプレートの見出しがすべてあり、Trial が3つ以上ある。comparison.md に比較表と、Q1〜Q5 への回答（または「未確認」）とその根拠が書かれている（quickstart.md の S5）

### Implementation for User Story 4

> 実験を行う前に、仮説を書いて確定させておくこと（後から仮説を書き換えない。Constitution II）

- [ ] T028 [P] [US4] 【学習者】contracts/experiment-record-template.md をもとに docs/experiments/001-app-process-observation/exp1-first-launch.md を作り、「調査目的」「仮説」「実験手順」を書く。仮説は data-model.md の参考仮説を見る前に、自分の予想として書く（I2）。手順: 各 Trial で C-INS-2（uninstall）→ C-INS-1（install）→ C-ENV-3（`0` 以外なら C-ENV-4 を実行し、そのことを記録して、その試行をやり直す） → C-LOG-1 → C-PS-1（step=`before`、空のはず）→ C-OP-1 → C-PS-1/C-PS-2（step=`after-launch`）→ C-LOG-3（research R7）
- [ ] T029 [P] [US4] 【学習者】docs/experiments/001-app-process-observation/exp2-background-return.md を作り、「調査目的」「仮説」「実験手順」を書く。仮説は data-model.md の参考仮説を見る前に、自分の予想として書く（I2）。手順: 各 Trial で C-OP-3 → C-ENV-3（`0` 以外なら C-ENV-4 を実行し、そのことを記録して、その試行をやり直す） → C-OP-1 → C-PS-1（step=`after-launch`）→ C-LOG-1 → C-OP-2 → C-PS-1（step=`after-home`）→ **30秒待つ** → C-PS-1（step=`after-home+30s`）→ C-OP-1 → C-PS-1/C-PS-2（step=`after-return`）→ C-LOG-3（research R7）
- [ ] T030 [P] [US4] 【学習者】docs/experiments/001-app-process-observation/exp3-force-stop-relaunch.md を作り、「調査目的」「仮説」「実験手順」を書く。仮説は data-model.md の参考仮説を見る前に、自分の予想として書く（I2）。手順: 各 Trial で C-ENV-3（`0` 以外なら C-ENV-4 を実行し、そのことを記録して、その試行をやり直す） → C-OP-1 → C-PS-1（step=`after-launch`）→ C-LOG-1 → C-OP-3 → C-PS-1（step=`after-force-stop`、空のはず）→ C-OP-1 → C-PS-1/C-PS-2（step=`after-relaunch`）→ C-LOG-3（research R7）
- [ ] T031 [US4] EXP-1 を3回行い、exp1-first-launch.md の「観測結果」に Trial 1〜3 を書く。ログは contracts/log-format.md の照合ルールを通してから貼る。除いた行は「照合ルールで除いた行」に残す。OS の記録は原文のまま書く（data-model.md の V-OS-1: 「原文は手を加えずに貼り付ける。解釈は『考察』の欄にだけ書く」）。試行間で違いがあれば「試行間の差」に書く
- [ ] T032 [US4] EXP-2 を3回行い、exp2-background-return.md の「観測結果」に Trial 1〜3 を書く。復帰の前後で Activity の `instance` が同じかどうか、`Application onCreate` が出たかどうかを、事実として記録する。ホームにいる間にプロセスが消えていた場合は、spec の Edge Cases に従って「別のケース」として記録する
- [ ] T033 [US4] EXP-3 を3回行い、exp3-force-stop-relaunch.md の「観測結果」に Trial 1〜3 を書く。強制停止したときに `onDestroy` が出たかどうか、`am_proc_died` と `am_kill` が出たかどうかも、事実として記録する
- [ ] T034 [US4] 【学習者】3つの exp*.md の「考察」「仮説との照合」「確認できなかったこと・新しく出てきた疑問」「参考資料」を書く。結論には「API 36 で確認。他のバージョンは未確認」と書き添える（data-model.md の V-ER-2、FR-015）
- [ ] T035 [US4] docs/experiments/001-app-process-observation/comparison.md を作る。data-model.md §7 の列（実験、操作前の PID → 操作後の PID、Application.onCreate、Activity のイベント列、Activity の instance、OS のプロセス起動記録、試行間の差）で比較表を書く
- [ ] T036 [US4] 【学習者】comparison.md に、Q1〜Q5 それぞれへの回答または「未確認」を、根拠にした実験と Trial の番号つきで書く（SC-006）。「Activity の起動とプロセスの生成は同じではない」ことの是非を、PID の変化と Application.onCreate が出たかどうかを根拠に説明する（SC-007）

**Checkpoint**: 3つの実験が記録され、比べた結果から Q1〜Q5 に答えられている

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Feature 全体を確かめて振り返る（Constitution の Development Workflow）

- [ ] T037 quickstart.md の S1〜S5 を最初から最後まで通しで行い、すべての期待される結果を満たすことを確かめる。満たさないものがあれば、関係するタスクに戻って直す
- [ ] T038 [P] specs/001-app-process-observation/research.md と plan.md を見直して、[仮説] が残っていないことを確かめる。残っているものは、すべて [環境で確認]、[AOSPで確認]、「誤り（正しくは〜）」、「未確認（理由）」のどれかに書き換える
- [ ] T039 [P] spec の SC-001〜SC-007 が一つずつ満たされているかを確かめ、結果を docs/experiments/001-app-process-observation/comparison.md の末尾に「達成状況」として書く
- [ ] T040 【学習者】docs/experiments/001-app-process-observation/comparison.md に「振り返り」と「後続 Feature の候補」を書く。候補には、少なくとも spec と research で後回しにしたもの（起動要求の形による違い、`am kill` との比較、「アクティビティを保持しない」をオンにした場合、バージョン間の比較、`attachBaseContext` と ContentProvider の初期化順序、AMS/ATMS/Zygote の内部）と、実験で新しく出てきた疑問を含める（Constitution VI）

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: 依存なし。T001 → T002/T003 → T006 → T007 の順に進める（T004 と T005 は並行してよい）
- **Phase 2 Foundational**: Phase 1 が終わってから。T008〜T010 は並行してよい → T011 → T012 → T013
- **Phase 3 US1**: Phase 2 が終わってから
- **Phase 4 US2**: Phase 2 が終わってから。コード上は US1 に依存しない。ただし C-OP-1 の出力を見るときに US1 のログがあると読みやすいので、US1 の後に進めるのがおすすめ
- **Phase 5 US3**: US1 に依存する（ログの PID と照合するため）。US2 の adb-commands.md に書き足すので、US2 の後に進める
- **Phase 6 US4**: US1〜US3 のすべてに依存する（ログ、操作、プロセスの観測、OS の記録を組み合わせる）
- **Phase 7 Polish**: すべてのストーリーの後

### User Story Dependencies

```
Setup → Foundational ─┬─ US1 (P1, MVP) ─┐
                      └─ US2 (P1) ──────┴─ US3 (P2) ─ US4 (P2) ─ Polish
```

### Within Each User Story

- ソースを変えたら、ビルド・インストールしてから観測する
- 実験記録は「仮説を書く → 実験する → 観測結果を書く → 考察を書く」の順で進める。順番を入れ替えない
- 【学習者】のタスクは、その前にある AI のタスクの成果物を見てから行う

### Parallel Opportunities

- Phase 1: T004、T005
- Phase 2: T008、T009、T010（ファイルが別）
- Phase 3: T014、T015（ファイルが別）
- Phase 6: T028、T029、T030（実験記録のファイルが別で、実験を行う前の作業）
- Phase 7: T038、T039
- 実験を行うタスク（T031〜T033）は、エミュレータが1台なので並行しない

---

## Parallel Example: User Story 1

```text
# 別のファイルなので、同時に進められる:
Task: "T014 [US1] StartupLabApplication.kt の onCreate に logLifecycle を入れる"
Task: "T015 [US1] MainActivity.kt の7つのコールバックに logLifecycle を入れる"
# 両方が終わってから:
Task: "T016 [US1] ビルド・インストールして S1 で確かめる"
```

## Parallel Example: User Story 4

```text
# 実験を行う前の、記録の骨組みづくり（ファイルが別）:
Task: "T028 [US4] exp1-first-launch.md の目的・仮説・手順を書く"
Task: "T029 [US4] exp2-background-return.md の目的・仮説・手順を書く"
Task: "T030 [US4] exp3-force-stop-relaunch.md の目的・仮説・手順を書く"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1（Setup）→ Phase 2（Foundational）
2. Phase 3（US1）: Logcat でライフサイクルが見える
3. **ここで一度止めて**、quickstart.md の S1 で確かめる。ログの形式に問題があれば、実験を始める前に直す

### Incremental Delivery

1. Setup ＋ Foundational → アプリが起動する
2. ＋US1 → ログを観察できる（MVP）
3. ＋US2 → ADB で操作でき、コマンドを説明できる
4. ＋US3 → PID を照合でき、OS の記録を取り出せる
5. ＋US4 → 3つの実験と比較の結果 → Q1〜Q5 に答える
6. Polish → 振り返りと、後続 Feature の候補を出す

### Parallel Team Strategy

学習者1人と AI で進める想定。AI が下書きや実装をしたら、学習者が【学習者】のタスクで内容を確かめてから次のフェーズに進む。

---

## Notes

- 実験記録には、ログ全体ではなく、照合ルールを通した行だけを貼る（ログの永続化は対象外）
- 観測結果と考察を混ぜない。想定と違った結果も、そのまま記録する
- フェーズが終わるたびにコミットする
