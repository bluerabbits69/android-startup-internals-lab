# Contract: ライフサイクルログの形式

**対応要件**: FR-002, FR-003, FR-004, FR-005 | **関連**: [data-model.md §1](../data-model.md)

観察用アプリが Logcat に書き出すログの約束事。実験記録と照合の手順は、この形式を前提にする。

## 形式

- **タグ**: `StartupLab`（固定。他の用途には使わない）
- **レベル**: INFO
- **メッセージ**: 半角スペースで区切った `key=value` を、次の順に並べる

```
source=<Application|Activity> event=<イベント名> pid=<10進> instance=<16進>
```

| key | 値 | 取得方法 |
|-----|----|----------|
| source | `Application` / `Activity` | 書き出しているクラスで決まる |
| event | コールバック名（`onCreate` など） | 書き出しているメソッドで決まる |
| pid | 例 `12345` | `android.os.Process.myPid()` |
| instance | 例 `a1b2c3d` | `Integer.toHexString(System.identityHashCode(this))` |

## 出力するタイミング

| source | event | 出力する位置 |
|--------|-------|-------------|
| Application | onCreate | `Application.onCreate()` の中で、`super` を呼んだ直後 |
| Activity | onCreate / onStart / onResume / onRestart | 各メソッドで `super` を呼んだ直後 |
| Activity | onPause / onStop / onDestroy | 各メソッドで `super` を呼んだ直後 |

## `adb logcat -v threadtime` で見たときの例

```
10-09 21:30:01.234 12345 12345 I StartupLab: source=Application event=onCreate pid=12345 instance=5f3c2a1
10-09 21:30:01.301 12345 12345 I StartupLab: source=Activity event=onCreate pid=12345 instance=9e8d7c6
```

（数値は説明用の架空の値）

## 照合ルール

1. タグが `StartupLab` の行を候補にする。
2. 候補のうち、`threadtime` の PID 列（3列目）とメッセージの `pid=` が一致する行だけを残す。
3. 残った行の PID が、同じ手順の時点で `adb shell pidof com.example.startuplab` が返した PID と
   一致すれば、本アプリのその時点のログとして採用する。
4. 一致しない行は削除せずに、「別プロセスのログ」として記録に残す。

## 変更するとき

この形式を変えるときは、data-model.md とすべての実験記録テンプレートも合わせて直すこと。
