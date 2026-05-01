# Deepwither_V2 スキルツリー制作ガイド

このドキュメントでは、Deepwither_V2 における「Java定義形式のスキルツリー」の作り方と、スキル解放システムの基本仕様について解説します。

## 1. スキルツリーシステムの特徴

スキルツリーはYAMLではなく、Javaクラスとして定義します。
ノード配置、前提条件、競合、コスト、最大レベル、対応スキル、パッシブ効果までJavaで記述できます。

* **Java定義**: `SkillTreeDefinition` を実装したクラスを作成します。
* **DI自動登録**: `@Component` を付けると `SkillTreeRegistry` に自動登録されます。
* **PlayerData保存**: 習得済みノード、残りSP、カメラ位置は `PlayerSkillTreeProvider` でDB保存されます。
* **レベルアップ連携**: プレイヤーが1レベル上がるごとにスキルポイントを2獲得します。
* **スキル解放制御**: `/skills` の割り当てGUIには、スキルツリーで解放済みのスキルだけが表示されます。
* **発動側でも制御**: GUIだけでなく `SkillCastService` 側でも未解放スキルの発動を防ぎます。

---

## 2. 新しいスキルツリーの作り方

スキルツリー定義は、原則として `com.ruskserver.deepwither_V2.modules.skilltree.definitions` パッケージに作成します。

```java
package com.ruskserver.deepwither_V2.modules.skilltree.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeDefinition;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeNode;
import org.bukkit.Material;

import java.util.List;

@Component
public class WarriorSkillTree implements SkillTreeDefinition {

    @Override
    public String getId() {
        return "warrior";
    }

    @Override
    public String getDisplayName() {
        return "戦士";
    }

    @Override
    public Material getIcon() {
        return Material.IRON_SWORD;
    }

    @Override
    public List<SkillTreeNode> getNodes() {
        return List.of(
                SkillTreeNode.skill("slash_node", "slash")
                        .name("スラッシュ")
                        .description("前方を斬り払う基本スキル。")
                        .icon(Material.IRON_SWORD)
                        .position(4, 2)
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build()
        );
    }
}
```

> [!WARNING]
> **スキルノードの第2引数は Skill ID です**
> `SkillTreeNode.skill("slash_node", "slash")` の `"slash"` は、`Skill#getId()` と一致している必要があります。

---

## 3. ノードの種類

現在のノード種別は2つです。

* `SkillTreeNode.skill(...)`: スキル解放ノード
* `SkillTreeNode.passive(...)`: パッシブ効果ノード

スキルノードを習得すると、そのスキルが `/skills` GUIに表示され、スロットへ割り当て可能になります。

---

## 4. 前提条件・競合・レベル

ノードには前提条件、競合、最大レベル、コストを定義できます。

```java
SkillTreeNode.skill("fire_bolt_plus", "fire_bolt")
        .name("ファイアボルト強化")
        .position(6, 2)
        .requires("fire_bolt_node")
        .conflicts("ice_bolt_node")
        .maxLevel(3)
        .costPerLevel(1)
        .build();
```

ルールは以下です。

* `requires(...)` に指定されたノードは、最大レベルまで習得済みである必要があります。
* `conflicts(...)` に指定されたノードを習得済みの場合、そのノードは習得できません。
* `maxLevel` に達しているノードはそれ以上習得できません。
* 1回のクリックで `costPerLevel` 分のSPを消費し、ノードレベルが1上がります。

---

## 5. パッシブノードの作り方

パッシブノードは `SkillTreePassiveEffect` を定義します。
`apply()` で効果を付与し、`clear()` で効果を解除します。

```java
@Component
public class MageSkillTree implements SkillTreeDefinition {

    private static final String SOURCE_ID = "skilltree_arcane_focus";
    private final StatManager statManager;

    @Inject
    public MageSkillTree(StatManager statManager) {
        this.statManager = statManager;
    }

    @Override
    public List<SkillTreeNode> getNodes() {
        return List.of(
                SkillTreeNode.passive("arcane_focus")
                        .name("魔力集中")
                        .description("魔法攻撃力をレベルごとに5%上昇させる。")
                        .icon(Material.ENCHANTED_BOOK)
                        .position(6, 1)
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(
                                        player.getUniqueId(),
                                        StatType.MAGIC_DAMAGE,
                                        SOURCE_ID,
                                        level * 0.05,
                                        ModifierType.MULTIPLICATIVE
                                );
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, SOURCE_ID);
                            }
                        })
                        .build()
        );
    }
}
```

> [!NOTE]
> **clear() は必ず書くのがおすすめです**
> パッシブ再計算時には、まず全パッシブの `clear()` を呼び、その後に習得済みパッシブの `apply()` を呼び直します。
> これにより、レベル変化やリセット時に古い効果が残りにくくなります。

---

## 6. GUIとコマンド

ゲーム内では以下のコマンドでスキルツリーGUIを開けます。

```text
/skilltree
/stree
```

GUIは6行インベントリです。

* 上5行: ノード表示エリア
* 下1行: カメラ移動、位置リセット、SP表示
* 習得済み: 緑 + グロウ
* 習得可能: 黄
* 未解放: 赤
* 競合: バリア

ノード配置は `position(x, y)` で指定します。
カメラ位置はツリーごとにDB保存されます。

---

## 7. 実装時の注意点

> [!WARNING]
> **@Component を忘れると登録されません**
> `SkillTreeDefinition` 実装クラスにも、通常のモジュールと同じく `@Component` が必要です。

> [!WARNING]
> **ノードIDは全ツリーで一意にしてください**
> 現在の `SkillTreeRegistry` はノードIDをグローバルに管理します。
> 例: `mage_arcane_bolt`, `warrior_slash` のようにツリー名を含めると安全です。

> [!NOTE]
> **初期状態ではスキルは解放されていません**
> スキルを使えるようにするには、スキルツリーで該当スキルノードを習得する必要があります。
