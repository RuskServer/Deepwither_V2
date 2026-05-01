# Deepwither_V2 スキル制作ガイド

このドキュメントでは、Deepwither_V2 における「Java定義形式のスキル」の作り方と、スキルキャストシステムの基本仕様について解説します。

## 1. スキルシステムの特徴

Deepwither_V2 のスキルは、YAMLではなく **Javaクラスとして直接定義**します。
スキル名、説明、アイコン、消費マナ、クールダウン、詠唱時間、実際の効果処理まで、すべてJava側で完結します。

* **Java定義**: `Skill` インターフェースを実装したクラスを作るだけでスキルを定義できます。
* **DI自動登録**: スキルクラスに `@Component` を付けると、`SkillRegistry` に自動登録されます。手動登録は不要です。
* **成功後消費**: `cast()` が `CastResult.success()` を返した場合のみ、マナ消費とクールダウン付与が行われます。
* **イベント駆動**: キャスト開始、実行、マナ消費、クールダウン付与、完了などのタイミングでイベントが発火します。
* **GUI割り当て対応**: `/skills` でスキル割り当てGUIを開き、ホットバースロットへスキルを設定できます。

---

## 2. 新しいスキルの作り方

新しいスキルは、原則として `com.ruskserver.deepwither_V2.modules.skill.definitions` パッケージに作成します。

必要な作業は以下の2つです。

1. `Skill` インターフェースを実装する
2. クラスに `@Component` を付ける

```java
package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.skill.api.CastResult;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.time.Duration;
import java.util.List;

@Component
public class ExampleSkill implements Skill {

    @Override
    public String getId() {
        return "example_skill";
    }

    @Override
    public String getDisplayName() {
        return "サンプルスキル";
    }

    @Override
    public List<String> getDescription() {
        return List.of("Javaだけで定義されたサンプルスキルです。");
    }

    @Override
    public Material getIcon() {
        return Material.NETHER_STAR;
    }

    @Override
    public double getManaCost(SkillContext context) {
        return 10.0;
    }

    @Override
    public Duration getCooldown(SkillContext context) {
        return Duration.ofSeconds(5);
    }

    @Override
    public Duration getCastTime(SkillContext context) {
        return Duration.ZERO;
    }

    @Override
    public CastResult cast(SkillContext context) {
        context.getCaster().playSound(context.getCaster().getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        context.getCaster().sendMessage("§aサンプルスキルを発動しました！");
        return CastResult.success();
    }
}
```

---

## 3. 重要なルール

> [!WARNING]
> **cast() 成功後にだけマナとクールダウンが消費されます**
> `SkillCastService` は、まずマナ不足やクールダウン中かどうかを確認します。
> その後 `skill.cast(context)` を呼び、戻り値が `CastResult.success()` の場合のみマナ消費とクールダウン付与を行います。
> `CastResult.fail()` を返した場合、マナもクールダウンも消費されません。

```java
@Override
public CastResult cast(SkillContext context) {
    if (!someCondition()) {
        return CastResult.fail();
    }

    // 効果処理
    return CastResult.success();
}
```

失敗理由をプレイヤーに表示したい場合は、メッセージ付きで返せます。

```java
return CastResult.fail(Component.text("対象が見つかりません。", NamedTextColor.RED));
```

---

## 4. SkillContext で取得できるもの

`SkillContext` は、スキル発動時に必要な情報をまとめたコンテキストです。

主に以下を取得できます。

* `getCaster()`: 発動したプレイヤー
* `getSkill()`: 発動中のスキル定義
* `getLevel()`: スキルレベル（現在は基本値として1）
* `getManaManager()`: マナ管理サービス
* `getCooldownService()`: クールダウン管理サービス
* `getEyeLocation()`: プレイヤーの視点位置
* `getDirection()`: プレイヤーの視線方向

消費マナやクールダウンは `SkillContext` を受け取れるため、将来的にスキルレベル、装備、ステータスに応じて動的に変化させられます。

```java
@Override
public double getManaCost(SkillContext context) {
    return 20.0 - context.getLevel();
}
```

---

## 5. Projectile型スキルの作り方

弾を飛ばすスキルは `SkillProjectile` と `SkillProjectileService` を使います。
`SkillProjectileService.launch(projectile)` が成功したら、`CastResult.success()` を返します。

```java
@Component
public class SimpleBoltSkill implements Skill {

    private final SkillProjectileService projectileService;

    @Inject
    public SimpleBoltSkill(SkillProjectileService projectileService) {
        this.projectileService = projectileService;
    }

    @Override
    public String getId() {
        return "simple_bolt";
    }

    @Override
    public String getDisplayName() {
        return "シンプルボルト";
    }

    @Override
    public Material getIcon() {
        return Material.AMETHYST_SHARD;
    }

    @Override
    public CastResult cast(SkillContext context) {
        SkillProjectile projectile = new SkillProjectile(
                context.getCaster(),
                context.getEyeLocation(),
                context.getDirection(),
                1.0,
                0.6,
                60
        ) {
            @Override
            protected void onTick() {
                getCurrentLocation().getWorld().spawnParticle(Particle.ENCHANT, getCurrentLocation(), 5);
            }

            @Override
            protected void onHitEntity(LivingEntity target) {
                target.damage(4.0);
                remove();
            }

            @Override
            protected void onHitBlock(Block block) {
                remove();
            }
        };

        return projectileService.launch(projectile) ? CastResult.success() : CastResult.fail();
    }
}
```

> [!TIP]
> **V2のダメージシステムに乗せたい場合**
> バニラの `target.damage()` ではなく、`DamagePipelineManager#processDamage(...)` をDIして呼ぶと、仮想HPや防御力などのV2戦闘システムに統合できます。

---

## 6. スキル割り当てと発動方法

ゲーム内では `/skills` または `/skill` でスキル割り当てGUIを開けます。

基本操作は以下です。

* 上段のスキル一覧からスキルを選択
* 下段のホットバースロットへ割り当て
* Fキーでスキルモードを切り替え
* スキルモード中にホットバーを選択すると、そのスロットのスキルを発動
* 詠唱中にFキーを押すとキャンセル

スキルスロットは `PlayerSkillSlotProvider` によってDBへ保存されます。
YAML保存は使いません。

---

## 7. イベントで拡張できるポイント

スキル発動フローでは以下のイベントが発火します。

* `SkillCastAttemptEvent`
* `SkillCastStartEvent`
* `SkillExecuteEvent`
* `SkillManaConsumeEvent`
* `SkillCooldownApplyEvent`
* `SkillCastCompleteEvent`
* `SkillCastCancelEvent`

Projectile系では以下のイベントがあります。

* `SkillProjectileLaunchEvent`
* `SkillProjectileTickEvent`
* `SkillProjectileExpireEvent`

例えば「特定装備中は火属性スキルのマナ消費を下げる」ような処理は、`SkillManaConsumeEvent` を監視して `setAmount()` で調整できます。

```java
@Component
public class SkillCostListener implements Listener {

    @EventHandler
    public void onManaConsume(SkillManaConsumeEvent event) {
        if (event.getSkill().getTags().contains("fire")) {
            event.setAmount(event.getAmount() * 0.9);
        }
    }
}
```

---

## 8. よくある注意点

> [!WARNING]
> **@Component を忘れるとスキルが登録されません**
> `Skill` を実装していても、`@Component` がないクラスはDIコンテナに収集されません。

> [!WARNING]
> **IDは必ず一意にしてください**
> `getId()` が重複すると、後から見つかったスキルは登録されません。

> [!NOTE]
> **依存サービスはコンストラクタで注入します**
> `SkillProjectileService` や `DamagePipelineManager` などが必要な場合は、フィールドに直接 `@Inject` するのではなく、コンストラクタに `@Inject` を付けて受け取ります。
