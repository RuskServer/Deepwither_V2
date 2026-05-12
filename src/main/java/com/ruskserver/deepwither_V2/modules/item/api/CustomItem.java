package com.ruskserver.deepwither_V2.modules.item.api;

import com.ruskserver.deepwither_V2.core.stat.StatType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * カスタムアイテムの基底となるインターフェース。
 * ハードコーディングでアイテムを定義する際に実装します。
 */
public interface CustomItem {

    /**
     * @return アイテムの一意のID（例: "starter_sword"）
     */
    String getId();

    /**
     * @return アイテムのベースとなるMaterial
     */
    Material getMaterial();

    /**
     * @return アイテムの表示名
     */
    String getDisplayName();

    /**
     * @return アイテムの固定ステータス（ベースステータス）
     */
    Map<StatType, Double> getBaseStats();

    /**
     * @return アイテムのレアリティ
     */
    ItemRarity getRarity();

    /**
     * @return アイテム固有のフレーバーテキスト。文字列は自動で30文字改行されます。
     */
    String getFlavorText();

    /**
     * @return アイテムの武器種（例: "剣", "杖", "斧"など）。nullの場合は表示されません。
     */
    default String getWeaponType() {
        return null;
    }

    /**
     * @return アイテムの基本売却価格。0以下の場合は売却不可とみなします。
     */
    default double getSellPrice() {
        return 0.0;
    }

    /**
     * カスタムモデルデータ番号を取得します。必要に応じてオーバーライドしてください。
     * @return CustomModelData (設定しない場合は 0 を返す)
     */
    default int getCustomModelData() {
        return 0;
    }

    /**
     * 手に持って右クリック・左クリック等をした際に呼ばれるフックメソッド。
     */
    default void onInteract(org.bukkit.event.player.PlayerInteractEvent event) {}

    /**
     * 自分が攻撃者になった際、ダメージ計算パイプラインの途中で呼び出されます。
     * このメソッド内で context.multiplyDamage() 等を呼ぶことで、特定の属性のダメージを上げたりできます。
     */
    default void onAttack(com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext context) {}

    /**
     * 自分が防御者（ダメージを受ける側）になった際、ダメージ計算パイプラインの途中で呼び出されます。
     * このメソッド内で context.setDamage(0) などを呼ぶことで、攻撃をブロックするパッシブなどが作れます。
     */
    default void onDefend(com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext context) {}
}
