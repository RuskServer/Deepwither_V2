package com.ruskserver.deepwither_V2.modules.party;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class PartyChatListener implements Listener {

    private final PartyManager partyManager;

    @Inject
    public PartyChatListener(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!partyManager.isInPartyChatMode(player.getUniqueId())) return;

        Party party = partyManager.getParty(player);
        if (party == null) return;

        event.setCancelled(true);

        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

        Component partyMessage = Component.text("[Party] ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(player.getName(), NamedTextColor.WHITE))
                .append(Component.text(": ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(plainMessage, NamedTextColor.WHITE));

        for (Player member : party.getOnlineMembers()) {
            member.sendMessage(partyMessage);
        }

        Bukkit.getLogger().info("[PartyChat] " + player.getName() + ": " + plainMessage);
    }
}
