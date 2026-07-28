# Deepwither_V2 カスタムアイテム制作ガイド

このドキュメントでは、Deepwither_V2 における「Javaハードコーディング形式のカスタムアイテム」の作り方と、システムの仕様について解説します。

## 1. アイテムシステムの特徴

本プロジェクトのアイテムシステムは、YAMLではなく**Javaのクラスとして直接アイテムを定義**するアプローチを採用しています。これにより型安全で高度なロジックを組み込みやすくなっています。

*   **PDCによるモディファイア管理**: 生成時にランダム付与される追加ステータス（モディファイア）は、`PersistentDataContainer (PDC)` に保存されます。
*   **動的・共通Lore生成**: アイテムのLore（説明文）は、アイテムの「ベースステータス」「モディファイア」「フレーバーテキスト」を元にシステムが自動で共通レイアウトに構築します。バニラのツールチップは自動的に隠蔽され、斜体も解除されます。
*   **自動アップデート**: アイテムの設定（Javaコード上のステータス数値やテキスト）を書き換えてサーバーを再起動すると、プレイヤーが既に所持しているアイテムも**自動的に新しい設定に合わせてLoreが書き換わります**（PDC内のランダムモディファイア値は維持されます）。

---

## 2. 新しいアイテムの作り方

新しいアイテムを作るには、`com.ruskserver.deepwither_V2.modules.item.api.CustomItem` インターフェースを実装したクラスを作成するだけです。
**DIコンテナによって自動的に収集・登録されるため、管理クラス（ItemManagerなど）への手動登録は不要です。**

### サンプル: 「駆け出しの剣」の作り方

アイテム定義クラスは、原則として `com.ruskserver.deepwither_V2.modules.item.definitions` パッケージ（またはそのサブパッケージ）に作成します。

```java
package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

/**
 * 1. 必ず @Component アノテーションを付与してください（DIコンテナの自動収集対象になります）
 * 2. CustomItem インターフェースを実装します
 */
@Component
public class StarterSword implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public StarterSword() {
        this.baseStats = new EnumMap<>(StatType.class);
        // このアイテムの固定ステータス（ベース値）を設定します
        this.baseStats.put(StatType.ATTACK_DAMAGE, 20.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 5.0);
    }

    @Override
    public String getId() {
        // システム内で一意となるID（コマンドやPDCでの識別に使用）
        return "starter_sword";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§f駆け出しの剣";
    }

    @Override
    public ItemRarity getRarity() {
        // レアリティを指定します（COMMON, UNCOMMON, RARE, EPIC, LEGENDARY など）
        // レアリティに応じた色とタグがLoreの先頭に自動で付与されます
        return ItemRarity.COMMON;
    }

    @Override
    public String getFlavorText() {
        // アイテムの説明文です。
        // ここに入力した文字列は、ゲーム内表示時にシステム側で自動的に30文字程度で改行されます。
        return "初期装備として支給される標準的な剣です。よく手入れされており、それなりの切れ味を誇りますが、過酷な環境には耐えられないかもしれません。";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    // （オプション）リソースパック等を使用する場合、カスタムモデルデータを指定できます
    // @Override
    // public int getCustomModelData() {
    //     return 1001;
    // }
}
```

---

## 3. 生成されるアイテムのレイアウト

上記のコードで定義したアイテムは、`/givecustom starter_sword` コマンド等で生成された際、以下のようなフォーマットで表示されます。

```text
§f駆け出しの剣
◆ COMMON (← レアリティの色が付与されます)
初期装備として支給される標準的な剣です。よく手入れされ
ており、それなりの切れ味を誇りますが、過酷な環境には
耐えられないかもしれません。

-------- 装備ステータス --------
 攻撃力: 20.0 (+1.2)  (← カッコ内は生成時にランダムで付与されたモディファイア値)
 クリティカル率: 5.0
```

> [!TIP]
> **ランダムモディファイアについて**
> `ModifierManager` が共通ロジックとして、生成されるアイテムの `getBaseStats()` をもとにランダムなステータスボーナスを計算・付与します。このため、アイテム定義側でモディファイアの計算を書く必要はありません。
>
> `StatType.SPEED` はパーセント値で定義します。たとえば `5.0` は移動速度 `+5%`、`-10.0` は `-10%` として計算・表示されます。
