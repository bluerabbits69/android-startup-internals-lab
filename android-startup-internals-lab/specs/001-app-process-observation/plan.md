# Implementation Plan: Androidアプリの起動とプロセスの観察

**Branch**: `001-app-process-observation` | **Date**: 2026-10-09 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-app-process-observation/spec.md`

## Summary

外部依存のない最小限の Android アプリ（Application 1つ、Activity 1つ）を作り、
Application.onCreate と Activity の7つのライフサイクルイベントを、PID とインスタンス識別子
付きの決まった形式で Logcat に書き出す。そのうえで、ランチャーと同等の起動要求を送る
ADB コマンド、ホームへの移動、強制停止、`pidof`/`ps` によるプロセスの観測、OS 側の
プロセス起動記録（`Start proc` / `am_proc_start`）の取得を組み合わせて、
3つの実験（初回起動・バックグラウンドからの復帰・強制停止後の再起動）を、
3回ずつ再現できる手順と記録テンプレートとして整える。
環境は AVD `Pixel_5_API_36`（API 36、`google_apis`）に固定する。詳しくは [research.md](./research.md)。

## Technical Context

**Language/Version**: Kotlin（AGP に対応する安定版。実装時に決めて追記する）、JDK 17

**Primary Dependencies**: Android SDK（compileSdk 36）だけ。AndroidX と外部ライブラリは使わない（R2）。
ビルドは Gradle Wrapper ＋ AGP（Kotlin DSL）（R3）

**Storage**: N/A（ログは Logcat だけ。実験記録はリポジトリ内の Markdown）

**Testing**: 自動テストはなし。`assembleDebug` と `lint` が通ること、
[quickstart.md](./quickstart.md) の手順で観測できることで確かめる（R8）

**Target Platform**: Android 16（API 36）。AVD `Pixel_5_API_36`（arm64-v8a、`google_apis`）。
`minSdk = targetSdk = 36`

**Project Type**: mobile-app（観察用アプリ1つ）＋ 実験ドキュメント

**Performance Goals**: N/A（起動時間の計測と最適化は対象外）

**Constraints**: 起動の流れに、自分で書いていない処理（ContentProvider による初期化など）を混ぜない。
UI は最小限にする。実験の自動化スクリプトは作らない（Constitution VII）

**Scale/Scope**: ソースは Kotlin ファイル 3つ程度、Manifest 1つ。実験は3種類×3回。記録は6ファイル

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 原則 | ゲート | 判定（Phase 0 前） | 判定（Phase 1 後） |
|------|--------|-------------------|-------------------|
| I. Understanding First | 学習に関係のない機能がない／書いたコードを説明できる規模か | ✅ 機能はログ出力と最小限の表示だけ | ✅ ソースは3ファイル程度。どのクラスも役割を1行で説明できる |
| II. Evidence-Based | 事実・資料・推測を分けているか／根拠のない説明がないか | ✅ spec でそう決めている | ✅ research.md で [確認済]/[資料]/[仮説] を付けて区別。テンプレートで観測結果と考察を別の見出しにしている |
| III. Source Code as Evidence | バージョンと AOSP の参照先を書いているか | ✅ API 36 に固定 | ✅ fingerprint と AOSP タグを記録する（R1）。OS ログを出しているソースのパスを書いた（R6） |
| IV. Reproducible | 7項目と環境を記録するか／第三者が再現できるか | ✅ spec の FR-011〜013 | ✅ コマンドを ID 付きで固定（contracts/adb-commands.md）。Gradle Wrapper でビルドを固定。開始状態を作る手順を決めた（R7） |
| V. Minimal Implementation | 依存・抽象化・UI は最小限か | ✅ | ✅ 依存ゼロ。抽象化はログを出す関数1つだけ（形式を揃えるため。下を参照） |
| VI. Incremental | 学習目標と完了条件があるか／範囲を広げすぎていないか | ✅ | ✅ AMS や Zygote の解析、バージョン比較、起動方法の比較は後続 Feature に回した |
| VII. Explainable AI | AI が出した説明を検証する仕組みがあるか／自動化しすぎていないか | ✅ | ✅ AI が書いた仮説（ランチャーのフラグ、LaunchState、OS ログ）には [仮説] を付け、実験か AOSP で確かめることにした。実験は手でコマンドを打って行う |

**判定**: 違反はない。Complexity Tracking は空のまま。

**補足（V. 抽象化の必要性）**: ログを出す処理だけは、関数1つにまとめる。8か所で同じ形式
（contracts/log-format.md）を守る必要があり、1か所ずつ書くと形式がずれる危険があるため。
これ以外の抽象化（基底クラス、DI、設計パターン）は入れない。

## Project Structure

### Documentation (this feature)

```text
specs/001-app-process-observation/
├── spec.md
├── plan.md              # 本書
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/           # Phase 1
│   ├── log-format.md
│   ├── adb-commands.md
│   └── experiment-record-template.md
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2（/speckit-tasks で作る）
```

### Source Code (repository root)

```text
settings.gradle.kts
build.gradle.kts
gradle.properties
gradlew / gradlew.bat
gradle/wrapper/
app/
├── build.gradle.kts                      # minSdk/targetSdk/compileSdk = 36、dependencies は空
└── src/main/
    ├── AndroidManifest.xml               # Application と MainActivity（LAUNCHER）だけ
    └── java/com/example/startuplab/
        ├── StartupLabApplication.kt      # Application.onCreate のログ
        ├── MainActivity.kt               # 7つのライフサイクルのログ ＋ 最小限の表示
        └── LifecycleLog.kt               # log-format.md の形式でログを出す関数

docs/experiments/001-app-process-observation/
├── environment.md
├── adb-commands.md
├── exp1-first-launch.md
├── exp2-background-return.md
├── exp3-force-stop-relaunch.md
└── comparison.md
```

**Structure Decision**: Gradle プロジェクトはリポジトリのルートに置き、モジュールは `app` の1つだけにする。
後続 Feature で観察用のアプリが増えたら、モジュールを追加して対応する。
実験記録は、仕様（`specs/`）とは分けて `docs/experiments/<feature>/` に置く。
仕様は「何をするか」、記録は「何が観測されたか」で、目的が違うため。
パッケージ名 `com.example.startuplab` は仮の名前で、学習用なので公開は想定していない。

## Phase 0 / Phase 1 の成果物

- [research.md](./research.md): R1 環境、R2 言語と依存、R3 ビルド、R4 ログの設計、R5 ADB 操作、
  R6 OS 側の記録、R7 開始状態とパラメータ、R8 検証の方法、R9 記録の形式
- [data-model.md](./data-model.md): ログ行と観測値のフィールド、検証ルール、状態遷移の仮説
- [contracts/](./contracts/): ログの形式、使う ADB コマンドの一覧、実験記録のテンプレート
- [quickstart.md](./quickstart.md): ビルドから S1〜S5 の動作確認まで

## 実装時に決めて追記すること

- AGP・Gradle・Kotlin の具体的なバージョン（R3。公式の互換表で確認して追記する）
- `ro.build.type` と `ro.build.fingerprint` の実際の値（environment.md に書く）
- R5 と R6 の [仮説] を予備確認した結果（ランチャーのフラグ、LaunchState、`Start proc`、`am_proc_start`）

## Complexity Tracking

違反はないため記載なし。
