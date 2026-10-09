<!--
Sync Impact Report
- Version change: (template, unversioned) → 1.0.0
- Modified principles: N/A（初版制定。テンプレートのプレースホルダー5原則を以下7原則で置換）
  - I. Understanding First（理解を最優先する）
  - II. Evidence-Based Investigation（根拠に基づく調査）
  - III. Source Code as Evidence（ソースコードを根拠にする）
  - IV. Reproducible Experiments（再現可能な実験）
  - V. Minimal Implementation（必要最小限の実装）
  - VI. Incremental Learning（段階的な学習）
  - VII. Explainable AI Development（説明可能なAI開発）
- Added sections: Project Vision, Development Workflow（SECTION_3 として）
- Removed sections: SECTION_2（独立セクションとしては使用せず、Project Vision を冒頭に配置）
- Templates requiring updates: なし（テンプレートは実行時に本ファイルを参照する）
- Follow-up TODOs: なし
-->

# Android Startup Internals Lab Constitution

## Project Vision

本プロジェクトは、Androidアプリの起動処理を題材として、
Android OSおよびAndroid Frameworkの内部構造を
体系的に理解するための学習・研究プロジェクトである。

最終的な目標は、Androidアプリが起動するまでの処理を、
実験結果とAOSPのソースコードを根拠に、
自分の言葉で説明できるようになることである。

アプリケーションの完成そのものではなく、
技術的な理解の深化をプロジェクトの成功基準とする。

## Core Principles

### I. Understanding First（理解を最優先する）

- 実装の完了よりも、仕組みの理解を優先しなければならない（MUST）。
- 実装したコードについて、役割と動作原理を説明できなければならない（MUST）。
- Android Frameworkの動作をブラックボックスとして扱わず、
  必要に応じて内部実装を調査する（SHOULD）。
- 学習目的に直接関係しない機能開発を行ってはならない（MUST NOT）。

Rationale:
本プロジェクトの目的はアプリケーション開発ではなく、
Android OSの内部構造を理解することである。

### II. Evidence-Based Investigation（根拠に基づく調査）

- 技術的な仮説は、可能な限り実験によって検証する（SHOULD）。
- Androidの内部動作に関する説明は、
  AOSPまたは公式ドキュメントを根拠としなければならない（MUST）。
- 観測した事実、ソースコードから確認した事実、
  推測・考察を明確に区別して記録しなければならない（MUST）。
- 根拠が不足している場合は、未検証であることを明示しなければならない（MUST）。
- 仮説が誤っていた場合は、その過程も学習成果として記録する（SHOULD）。

Rationale:
推測だけでAndroidの仕組みを理解したつもりになることを防ぐ。

### III. Source Code as Evidence（ソースコードを根拠にする）

- Android Frameworkの内部処理を調査する場合は、
  可能な限りAOSPの実装を参照する（SHOULD）。
- 調査対象のAndroidバージョンとAOSPのブランチ、
  またはコミットを明示しなければならない（MUST）。
- 調査結果には参照したクラス、メソッド、
  ソースコードのURLを記録しなければならない（MUST）。
- AOSPの実装と実機の挙動が必ずしも一致しないことを考慮する（SHOULD）。
- Androidのバージョンによる実装の違いを無視してはならない（MUST NOT）。

Rationale:
Androidの内部実装はバージョンによって変化するため、
検証可能な根拠を残す必要がある。

### IV. Reproducible Experiments（再現可能な実験）

- 実験は第三者が再現できる形で記録しなければならない（MUST）。
- 各実験では以下を明確にしなければならない（MUST）。
  - 調査目的
  - 仮説
  - 実験環境
  - 実験手順
  - 観測結果
  - 考察
  - 参考資料
- ADBやLogcatを活用して、可能な限り客観的なデータを取得する（SHOULD）。
- 実験環境にはAndroidバージョンや端末情報を記録しなければならない（MUST）。
- 実験によって確認できなかった内容も明示しなければならない（MUST）。

Rationale:
一度きりの動作確認ではなく、再現可能な知識を蓄積する。

### V. Minimal Implementation（必要最小限の実装）

- 学習目的を達成するための最小限の機能のみを実装する（MUST）。
- UIの装飾や不要な機能追加を行ってはならない（MUST NOT）。
- 外部ライブラリへの依存を必要最小限に抑える（SHOULD）。
- 抽象化や設計パターンは、必要性を説明できる場合にのみ導入する（MUST）。
- 新しい技術要素は、学習目的との関連性を確認してから採用する（MUST）。

Rationale:
アプリケーション自体の複雑さによって、
Android OSの学習が妨げられることを防ぐ。

### VI. Incremental Learning（段階的な学習）

- 一度にAndroidの起動処理全体を解明しようとしない（SHOULD NOT）。
- 各Featureには明確な学習目標を設定しなければならない（MUST）。
- Featureごとに検証可能な完了条件を定義しなければならない（MUST）。
- 前のFeatureで得た知識を次の調査に活用する（SHOULD）。
- 学習の進展によって新しい疑問が生まれた場合は、
  後続のFeatureとして管理する（SHOULD）。

Rationale:
複雑なAndroid Frameworkを段階的に理解することで、
知識の定着と継続的な学習を実現する。

### VII. Explainable AI Development（説明可能なAI開発）

- AIによるコード生成や調査支援を積極的に活用する（SHOULD）。
- AIが生成したコードについても、開発者が内容を理解しなければならない（MUST）。
- AIが提示したAndroid内部の動作説明は、
  必ずしも正しいとは限らないことを前提とする（MUST）。
- 技術的な説明は公式資料または実験結果によって検証しなければならない（MUST）。
- AIが提案した設計について、採用理由を説明できなければならない（MUST）。
- 学習機会を失うような過度な自動化を避ける（SHOULD）。

Rationale:
AIを学習の代替手段ではなく、
技術的理解を深めるための支援手段として活用する。

## Development Workflow

各Featureは原則として以下の流れで進める。

1. 学習目標と解明したい疑問を定義する。
2. 現時点での仮説を整理する。
3. Spec Kitを使って仕様と実装計画を作成する。
4. 必要最小限の実装を行う。
5. ADBやLogcatなどを使って実験する。
6. 必要に応じてAOSPのコードを調査する。
7. 実験結果と理解した内容を記録する。
8. 未解決の疑問を整理する。

各Featureの完了時には、学習目標に対する
達成状況を振り返らなければならない（MUST）。

## Governance

- 本Constitutionはプロジェクト全体の開発判断に優先する。
- Featureの仕様、計画、実装は本Constitutionに従わなければならない（MUST）。
  実装計画（plan）作成時のConstitution Checkで各原則への適合を確認する。
- 原則を変更する場合は、変更理由と影響範囲を記録しなければならない（MUST）。
- 原則の変更後は、既存の仕様や計画との整合性を確認しなければならない（MUST）。
- ConstitutionのバージョンはSemantic Versioningで管理する。
  - MAJOR: 原則の削除、または後方互換性のない再定義
  - MINOR: 原則・セクションの追加、または指針の実質的な拡張
  - PATCH: 文言の明確化、誤字修正など意味を変えない修正
- 学習目的の変化に応じて、Constitution自体の改善も認める。

**Version**: 1.0.0 | **Ratified**: 2026-10-09 | **Last Amended**: 2026-10-09
