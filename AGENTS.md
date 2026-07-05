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
* 攻撃スキルのダメージは、原則として攻撃力または魔法攻撃力に対する倍率で定義します。固定ダメージは特殊な効果や割合ダメージなど、倍率化できない場合に限定します。
* スキルレベルによってダメージ倍率を直接上げる実装は原則避けます。スキルレベルを導入する場合は、クールダウン、消費マナ、範囲、持続時間、命中数、追加効果などで成長を表現します。
* `Skill#getTags()` の文字列タグは、属性補正や戦闘処理に使う低レベルタグとして維持します。
* スキルの表示分類、検索、ビルド提案には `SkillTag` の型付きタグを使います。新規スキルには原則として `Role` を付け、攻撃スキルには `Scaling` も付けます。`Tactic` と `Constraint` は該当する場合のみ付けます。
* スキル説明文は原則2文構成にします。
  * 1文目: スキルの動作、発生位置、主要挙動を自然文で説明します。
  * 2文目: 対象範囲、対象、ダメージ種別、倍率/量、追加効果、持続時間を自然文で説明します。
* スキル説明文はスペックシート形式にせず、初心者が読んで効果を想像できる文章にします。ただし、MMOとして比較に必要な範囲、倍率、秒数、回復量、軽減量などは可能な限り明記します。
* 例: `前方に火球を放ち、着弾地点で爆発する。` / `周囲3mの敵に魔法ダメージ(120%)と炎上(4秒)を与える。`
* カスタムモブは `CustomMobManager#registerMob()` をコンストラクタ内で呼び自己登録します。スポーン設定は `config.yml` の `mob-regions` セクションで行います。

## パーティクル演出方針

Paper のパーティクルシステムでは、以下の2系統を使い分けて演出を構築します。

### 系統A: Particle.TRAIL（線・輪郭）

* `Particle.TRAIL` は spawn位置 → target位置 へ向かって飛ぶ「追尾する線」です。
* `extra`(speed) パラメータは効きません。代わりに `Particle.Trail(target, color, duration)` で色・持続時間を指定します。
* **ユーティリティ:**
  * `TrailCircleHelper` — 円・円弧を描く。衝撃波リング、チャージリングに使用。
  * `TrailHelper` — 直線(spawnLine/spawnSegmentedLine)、ビーム(spawnBeam)、正弦波(spawnWave)、螺旋(spawnSpiral)、扇(spawnCone)を描く。
* TIP: 衝撃波の輪っかや魔法陣など、**形が重要な静的表現**に向く。

### 系統B: ベクトルパーティクル（動きのある弾）

* `count=0` + offsetを方向ベクトル + `extra`を速度 にすると、パーティクルが指定方向へ飛んでいきます。
  ```java
  // 炎が (1, 0.5, 0) 方向へ speed=0.1 で飛ぶ
  world.spawnParticle(Particle.FLAME, loc, 0, 1.0, 0.5, 0.0, 0.1);
  ```
* この挙動は `/particle` コマンドの仕様に基づきます（count=0 で offset が方向ベクトルに変わる）。
* 速度が実際に反映されるパーティクル種別（FLAME, ELECTRIC_SPARK, CRIT, CLOUD, SONIC_BOOM, ENCHANT, GLOW 等）でのみ使用可能です。
* `Particle.TRAIL`/`Particle.DUST`/`Particle.DUST_COLOR_TRANSITION` など速度が効かない種別では使えません。
* TIP: 飛翔物の軌跡、爆風、火花の飛び散りなど、**動きが重要な動的表現**に向く。

### 複合テクニック

* TRAIL（系統A）で輪郭や形状を描き、系統Bのベクトルパーティクルを中に通すことで、立体感と動きを両立できます。
* 例: `TrailCircleHelper.spawnCircle()` で衝撃波リングを描き、その中心からベクトルパーティクルを放射する。

### 避けるべきパターン

* `count > 0` かつ `extra > 0` のパーティクルはランダム拡散し、意図した形になりません。輪郭や方向制御が必要な場面では上記2系統を使ってください。
* スカラーパーティクルのランダム散財は「空気感」として最小限に留め、主要な演出は TRAIL またはベクトルパーティクルで構築します。

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
