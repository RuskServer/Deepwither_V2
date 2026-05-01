package com.ruskserver.deepwither_V2.modules.player;

import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.stat.AttributeType;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.player.provider.PlayerAttributeProvider;
import com.ruskserver.deepwither_V2.modules.player.provider.PlayerLevelProvider;
import com.ruskserver.deepwither_V2.modules.skilltree.service.SkillTreeService;
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

    /** Lv1〜100 の次のレベルに必要な累積経験値テーブル（インデックス0 = Lv1 -> Lv2に必要な量） */
    private static final int[] EXP_TABLE = {
            // Lv1 - 10 (雑魚10匹〖30匹ペース)
            500, 900, 1400, 2000, 2700, 3500, 4400, 5400, 6500, 7800,
            // Lv11 - 20 (ここから1レベルあたり雑魚50匹〖30ペース)
            9200, 10700, 12300, 14000, 15800, 17700, 19700, 21800, 24000, 26500,
            // Lv21 - 30 (中盤：ダンジョン周回前提)
            29000, 31800, 34800, 38000, 41400, 45000, 48800, 52800, 57000, 61500,
            // Lv31 - 40
            66500, 72000, 78000, 84500, 91500, 99000, 107000, 115500, 124500, 134000,
            // Lv41 - 50 (かなりマゾくなってくる)
            144000, 155000, 167000, 180000, 194000, 209000, 225000, 242000, 260000, 280000,
            // Lv51 - 60
            305000, 335000, 370000, 410000, 455000, 505000, 560000, 620000, 685000, 755000,
            // Lv61 - 70 (上位コンテンツ向け)
            830000, 910000, 1000000, 1100000, 1210000, 1330000, 1460000, 1600000, 1750000, 1910000,
            // Lv71 - 80
            2100000, 2300000, 2520000, 2760000, 3020000, 3300000, 3600000, 3920000, 4260000, 4620000,
            // Lv81 - 90
            5000000, 5400000, 5820000, 6260000, 6720000, 7200000, 7700000, 8220000, 8760000, 9320000,
            // Lv91 - 100 (カンストへの道：伝説級)
            10000000, 10800000, 11700000, 12700000, 13800000, 15000000, 16300000, 17700000, 19200000, 21000000
    };

    /** レベルアップ時に付与する属性ポイント数 */
    private static final int POINTS_PER_LEVEL = 2;

    /** 最大レベル */
    private static final int MAX_LEVEL = 100;

    private final PlayerDataRepository repository;
    private final StatManager statManager;
    private final SkillTreeService skillTreeService;

    @Inject
    public PlayerManager(PlayerDataRepository repository, StatManager statManager, SkillTreeService skillTreeService) {
        this.repository = repository;
        this.statManager = statManager;
        this.skillTreeService = skillTreeService;
    }

    /**
     * 指定レベルから次のレベルに必要な経験値を返します。
     * レベル100（最大）の場合は Integer.MAX_VALUE を返します。
     */
    public int getExpToNextLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) return Integer.MAX_VALUE;
        int index = currentLevel - 1; // Lv1→index0
        if (index < 0 || index >= EXP_TABLE.length) return Integer.MAX_VALUE;
        return EXP_TABLE[index];
    }

    /**
     * カスタムレベルとEXPをバニラのレベル・経験値バーに同期させます。
     */
    public void syncVanillaExp(Player player) {
        UUID uuid = player.getUniqueId();
        repository.get(uuid).ifPresent(data -> {
            PlayerLevelProvider.LevelData levelData = data.get(PlayerLevelProvider.KEY);
            if (levelData == null) return;

            int level = levelData.getLevel();
            int currentExp = levelData.getExp();
            int nextExp = getExpToNextLevel(level);

            float expRatio = (nextExp > 0 && nextExp != Integer.MAX_VALUE) 
                    ? (float) currentExp / nextExp 
                    : 0f;

            player.setLevel(level);
            player.setExp(Math.max(0f, Math.min(expRatio, 0.999f)));
        });
    }

    /**
     * 経験値を追加し、必要であればレベルアップ処理を行います。
     */
    public void addExp(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        repository.get(uuid).ifPresent(data -> {
            PlayerLevelProvider.LevelData levelData = data.get(PlayerLevelProvider.KEY);
            if (levelData == null || levelData.getLevel() >= MAX_LEVEL) return;

            int beforeLevel = levelData.getLevel();
            int currentLevel = beforeLevel;
            int newExp = levelData.getExp() + amount;

            // EXP獲得メッセージ
            player.sendMessage(Component.text("+ " + String.format("%,d", amount) + " EXP", NamedTextColor.GREEN));

            boolean leveledUp = false;
            while (newExp >= getExpToNextLevel(currentLevel) && currentLevel < MAX_LEVEL) {
                newExp -= getExpToNextLevel(currentLevel);
                currentLevel++;
                leveledUp = true;

                // 1レベルにつき2ポイント付与
                PlayerAttributeProvider.AttributeData attrData = data.get(PlayerAttributeProvider.KEY);
                if (attrData != null) {
                    attrData.addRemainingPoints(POINTS_PER_LEVEL);
                    data.markDirty(PlayerAttributeProvider.KEY);
                }
            }

            levelData.setLevel(currentLevel);
            levelData.setExp(newExp);
            data.markDirty(PlayerLevelProvider.KEY);
            repository.save(uuid, data);

            // バニラ経験値バーを更新
            syncVanillaExp(player);

            if (leveledUp) {
                int gainedLevels = currentLevel - beforeLevel;
                skillTreeService.grantLevelUpPoints(player, gainedLevels);

                // タイトル表示
                net.kyori.adventure.title.Title title = net.kyori.adventure.title.Title.title(
                        Component.text("LEVEL UP!", NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD),
                        Component.text(beforeLevel, NamedTextColor.YELLOW)
                                .append(Component.text(" → ", NamedTextColor.WHITE))
                                .append(Component.text(currentLevel, NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD))
                );
                player.showTitle(title);

                // サウンド
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

                // チャットメッセージ
                Component separator = Component.text("--------------------------------------", NamedTextColor.AQUA)
                        .decorate(net.kyori.adventure.text.format.TextDecoration.STRIKETHROUGH);
                player.sendMessage(separator);
                player.sendMessage(Component.text("    »» ", NamedTextColor.WHITE, net.kyori.adventure.text.format.TextDecoration.BOLD)
                        .append(Component.text("レベルアップ！", NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD))
                        .append(Component.text(" ««", NamedTextColor.WHITE, net.kyori.adventure.text.format.TextDecoration.BOLD)));
                player.sendMessage(Component.text("   レベル: ", NamedTextColor.YELLOW)
                        .append(Component.text(beforeLevel, NamedTextColor.WHITE))
                        .append(Component.text(" → ", NamedTextColor.WHITE))
                        .append(Component.text(currentLevel, NamedTextColor.GREEN, net.kyori.adventure.text.format.TextDecoration.BOLD)));
                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("- 獲得したボーナス -", NamedTextColor.GRAY));
                player.sendMessage(Component.text("  » ", NamedTextColor.RED)
                        .append(Component.text("属性ポイント: ", NamedTextColor.RED))
                        .append(Component.text(gainedLevels * POINTS_PER_LEVEL, NamedTextColor.WHITE, net.kyori.adventure.text.format.TextDecoration.BOLD)));
                player.sendMessage(Component.text("  » ", NamedTextColor.AQUA)
                        .append(Component.text("スキルポイント: ", NamedTextColor.AQUA))
                        .append(Component.text(gainedLevels * SkillTreeService.SKILL_POINTS_PER_LEVEL, NamedTextColor.WHITE, net.kyori.adventure.text.format.TextDecoration.BOLD)));
                player.sendMessage(separator);
            }

            if (currentLevel >= MAX_LEVEL) {
                Component separator = Component.text("--------------------------------------", NamedTextColor.AQUA)
                        .decorate(net.kyori.adventure.text.format.TextDecoration.STRIKETHROUGH);
                player.sendMessage(separator);
                player.sendMessage(Component.text("    »» ", NamedTextColor.WHITE, net.kyori.adventure.text.format.TextDecoration.BOLD)
                        .append(Component.text("最大レベル到達！", NamedTextColor.DARK_AQUA, net.kyori.adventure.text.format.TextDecoration.BOLD))
                        .append(Component.text(" ««", NamedTextColor.WHITE, net.kyori.adventure.text.format.TextDecoration.BOLD)));
                player.sendMessage(Component.text("   全ての戦いを乗り越えた証！", NamedTextColor.AQUA));
                player.sendMessage(separator);
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

            // providerがまだロードされていない場合はデフォルト値（全0）として扱う
            if (attrData == null) {
                return;
            }

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
        syncVanillaExp(player);
    }
}
