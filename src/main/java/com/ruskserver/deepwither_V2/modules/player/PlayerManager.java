package com.ruskserver.deepwither_V2.modules.player;

import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.stat.AttributeType;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.player.provider.PlayerAttributeProvider;
import com.ruskserver.deepwither_V2.modules.player.provider.PlayerLevelProvider;
import com.ruskserver.deepwither_V2.modules.stat.ModifierType;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

@Service
public class PlayerManager implements Listener {

    private final PlayerDataRepository repository;
    private final StatManager statManager;

    @Inject
    public PlayerManager(PlayerDataRepository repository, StatManager statManager) {
        this.repository = repository;
        this.statManager = statManager;
    }

    /**
     * 次のレベルに必要な経験値を計算します。
     */
    public int getExpToNextLevel(int currentLevel) {
        return currentLevel * 100; // シンプルな計算式
    }

    /**
     * 経験値を追加し、必要であればレベルアップ処理を行います。
     */
    public void addExp(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        repository.get(uuid).ifPresent(data -> {
            PlayerLevelProvider.LevelData levelData = data.get(PlayerLevelProvider.KEY);
            int currentLevel = levelData.getLevel();
            int currentExp = levelData.getExp();
            int newExp = currentExp + amount;

            boolean leveledUp = false;
            while (newExp >= getExpToNextLevel(currentLevel)) {
                newExp -= getExpToNextLevel(currentLevel);
                currentLevel++;
                leveledUp = true;
                
                // 1レベルにつき3ポイント付与
                PlayerAttributeProvider.AttributeData attrData = data.get(PlayerAttributeProvider.KEY);
                attrData.addRemainingPoints(3);
                data.markDirty(PlayerAttributeProvider.KEY);
            }

            levelData.setLevel(currentLevel);
            levelData.setExp(newExp);
            data.markDirty(PlayerLevelProvider.KEY);
            repository.save(uuid, data);

            if (leveledUp) {
                player.sendMessage(Component.text("レベルが " + currentLevel + " に上がりました！", NamedTextColor.GOLD));
                player.sendMessage(Component.text("属性ポイントを3ポイント獲得しました。", NamedTextColor.AQUA));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            } else {
                player.sendMessage(Component.text("経験値を " + amount + " 獲得しました。", NamedTextColor.GREEN));
            }
        });
    }

    /**
     * 属性ポイントを割り振ります。
     */
    public boolean addAttributePoint(Player player, AttributeType type) {
        UUID uuid = player.getUniqueId();
        var dataOpt = repository.get(uuid);
        if (dataOpt.isEmpty()) return false;

        var data = dataOpt.get();
        var attrData = data.get(PlayerAttributeProvider.KEY);

        if (attrData.getRemainingPoints() <= 0) {
            return false;
        }

        // 上限チェック (例えば最大100レベルまで)
        if (attrData.getAttribute(type) >= 100) {
            return false;
        }

        attrData.addRemainingPoints(-1);
        attrData.addAttribute(type, 1);
        data.markDirty(PlayerAttributeProvider.KEY);
        repository.save(uuid, data);

        // 割り振った瞬間、ステータスを再計算して反映させる
        recalculateStats(player);
        return true;
    }

    /**
     * 属性レベルに基づいて、StatManagerにモディファイア（バフ）を登録します。
     * このメソッドはログイン時やポイント割り振り時に呼ばれます。
     */
    public void recalculateStats(Player player) {
        UUID uuid = player.getUniqueId();
        repository.get(uuid).ifPresent(data -> {
            var attrData = data.get(PlayerAttributeProvider.KEY);

            // STR: 1pt -> 攻撃力 +1% (乗算)
            int str = attrData.getAttribute(AttributeType.STR);
            statManager.setModifier(uuid, StatType.ATTACK_DAMAGE, "attr_str", str * 0.01, ModifierType.MULTIPLICATIVE);

            // VIT: 1pt -> 最大HP +1%, 防御力 +0.5%
            int vit = attrData.getAttribute(AttributeType.VIT);
            statManager.setModifier(uuid, StatType.HEALTH, "attr_vit", vit * 0.01, ModifierType.MULTIPLICATIVE);
            statManager.setModifier(uuid, StatType.DEFENSE, "attr_vit", vit * 0.005, ModifierType.MULTIPLICATIVE);

            // MND: 1pt -> クリティカルダメージ +1.5%, 魔法ダメージ +1.5%
            int mnd = attrData.getAttribute(AttributeType.MND);
            statManager.setModifier(uuid, StatType.CRITICAL_DAMAGE, "attr_mnd", mnd * 0.015, ModifierType.MULTIPLICATIVE);
            statManager.setModifier(uuid, StatType.MAGIC_DAMAGE, "attr_mnd", mnd * 0.015, ModifierType.MULTIPLICATIVE);

            // INT: 1pt -> 最大マナ +2%
            int intVal = attrData.getAttribute(AttributeType.INT);
            statManager.setModifier(uuid, StatType.MAX_MANA, "attr_int", intVal * 0.02, ModifierType.MULTIPLICATIVE);
            // ※ CD短縮は必要になったら追加

            // AGI: 1pt -> クリティカル率 +0.2%
            int agi = attrData.getAttribute(AttributeType.AGI);
            statManager.setModifier(uuid, StatType.CRITICAL_CHANCE, "attr_agi", agi * 0.002, ModifierType.MULTIPLICATIVE);
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // ログイン時にDBからデータをロード（キャッシュに載せる）し、ステータスを反映
        recalculateStats(player);
    }
}
