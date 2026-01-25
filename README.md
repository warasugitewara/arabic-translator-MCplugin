# ArabicTranslator | アラビア語翻訳プラグイン

[English](#english) | [日本語](#japanese)

---

<a id="english"></a>

## English

A Minecraft Paper 1.21.6 Arabic translation plugin that translates public server chat messages to Arabic in real-time.

### Features

- **Real-time Translation**: High-accuracy Arabic translation using DeepL API
- **Romanization**: Automatic generation and display of romanized pronunciation (Latin characters)
- **Server-wide Translation**: Translates all public chat messages from all players
- **Easy Toggle**: `/arabic enable` and `/arabic disable` commands
- **Public Chat Only**: Excludes private messages like `/tell`
- **Tab Completion**: Full tab completion support for commands

### Installation

1. Obtain a [DeepL API Key](https://www.deepl.com/pro#developer)
2. Place the compiled `ArabicTranslator-1.0.0.jar` in your Paper server's `plugins/` directory
3. Start the server to generate `plugins/ArabicTranslator/config.yml`
4. Edit the config file and set your DeepL API Key

### Configuration

`plugins/ArabicTranslator/config.yml`:

```yaml
# ArabicTranslator Configuration
# DeepL API Version: 'free' or 'pro'
deepl-api-version: 'free'

# Get your API Key from: https://www.deepl.com/your-account/keys
# IMPORTANT: Always wrap the API Key with single quotes to avoid YAML parsing errors
deepl-api-key: 'your-api-key-here:fx'

# Enable/disable translation on startup
translation-enabled: false
```

**Important:**
- Always wrap the API Key in **single quotes** (due to special characters like `:`)
- `deepl-api-version: 'free'` for Free tier
- `deepl-api-version: 'pro'` for Pro tier

### Usage

#### Commands

```
/arabic enable   - Enable translation server-wide
/arabic disable  - Disable translation server-wide
/arabic status   - Check current translation status
/arabic reload   - Reload configuration from config.yml
/arabic help     - Show help message
```

**Tab Completion:**
- `/ar [TAB]` → Shows available subcommands

#### Example Output

When a player types "Hello":

```
PlayerName: مرحبا | marhaba
```

- **Arabic**: مرحبا (purple, bold)
- **Romanization**: marhaba (yellow, italic)

### Requirements

- Minecraft Paper Server 1.21.6+
- Java 21+
- Internet connection (for DeepL API)
- DeepL API Key from [https://www.deepl.com/your-account/keys](https://www.deepl.com/your-account/keys)

### Permissions

```yaml
arabic.enable   - Use /arabic enable command
arabic.disable  - Use /arabic disable command
arabic.status   - Use /arabic status command (default: all)
arabic.reload   - Use /arabic reload command
arabic.help     - Use /arabic help command (default: all)
arabic.*        - All commands (default: OP only)
```

### License

Copyright (c) 2026 Warasugi. All rights reserved.

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<a id="japanese"></a>

## 日本語

Minecraft Paper 1.21.6 向けアラビア語翻訳プラグイン。サーバー内のパブリックチャットをリアルタイムでアラビア語に翻訳します。

### 機能

- **リアルタイム翻訳**: DeepL API による高精度なアラビア語翻訳
- **ローマ字表記**: アラビア語のローマ字読みを自動生成・表示
- **サーバー全体翻訳**: パブリックチャット内のすべてのプレイヤーのメッセージを翻訳
- **簡単ON/OFF**: `/arabic enable` と `/arabic disable` で翻訳の切り替え
- **パブリックチャットのみ**: `/tell` などのプライベートメッセージは対象外
- **Tab補完対応**: すべてのコマンドに対応

### インストール

1. [DeepL API Key](https://www.deepl.com/ja/pro#developer) を取得
2. コンパイル済みの `ArabicTranslator-1.0.0.jar` を Paper サーバーの `plugins/` ディレクトリに配置
3. サーバーを起動して `plugins/ArabicTranslator/config.yml` を生成
4. config ファイルを編集して DeepL API Key を設定

### 設定

`plugins/ArabicTranslator/config.yml`:

```yaml
# ArabicTranslator Configuration
# DeepL API Version: 'free' or 'pro'
deepl-api-version: 'free'

# Get your API Key from: https://www.deepl.com/ja/your-account/keys
# IMPORTANT: Always wrap the API Key with single quotes to avoid YAML parsing errors
deepl-api-key: 'your-api-key-here:fx'

# Enable/disable translation on startup
translation-enabled: false
```

**重要：**
- API Key は **必ずシングルクォートで囲む** （`:` などの特殊文字が含まれるため）
- `deepl-api-version: 'free'` - Free版を使用
- `deepl-api-version: 'pro'` - Pro版を使用

### 使用方法

#### コマンド

```
/arabic enable   - 翻訳を有効化（サーバー全体）
/arabic disable  - 翻訳を無効化（サーバー全体）
/arabic status   - 翻訳の現在状態を確認
/arabic reload   - config.yml を再読み込み
/arabic help     - ヘルプを表示
```

**Tab補完:**
- `/ar [TAB]` → enable / disable / status / reload / help を表示

#### 例

プレイヤーが「こんにちは」とチャットすると：

```
PlayerName: مرحبا | marhaba
```

- **アラビア語**: مرحبا （紫色・太字）
- **ローマ字表記**: marhaba （黄色・斜字）

### 必要環境

- Minecraft Paper Server 1.21.6 以上
- Java 21 以上
- インターネット接続（DeepL API 通信用）
- DeepL API Key（[https://www.deepl.com/ja/your-account/keys](https://www.deepl.com/ja/your-account/keys) から取得）

### パーミッション

```yaml
arabic.enable   - /arabic enable コマンドを使用
arabic.disable  - /arabic disable コマンドを使用
arabic.status   - /arabic status コマンドを使用（デフォルト: 全員）
arabic.reload   - /arabic reload コマンドを使用
arabic.help     - /arabic help コマンドを使用（デフォルト: 全員）
arabic.*        - 全コマンド使用可能（デフォルト: OP のみ）
```

### ライセンス

Copyright (c) 2026 Warasugi. すべての権利は Warasugi に帰属します。

このプロジェクトは MIT ライセンスの下で公開されています - 詳細は [LICENSE](LICENSE) ファイルを参照してください。
