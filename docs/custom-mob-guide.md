# Deepwither_V2 カスタムモブ制作ガイド

このドキュメントでは、Deepwither_V2 における「Java定義形式のカスタムモブ」の作り方と、Regionベーススポーンシステムの設定方法について解説します。

## 1. モブシステムの特徴

Deepwither_V2 のカスタムモブは、YAMLではなく **Javaクラスとして直接定義**します。
モブのHP、ドロップ、AIロジック、スキル、装備などすべてJava側で完結します。

* **Java定義**: `CustomMob` を継承したクラスを作るだけでモブを定義できます。
* **DI自動登録**: `@Component` を付けたモブ定義クラスは、DIコンテナが自動収集してコンストラクタを呼び出し、その中でマネージャーへの自己登録が行われます。
* **VirtualHealth連携**: HPは `VirtualHealthManager` によって管理されます。バニラのハートとは独立した仮想HPシステムです。
* **イベント自動配送**: `onAttack()` / `onDamaged()` などのフックが自動的に呼ばれます。
* **Regionベーススポーン**: `config.yml` で定義したRegion内に、設定した確率とtick間隔で自動スポーンします。

---

## 2. 新しいモブの作り方

新しいモブは、原則として `com.ruskserver.deepwither_V2.modules.mob.definitions` パッケージに作成します。

必要な作業は以下の2つです。

1. `CustomMob` を継承する
2. クラスに `@Component` を付け、コンストラクタで `CustomMobManager` を受け取って自己登録する

### 最小構成の例

```java
package com.ruskserver.deepwither_V2.modules.mob.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import org.bukkit.entity.EntityType;

@Component
public class GoblinMob extends CustomMob {

    @Inject
    public GoblinMob(CustomMobManager manager) {
        // モブID と エンティティタイプ を登録する
        // GoblinMob::new はスポーン時に新しいインスタンスを生成するファクトリ
        manager.registerMob("goblin", EntityType.ZOMBIE, GoblinMob::new);
    }

    @Override
    public void onSpawn() {
        // スポーン時に1度だけ呼ばれます
        setMaxHealth(150.0);
        setExp(30);
        entity.setCustomName(net.kyori.adventure.text.Component.text("ゴブリン"));
        entity.setCustomNameVisible(true);
    }

    @Override
    public void onDeath() {
        // 死亡時に呼ばれます。バニラドロップは自動でクリアされます
        dropIfPresent("goblin_coin", 0.7, getLocation());   // 70%の確率でドロップ
        dropIfPresent("goblin_ear",  0.3, getLocation());   // 30%の確率でドロップ
    }
}
```

> [!IMPORTANT]
> **`@Component` と `@Inject` の両方が必要です**
> `@Component` がないとDIコンテナに収集されず、モブが登録されません。
> 引数付きコンストラクタには `@Inject` を忘れずに付けてください。

---

## 3. フックメソッド一覧

`CustomMob` には以下のフックメソッドが用意されています。必要なものだけオーバーライドしてください。

| メソッド | 呼ばれるタイミング | 主な用途 |
|---|---|---|
| `onSpawn()` | スポーン直後（1回のみ） | HP設定、装備、名前表示 |
| `onTick()` | 毎tick（約20回/秒） | AIロジック、定期スキル |
| `onDeath()` | 死亡時 | ドロップ追加、EXP付与 |
| `onAttack(victim, event)` | このモブが攻撃を与えた時 | 追加効果（毒、ノックバックなど） |
| `onDamaged(attacker, event)` | このモブがダメージを受けた時 | 被弾リアクション、スキル発動 |

---

## 4. よく使うユーティリティ

### HP の設定と取得

```java
@Override
public void onSpawn() {
    setMaxHealth(300.0);  // 最大HPを300に設定し、現在HPも300でスタート
    setExp(120);          // プレイヤーが倒した時に付与するカスタムEXP
}

@Override
public void onTick() {
    if (getHealth() < getMaxHealth() * 0.3) {
        // HPが30%を切ったら何かする
    }
}
```

### EXP 報酬の設定

```java
@Override
public void onSpawn() {
    setMaxHealth(150.0);
    setExp(30);
}
```

`setExp()` に設定した値は、プレイヤーがそのカスタムモブへ最後にダメージを与えて倒した時に、
独自レベルシステムの `PlayerManager#addExp()` を通じて自動付与されます。
バニラの経験値オーブはドロップしません。

### 装備の設定

```java
@Override
public void onSpawn() {
    setMaxHealth(200.0);
    var equipment = entity.getEquipment();
    if (equipment != null) {
        // ItemManager連携でアイテムを装備（将来対応）
        // equipIfPresent("iron_chestplate", equipment::setChestplate);
        equipment.setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_HELMET));
        equipment.setHelmetDropChance(0.0f);  // ドロップしない
    }
}
```

### ドロップの追加

```java
@Override
public void onDeath() {
    Location loc = getLocation();
    // アイテムIDとドロップ確率を指定する
    dropIfPresent("rare_gem", 0.05, loc);   // 5%でドロップ
    dropIfPresent("mob_tooth", 1.0, loc);   // 100%でドロップ
}
```

### ticksLived を使った時間制御

```java
@Override
public void onTick() {
    // 5秒（100tick）ごとに実行
    if (ticksLived % 100 == 0) {
        entity.getWorld().strikeLightningEffect(getLocation());
    }
}
```

### 攻撃・被弾フックの例

```java
@Override
public void onAttack(LivingEntity victim, EntityDamageByEntityEvent event) {
    // 攻撃した相手に毒を付与する
    victim.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.POISON, 60, 0));
}

@Override
public void onDamaged(LivingEntity attacker, EntityDamageByEntityEvent event) {
    // HPが50%以下になったら逃走する（例）
    if (getHealth() < getMaxHealth() * 0.5) {
        entity.setVelocity(getLocation().subtract(attacker.getLocation())
                .toVector().normalize().multiply(1.5).setY(0.4));
    }
}
```

---

## 5. Regionベーススポーンの設定

スポーン設定はプラグインのデータフォルダ内の `config.yml` で管理します。
サーバー起動時にデフォルト設定が自動でコピーされます。

Region の座標範囲は **WorldGuard の ProtectedRegion** を参照するため、
先に WorldGuard 側でRegionを作成しておく必要があります。

### 手順

1. WorldGuard で Region を作成する
   ```
   /rg define forest_zone
   ```
2. `config.yml` の `worldguard-region` にその名前を指定する
3. サーバーを再起動（または `/reload` に対応したコマンドを実行）

### config.yml のフォーマット

```yaml
mob-regions:
  <region名（任意の識別名）>:
    safe-zone: <boolean>              # true: このRegionにはモブがスポーンせずPvPも無効
    pvp-enabled: <boolean>            # true: このRegion内のPvPを許可（デフォルト: false）
    world: <ワールド名>
    worldguard-region: <WG Region名> # /rg define で作成した WorldGuard Region の名前
    spawn-interval-ticks: <int>       # スポーンを試みるtick間隔 (20 = 1秒)
    max-mobs: <int>                   # Region内の同時最大モブ数
    spawn-table:
      - mob-id: <モブID>              # registerMob() に渡したIDと一致させる
        weight: <int>                 # 出現重み（合計に対する相対値）
```

### 設定例

```yaml
mob-regions:

  # セーフゾーン（スポーン禁止）
  # /rg define spawn_area で作成した WG Region と紐付ける
  spawn_area:
    safe-zone: true
    pvp-enabled: false
    world: world
    worldguard-region: spawn_area

  # 通常地域（10秒ごとにスポーン試行、最大30体）
  # /rg define forest_zone で作成した WG Region と紐付ける
  forest_zone:
    safe-zone: false
    pvp-enabled: false
    world: world
    worldguard-region: forest_zone
    spawn-interval-ticks: 200
    max-mobs: 30
    spawn-table:
      - mob-id: goblin         # 60/(60+30+10) = 60% の確率で選ばれる
        weight: 60
      - mob-id: forest_troll   # 30% の確率
        weight: 30
      - mob-id: dark_elf       # 10% の確率
        weight: 10
```


> [!NOTE]
> **Region PvPの既定値**
> セーフゾーンは常にPvP無効です。通常Regionも `pvp-enabled` を省略した場合はPvP無効として扱われ、
> PvPを許可したいRegionだけ `pvp-enabled: true` を設定します。
> V2の独自ダメージパイプライン（通常攻撃・弓・スキルなどの `DamagePipelineManager#processDamage()`）でも同じ判定が使われます。

> [!NOTE]
> **スポーンが行われない条件**
> * Region内のアクティブなカスタムモブ数が `max-mobs` に達している場合
> * `spawn-table` が空の場合
> * `spawn-interval-ticks` が0以下の場合
> * WorldGuard Region の Y 範囲外に地表があった場合（洞窟の天井など）

> [!WARNING]
> **`worldguard-region` に指定した名前の WG Region が存在しない場合**
> `start()` 時にログへ警告が出力され、そのRegionエントリはスキップされます。
> WG Regionを作成した後、再読み込みしてください。


---



## 6. モブIDの対応表を管理する

`registerMob()` に渡す **モブID** と **config.yml の mob-id** は必ず一致させてください。

```
Javaクラス (GoblinMob.java)
  └─ manager.registerMob("goblin", ...)
                            ↕ 一致させる
config.yml
  └─ spawn-table:
       - mob-id: goblin
```

> [!WARNING]
> **未登録IDがconfig.ymlに書かれていた場合**
> スポーン試行時にログへ警告が出力され、そのtickのスポーンはスキップされます。
> サーバーはクラッシュしません。

---

## 7. パッケージ構成

```
modules/mob/
  framework/
    CustomMob.java              ← 基底クラス（継承するだけ、編集不要）
    CustomMobManager.java       ← ライフサイクル管理（編集不要）
  region/
    MobRegion.java              ← Regionデータクラス（編集不要）
    MobRegionConfig.java        ← config.yml パーサー（編集不要）
    MobRegionSpawnService.java  ← スポーンスケジューラー（編集不要）
    SpawnEntry.java             ← スポーンテーブルエントリ（編集不要）
  definitions/                  ← ★ ここにモブ定義クラスを追加する
    GoblinMob.java
    ForestTrollMob.java
    ...
```

---

## 8. よくある注意点

> [!WARNING]
> **`@Component` を忘れるとモブが登録されません**
> DIコンテナはクラスパスを自動スキャンしますが、`@Component` がないクラスは収集されません。

> [!WARNING]
> **モブIDは必ず一意にしてください**
> `registerMob()` に同じIDを2回渡すと、後から登録したモブが上書きします。

> [!NOTE]
> **`onAttack` / `onDamaged` の `event` はすでにキャンセル済みです**
> `DamagePipelineManager` がバニラのダメージイベントをHIGHEST優先でキャンセルした後、
> MONITOR優先で `CustomMobManager` がフックを呼び出します。
> フック内でダメージを与えたい場合は `DamagePipelineManager#processDamage()` を使用してください。

> [!TIP]
> **依存サービスが必要なモブは `@Inject` を使ってください**
> ただし、`CustomMob` のインスタンスは `CustomMobManager` がスポーン時にファクトリ経由で生成するため、
> **DIコンテナが管理するシングルトンとして依存注入される訳ではありません**。
> 必要なサービスはコンストラクタで受け取り、フィールドに保持してください。
>
> ```java
> @Component
> public class MageMob extends CustomMob {
>
>     private final DamagePipelineManager damageManager;
>
>     @Inject
>     public MageMob(CustomMobManager mobManager, DamagePipelineManager damageManager) {
>         mobManager.registerMob("mage", EntityType.SKELETON, () -> new MageMob(mobManager, damageManager));
>         this.damageManager = damageManager;
>     }
>
>     @Override
>     public void onAttack(LivingEntity victim, EntityDamageByEntityEvent event) {
>         damageManager.processDamage(entity, victim, DamageType.MAGIC, 50.0, null);
>     }
> }
> ```
