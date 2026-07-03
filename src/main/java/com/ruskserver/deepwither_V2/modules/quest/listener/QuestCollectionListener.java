package com.ruskserver.deepwither_V2.modules.quest.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestState;
import com.ruskserver.deepwither_V2.modules.quest.provider.QuestProgressProvider.QuestProgress;
import com.ruskserver.deepwither_V2.modules.quest.service.QuestService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class QuestCollectionListener implements Listener {

    private final QuestService questService;
    private final ItemPDCUtil pdcUtil;

    @Inject
    public QuestCollectionListener(QuestService questService, ItemPDCUtil pdcUtil) {
        this.questService = questService;
        this.pdcUtil = pdcUtil;
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        QuestProgress progress = questService.getProgress(player.getUniqueId());
        if (progress.isEmpty() || progress.state() != QuestState.ACCEPTED) return;

        ItemStack picked = event.getItem().getItemStack();
        String itemId = pdcUtil.getItemId(picked);
        if (itemId == null) return;

        var quest = questService.getQuest(progress.questId());
        if (quest == null) return;

        boolean isQuestItem = quest.getObjectives().stream()
                .anyMatch(obj -> obj.getItemId().equals(itemId));
        if (!isQuestItem) return;

        if (questService.checkCompletion(player)) {
            player.sendMessage(Component.text("§a§l全てのアイテムが揃いました！ 村長に報告しましょう。"));
        }
    }
}
