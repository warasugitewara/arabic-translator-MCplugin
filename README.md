# ArabicTranslator | チャット翻訳プラグイン
<img width="385" height="164" alt="{4E81D746-D01A-48AD-AE77-AA872CD49092}" src="https://github.com/user-attachments/assets/51bb201f-844d-4b4f-83c4-f60b68c3a9e5" />

[English](#english) | [日本語](#japanese)

---

<a id="english"></a>

## English

A Paper plugin that translates public chat in real time and shows a pronunciation
guide next to each translation. Arabic and Chinese ship configured; any language
DeepL supports is a few lines of `config.yml`.

> **Upgrading from 1.x?** Your `config.yml` keeps working as it is, and the separate
> ChineseTranslator addon is no longer needed — Chinese is now a language in this
> plugin. See [Upgrading](#upgrading).

### Features

- **Real-time translation** of every public chat message
- **Several languages at once**, each with its own `/command`, on/off state and colours
- **Pronunciation guide**: Arabic transliteration and Chinese Pinyin
- **Provider failover**: DeepL first, then free backends when a quota runs out
- **Per-player display**: anyone can hide a language they do not need
- **Quota friendly**: cached, deduplicated, rate limited, and messages already in the
  target script are skipped
- **Brigadier commands** with real tab completion and per-node permissions

### Requirements

- Paper **1.21.x** (built and tested against 1.21.11) — or Paper **26.x**, see
  [Which jar](#which-jar)
- Java 21+
- A [DeepL API key](https://www.deepl.com/your-account/keys) — optional, a keyless
  free backend is enabled out of the box

### Installation

1. Drop `ArabicTranslator-2.0.0-mc1.21.jar` into your server's `plugins/` folder
2. Start the server once to generate `plugins/ArabicTranslator/config.yml`
3. Put your DeepL key in `deepl-api-key` (keep the single quotes)
4. `/arabic reload`, then `/arabic enable`

<a id="which-jar"></a>

### Which jar

Paper moved to CalVer after 1.21.11, and Paper 26.x ships Adventure 5 where 1.21.x
ships Adventure 4. The two are not binary compatible, so there are two jars:

| Your server | Jar |
| --- | --- |
| Paper 1.21.x | `ArabicTranslator-2.0.0-mc1.21.jar` |
| Paper 26.x | `ArabicTranslator-2.0.0-mc26.jar` |

### Languages

Each entry under `languages:` gets its own command and its own state:

```yaml
languages:
  arabic:
    enabled: true
    code: 'AR'
    aliases: [ 'ar' ]
    romanization: 'arabic'      # arabic | pinyin | none
    skip-script: 'ARABIC'
    format: '<white><player></white><gray>: </gray><light_purple><bold><translation></bold></light_purple><gray> | </gray><yellow><italic><romanization></italic></yellow>'

  chinese:
    enabled: false              # set to true to turn Chinese on
    code: 'ZH'
    aliases: [ 'ct', 'zh' ]
    romanization: 'pinyin'
    tone-style: 'marks'         # marks | numbers | none
    skip-script: 'HAN'
```

`format` is [MiniMessage](https://docs.advntr.dev/minimessage/); the placeholders are
`<player>`, `<message>`, `<translation>`, `<romanization>`, `<provider>` and
`<language>`. Chat text is inserted verbatim and can never be read back as a tag.

Adding a language is one block — `code: 'KO'`, `romanization: 'none'` — and a restart.

### Translation backends

Tried in the order listed under `providers.order`. A backend that is unconfigured,
rate limited or out of quota is skipped and the next one answers, so chat keeps
working.

| Provider | Key | Notes |
| --- | --- | --- |
| **DeepL** | required | Best quality, the default. Free keys end in `:fx`; the endpoint is chosen from the key, so there is nothing else to set. |
| **MyMemory** | none | Free fallback, ~5,000 chars/day (~50,000 with an e-mail address). Enabled by default. |
| **LibreTranslate** | optional | Open source and self-hostable: `docker run -p 5000:5000 libretranslate/libretranslate` gives unlimited free translation with no chat leaving your network. |

A backend that fails fatally — a rejected key, an exhausted quota — is paused for 15
minutes rather than retried on every message. A reload resumes it immediately.

### Commands

Every enabled language gets the same set, under its own name:

```
/arabic enable    Turn this language on for the whole server
/arabic disable   Turn this language off
/arabic status    Show every language, the providers and cache statistics
/arabic toggle    Show or hide these translations for yourself
/arabic reload    Re-read config.yml
/arabic help      Show the help
```

`/chinese …` (aliases `/ct`, `/zh`) does the same for Chinese, `/ar` for Arabic.

### Permissions

| Node | Default | Grants |
| --- | --- | --- |
| `translator.help` | everyone | The command and its help |
| `translator.status` | everyone | `status` |
| `translator.toggle` | everyone | `toggle` |
| `translator.enable` | op | `enable` |
| `translator.disable` | op | `disable` |
| `translator.reload` | op | `reload` |
| `translator.*` | op | Everything |

`arabic.*` and `chinese.*` still work — they are parents of `translator.*`, so
permission setups written for the 1.x plugins need no changes.

### Example

A player types "Hello":

```
Steve: مرحبا | marahba
Steve: 你好 | nǐ hǎo
```

The transliteration is a pronunciation aid, not a reversible transcription. Machine
translation output carries no Arabic short vowels, so no rule set can recover them;
`insert-short-vowels` breaks up consonant clusters with a default `a` to keep the
result pronounceable. Pinyin takes the first reading of a polyphonic character.

<a id="upgrading"></a>

### Upgrading from 1.x

- **Your config keeps working.** `deepl-api-key`, `deepl-api-version` and
  `translation-enabled` are still read from the top level. A config with no
  `languages:` section loads Arabic from those keys alone.
- **Delete ChineseTranslator.jar.** Chinese is a language here now: set
  `languages.chinese.enabled: true`.
- **Permissions are unchanged in practice** — `arabic.*` and `chinese.*` still grant
  everything.
- **`/arabic enable` no longer rewrites config.yml.** The on/off state lives in
  `state.yml`, so your comments survive.

### Building from source

```bash
./gradlew build                    # Paper 1.21.x (default)
./gradlew build -Ptarget=paper26   # Paper 26.x
```

Jars land in `build/libs/`. Requires JDK 21; Gradle comes from the wrapper.

### License

Copyright (c) 2026 Warasugi. Licensed under the MIT License — see [LICENSE](LICENSE).

Bundles [pinyin4j](https://github.com/belerweb/pinyin4j) (BSD) for Hanzi readings.

---

<a id="japanese"></a>

## 日本語

パブリックチャットをリアルタイムで翻訳し、読み方を併記する Paper プラグインです。
アラビア語と中国語は設定済みで同梱、DeepL が対応する言語なら `config.yml` に数行
追加するだけで増やせます。

> **1.x からの更新** `config.yml` はそのまま使えます。ChineseTranslator アドオンは
> 不要になりました（中国語はこのプラグインの一言語です）。
> [1.x からの更新](#upgrading-ja) を参照してください。

### 機能

- パブリックチャットの**リアルタイム翻訳**
- **複数言語の同時運用**。言語ごとに `/コマンド`・ON/OFF・配色を個別に設定
- **読み方の併記**: アラビア語のローマ字表記と中国語のピンイン
- **翻訳バックエンドの自動切替**: DeepL を優先し、上限に達したら無料バックエンドへ
- **プレイヤーごとの表示切替**: 不要な言語は各自で非表示にできる
- **API 節約**: キャッシュ・重複リクエストの統合・レート制限、翻訳先の文字種で
  書かれた発言はスキップ
- **Brigadier コマンド**: 本物の Tab 補完とノード単位の権限

### 動作環境

- Paper **1.21.x**（1.21.11 でビルド・テスト）または Paper **26.x**
  （[どちらの jar か](#which-jar-ja) 参照）
- Java 21 以上
- [DeepL API Key](https://www.deepl.com/ja/your-account/keys)（任意。キー不要の無料
  バックエンドが最初から有効です）

### インストール

1. `ArabicTranslator-2.0.0-mc1.21.jar` をサーバーの `plugins/` に配置
2. サーバーを一度起動して `plugins/ArabicTranslator/config.yml` を生成
3. `deepl-api-key` に DeepL の API Key を設定（**シングルクォートは外さない**）
4. `/arabic reload` のあと `/arabic enable`

<a id="which-jar-ja"></a>

### どちらの jar か

Paper は 1.21.11 のあと CalVer に移行し、26.x は Adventure 5、1.21.x は Adventure 4
を同梱しています。両者はバイナリ互換がないため、jar を分けています。

| サーバー | jar |
| --- | --- |
| Paper 1.21.x | `ArabicTranslator-2.0.0-mc1.21.jar` |
| Paper 26.x | `ArabicTranslator-2.0.0-mc26.jar` |

### 言語設定

`languages:` の各項目が、それぞれ専用のコマンドと状態を持ちます。

```yaml
languages:
  arabic:
    enabled: true
    code: 'AR'
    aliases: [ 'ar' ]
    romanization: 'arabic'      # arabic | pinyin | none
    skip-script: 'ARABIC'
    format: '<white><player></white><gray>: </gray><light_purple><bold><translation></bold></light_purple><gray> | </gray><yellow><italic><romanization></italic></yellow>'

  chinese:
    enabled: false              # true にすると中国語が有効になります
    code: 'ZH'
    aliases: [ 'ct', 'zh' ]
    romanization: 'pinyin'
    tone-style: 'marks'         # marks | numbers | none
    skip-script: 'HAN'
```

`format` は [MiniMessage](https://docs.advntr.dev/minimessage/) 形式で、使える
プレースホルダは `<player>` `<message>` `<translation>` `<romanization>`
`<provider>` `<language>` です。チャット本文はそのまま挿入され、タグとして
解釈されることはありません。

言語追加はブロックを 1 つ足して再起動するだけです（例: `code: 'KO'`,
`romanization: 'none'`）。

### 翻訳バックエンド

`providers.order` の順に試します。未設定・レート制限・上限到達のバックエンドは
飛ばして次に回るので、チャットが止まりません。

| プロバイダ | キー | 備考 |
| --- | --- | --- |
| **DeepL** | 必要 | 品質最優先の既定。Free キーは `:fx` で終わるので、接続先はキーから自動判定します |
| **MyMemory** | 不要 | 無料のフォールバック。1 日 5,000 文字程度（メールアドレス登録で約 50,000 文字）。既定で有効 |
| **LibreTranslate** | 任意 | オープンソースで自前ホスト可。`docker run -p 5000:5000 libretranslate/libretranslate` で無制限・無料、チャットが外部に出ません |

キー拒否や上限到達など回復しない失敗をしたバックエンドは、毎回再試行せず 15 分
休止します。リロードで即座に復帰します。

### コマンド

有効な言語ごとに、同じサブコマンドが用意されます。

```
/arabic enable    サーバー全体でこの言語を有効化
/arabic disable   無効化
/arabic status    全言語・プロバイダ・キャッシュ統計を表示
/arabic toggle    自分だけ表示 / 非表示を切り替え
/arabic reload    config.yml を再読み込み
/arabic help      ヘルプを表示
```

中国語は `/chinese …`（別名 `/ct`, `/zh`）、アラビア語は `/ar` でも使えます。

### パーミッション

| ノード | 既定 | 対象 |
| --- | --- | --- |
| `translator.help` | 全員 | コマンドとヘルプ |
| `translator.status` | 全員 | `status` |
| `translator.toggle` | 全員 | `toggle` |
| `translator.enable` | OP | `enable` |
| `translator.disable` | OP | `disable` |
| `translator.reload` | OP | `reload` |
| `translator.*` | OP | すべて |

`arabic.*` と `chinese.*` は `translator.*` の親として残してあるので、1.x 向けに
組んだ権限設定はそのまま動きます。

### 表示例

プレイヤーが「こんにちは」と発言した場合:

```
Steve: مرحبا | marahba
Steve: 你好 | nǐ hǎo
```

読み方はあくまで発音の目安で、元の綴りに戻せる転写ではありません。機械翻訳の
出力には短母音が付かないため、どんな規則でも復元はできません。
`insert-short-vowels` は子音の連続に既定の `a` を挿入して読める形にします。
ピンインは多音字の第一候補を採用します。

<a id="upgrading-ja"></a>

### 1.x からの更新

- **設定はそのまま使えます。** `deepl-api-key` / `deepl-api-version` /
  `translation-enabled` は引き続きトップレベルから読み込みます。`languages:` が
  無い設定ファイルでも、その情報だけでアラビア語が有効になります。
- **ChineseTranslator.jar は削除してください。** 中国語はこのプラグインの一言語です。
  `languages.chinese.enabled: true` にしてください。
- **権限は実質そのまま** — `arabic.*` / `chinese.*` は今までどおり全権限を含みます。
- **`/arabic enable` が config.yml を書き換えなくなりました。** ON/OFF 状態は
  `state.yml` に保存されるので、コメントが消えません。

### ソースからのビルド

```bash
./gradlew build                    # Paper 1.21.x（既定）
./gradlew build -Ptarget=paper26   # Paper 26.x
```

jar は `build/libs/` に出力されます。JDK 21 が必要で、Gradle は wrapper が入手します。

### ライセンス

Copyright (c) 2026 Warasugi. MIT ライセンスで公開しています — 詳細は
[LICENSE](LICENSE) を参照してください。

漢字の読み取得に [pinyin4j](https://github.com/belerweb/pinyin4j)（BSD）を同梱しています。
