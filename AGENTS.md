# AGENTS.md

Deepwither_V2 / Echoes of Aether Core Engine で作業するエージェント向けの基本ルールです。
詳細な実装方針は `docs/` 配下の各ガイドを参照してください。

## まず読むドキュメント

作業前に、変更内容に近いドキュメントを確認してください。

* 新規モジュール、DI、ライフサイクル: `docs/getting-started.md`
* DB、PlayerData、DirtyFlag、Repository: `docs/database-guide.md`
* Java定義カスタムアイテム: `docs/custom-item-guide.md`
* Java定義スキル、スキルキャスト、スキルイベント: `docs/skill-guide.md`
* Java定義スキルツリー、スキル解放、パッシブノード: `docs/skilltree-guide.md`
* Java定義カスタムモブ、Regionスポーン設定: `docs/custom-mob-guide.md`
* 小規模機能を1ファイルで作る場合: `docs/example-single-file-module.md`

## アーキテクチャ方針

* このプロジェクトは独自DIコンテナを使うモジュラーモノリスです。
* 新機能は原則 `com.ruskserver.deepwither_V2.modules.<feature>` 配下に配置します。
* DI対象クラスには `@Service`, `@Component`, `@Repository`, `@Command` のいずれかを付けます。
* 依存関係はコンストラクタインジェクションで受け取り、依存を持つコンストラクタには必ず `@Inject` を付けます。
* フィールドインジェクションは使いません。
* 起動処理が必要な場合は `Startable`、終了処理が必要な場合は `Stoppable` を実装します。
* 循環参照を作らないようにしてください。共通処理は別Serviceへ切り出します。

## Paper API 方針

* Paper API を使える箇所では、原則として Paper API を優先してください。
* 廃止予定API、つまり `@Deprecated` が付いたAPIは避けてください。
* Paper APIでは、非推奨表示の中に「実験的API」や「将来変更される可能性があるAPI」が多く含まれます。Paperの設計上やむを得ない場合は使用して構いません。
* コマンドは `paper-plugin.yml` に手書き追加せず、既存方針どおり `@Command` と Paper の `BasicCommand` を使ってJava側で定義します。
* Bukkit/Spigot互換APIよりも、Paperに専用APIがあり、かつ安全に使える場合はPaper側を選んでください。

## データ保存方針

* プレイヤーに紐づくデータは `PlayerDataProvider<T>` と `PlayerDataRepository` を使います。
* 値を `data.set(KEY, value)` で置き換えた場合はDirtyFlagが自動で立ちます。
* `data.get(KEY)` で取得したオブジェクトの内部状態を直接変更した場合は、必ず `data.markDirty(KEY)` を呼んでから保存してください。
* H2のUpsertは既存方針どおり `MERGE INTO ... KEY(...) VALUES ...` を使います。
* YAML依存の新規データ定義は避け、Java定義またはDB保存を優先します。

## アイテム・スキル定義

* カスタムアイテムは `CustomItem` 実装クラスとしてJavaで定義します。
* スキルは `Skill` 実装クラスとしてJavaで定義します。
* カスタムモブは `CustomMob` 継承クラスとしてJavaで定義します。
* アイテムやスキルの表示名、説明、アイコン、数値、ロジックはJavaクラス側に持たせます。
* `@Component` を付ければDIコンテナ経由で自動登録されるため、Managerへの手動登録は原則不要です。
* スキルは `cast()` が `CastResult.success()` を返した場合のみ、マナ消費とクールダウン付与が行われます。
* カスタムモブは `CustomMobManager#registerMob()` をコンストラクタ内で呼び自己登録します。スポーン設定は `config.yml` の `mob-regions` セクションで行います。

## コーディング方針

* 既存の設計、命名、パッケージ構成を優先してください。
* 変更範囲は依頼内容に必要な範囲へ絞ります。
* 不要なリファクタリングやメタデータ変更は避けます。
* コメントは、意図や設計判断を補う必要がある場所にだけ短く書いてください。
* 日本語ドキュメントはUTF-8で保存してください。

## Issue 管理方針

* 機能開発や重大なバグ修正は、必ず Issue として GitHub に登録してください。
* Issue には以下の情報を含めてください：
  * **概要**: 何を実装・修正するのか
  * **実装内容**: 具体的なタスクリスト
  * **技術仕様**: 関連するドキュメント・既存コード参照
* ロードマップ用 Issue は `milestone` パラメータで紐づけます。
  * 例: `Alpha 1.0` Milestone に紐づける場合、Issue 作成時に `milestone: 1` を指定
* MCP を通じて Issue を作成・更新する場合：
  * `mcp_github_issue_write` で `method: "create"` または `method: "update"` を使用
  * `owner: "RuskServer"`, `repo: "Deepwither_V2"` を指定
  * `labels` で機能カテゴリを明記（例: `["feature", "alpha-1.0", "skill"]`）
  * **新しい Issue を作成したら、親 Issue（ロードマップ）も同時に更新してください**
* Sub-issues 機能（GitHub Enterprise）は MCP では非対応のため、Milestone + Projects で進捗管理します。

## 確認

実装後は可能な限りビルド確認を行います。

```powershell
.\gradlew.bat build
```

Gradleがユーザー領域のキャッシュへアクセスする必要がある場合があります。
