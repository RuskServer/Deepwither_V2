package com.ruskserver.deepwither_V2.modules.artifact;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

@Component
public class ArtifactGenerator {

    private final ArtifactPDCUtil pdcUtil;
    private final Random random = new Random();

    // アーティファクトのサブステータス候補
    private static final List<StatType> SUB_STAT_CANDIDATES = Arrays.asList(
            StatType.ATTACK_DAMAGE, StatType.DEFENSE, StatType.MAGIC_DAMAGE, StatType.MAGIC_DEFENSE,
            StatType.HEALTH, StatType.MAX_MANA, StatType.CRITICAL_CHANCE, StatType.CRITICAL_DAMAGE,
            StatType.ATTACK_SPEED, StatType.SPEED, StatType.COOLDOWN_REDUCTION
    );

    @Inject
    public ArtifactGenerator(ArtifactPDCUtil pdcUtil) {
        this.pdcUtil = pdcUtil;
    }

    /**
     * ランダムな固有アーティファクトを生成します。
     * @param rarity レアリティ
     * @return 生成されたアーティファクトのItemStack
     */
    public ItemStack generateRandomArtifact(ItemRarity rarity) {
        // 固有アーティファクトからランダムに1つ選ぶ
        ArtifactItemType[] types = ArtifactItemType.values();
        ArtifactItemType selectedType = types[random.nextInt(types.length)];

        return generateArtifact(selectedType, rarity);
    }

    /**
     * 指定された固有アーティファクトを生成します。
     */
    public ItemStack generateArtifact(ArtifactItemType type, ItemRarity rarity) {
        // サブステータス抽選 (レアリティ依存)
        int subStatCount = switch (rarity) {
            case COMMON -> 0;
            case UNCOMMON -> 1;
            case RARE -> 2;
            case EPIC -> 3;
            case LEGENDARY -> 4;
        };

        Map<StatType, Double> subStats = new EnumMap<>(StatType.class);
        List<StatType> availableSubStats = new ArrayList<>(SUB_STAT_CANDIDATES);
        availableSubStats.remove(type.getMainStatType()); // メインと被らないようにする

        for (int i = 0; i < subStatCount; i++) {
            if (availableSubStats.isEmpty()) break;
            StatType subStat = availableSubStats.remove(random.nextInt(availableSubStats.size()));
            subStats.put(subStat, generateStatValue(subStat, rarity));
        }

        ArtifactData data = new ArtifactData(type, subStats);

        // アイテム生成
        ItemStack item = new ItemStack(type.getDefaultMaterial());
        ItemMeta meta = item.getItemMeta();

        // PlayerHeadのカスタムテクスチャ適用
        if (type.getDefaultMaterial() == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta) {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            // textureUrlはURLではなく、Base64エンコードされたJSONが必要な場合もあるが、
            // 今回はテクスチャURLのみを持っているため、Base64化してプロパティにセットする
            String json = String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", "http://textures.minecraft.net/texture/" + type.getTextureUrl());
            String base64 = Base64.getEncoder().encodeToString(json.getBytes());
            profile.setProperty(new ProfileProperty("textures", base64));
            skullMeta.setPlayerProfile(profile);
            item.setItemMeta(skullMeta);
        }
        
        pdcUtil.setArtifactData(item, data);
        updateArtifactMeta(item, data, rarity);

        return item;
    }

    /**
     * アーティファクトのアイテムメタ（DisplayName, Lore）を更新します。
     */
    public void updateArtifactMeta(ItemStack item, ArtifactData data, ItemRarity rarity) {
        if (item == null || item.getItemMeta() == null || data == null) return;

        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        // 表示名 (固有アーティファクトの名称)
        meta.displayName(net.kyori.adventure.text.Component.text(data.getItemType().getDisplayName())
                .decoration(TextDecoration.ITALIC, false));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();

        // レアリティとカテゴリ
        lore.add(net.kyori.adventure.text.Component.text("◆ " + rarity.getDisplayName() + " | アーティファクト")
                .color(rarity.getColor())
                .decoration(TextDecoration.ITALIC, false));
        lore.add(net.kyori.adventure.text.Component.empty());

        // メインステータス
        lore.add(net.kyori.adventure.text.Component.text("【メインステータス】", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        lore.add(net.kyori.adventure.text.Component.text("  " + data.getMainStat().getDisplayName() + ": +" + String.format("%.1f", data.getMainStatValue()))
                .color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        lore.add(net.kyori.adventure.text.Component.empty());

        // サブステータス
        if (!data.getSubStats().isEmpty()) {
            lore.add(net.kyori.adventure.text.Component.text("【サブステータス】", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            for (Map.Entry<StatType, Double> entry : data.getSubStats().entrySet()) {
                lore.add(net.kyori.adventure.text.Component.text("  ・" + entry.getKey().getDisplayName() + ": +" + String.format("%.1f", entry.getValue()))
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(net.kyori.adventure.text.Component.empty());
        }

        // セット効果
        lore.add(net.kyori.adventure.text.Component.text("【セット効果】", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        lore.add(net.kyori.adventure.text.Component.text("  " + data.getSetType().getDisplayName())
                .color(NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
        
        // セット効果のLoreをそのまま追加
        List<net.kyori.adventure.text.Component> setLore = data.getSetType().getLoreLines();
        for (net.kyori.adventure.text.Component line : setLore) {
            lore.add(line.decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
    }

    private double generateStatValue(StatType stat, ItemRarity rarity) {
        double baseMultiplier = switch (rarity) {
            case COMMON -> 1.0;
            case UNCOMMON -> 1.5;
            case RARE -> 2.2;
            case EPIC -> 3.5;
            case LEGENDARY -> 5.0;
        };

        double statBase = switch (stat) {
            case HEALTH -> 10.0;
            case MAX_MANA -> 20.0;
            case ATTACK_DAMAGE, MAGIC_DAMAGE -> 2.0;
            case DEFENSE, MAGIC_DEFENSE -> 1.5;
            case CRITICAL_CHANCE, CRITICAL_DAMAGE, SPEED -> 0.5;
            default -> 1.0;
        };

        double variance = 0.8 + (random.nextDouble() * 0.4); // 0.8 ~ 1.2
        double val = statBase * baseMultiplier * variance;

        // 小数第1位で丸める
        return Math.round(val * 10.0) / 10.0;
    }

    public ItemRarity getRandomRarity() {
        double roll = random.nextDouble() * 100;
        if (roll < 5) return ItemRarity.LEGENDARY;
        if (roll < 20) return ItemRarity.EPIC;
        if (roll < 50) return ItemRarity.RARE;
        if (roll < 80) return ItemRarity.UNCOMMON;
        return ItemRarity.COMMON;
    }
}
