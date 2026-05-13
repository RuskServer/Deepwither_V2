package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.artifact.ArtifactGenerator;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ArtifactBoxItem implements CustomItem {

    private final ArtifactGenerator artifactGenerator;
    private final Map<StatType, Double> baseStats;

    @Inject
    public ArtifactBoxItem(ArtifactGenerator artifactGenerator) {
        this.artifactGenerator = artifactGenerator;
        this.baseStats = new EnumMap<>(StatType.class);
    }

    @Override
    public String getId() {
        return "artifact_box";
    }

    @Override
    public Material getMaterial() {
        return Material.ENDER_CHEST;
    }

    @Override
    public String getDisplayName() {
        return "未鑑定のアーティファクト";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public String getFlavorText() {
        return "何が眠っているか分からない、謎めいたオーラを放つ箱。右クリックで鑑定する。";
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        item.setAmount(item.getAmount() - 1);

        ItemRarity randomRarity = artifactGenerator.getRandomRarity();
        ItemStack artifact = artifactGenerator.generateRandomArtifact(randomRarity);

        var leftOvers = player.getInventory().addItem(artifact);
        if (!leftOvers.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.values().iterator().next());
        }

        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
    }
}
