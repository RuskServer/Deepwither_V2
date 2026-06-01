package com.ruskserver.deepwither_V2.modules.party;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Category;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.VoiceChannel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.logging.Logger;

public class PartyVoiceManager {

    private static final String CATEGORY_ID = "1491355316502266006";

    private final JavaPlugin plugin;
    private final Logger logger;
    private boolean discordAvailable;

    public PartyVoiceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.discordAvailable = Bukkit.getPluginManager().getPlugin("DiscordSRV") != null;
        if (!discordAvailable) {
            logger.info("[PartyVoiceManager] DiscordSRV が見つかりません。VC連携は無効です。");
        }
    }

    public void createVoiceChannel(Party party, Player leader) {
        if (!discordAvailable) return;
        if (party.getDiscordVoiceChannelId() != null) return;

        JDA jda = DiscordSRV.getPlugin().getJda();
        if (jda == null) return;

        Guild guild = null;
        for (Guild g : jda.getGuilds()) {
            guild = g;
            break;
        }
        if (guild == null) return;

        Category category = guild.getCategoryById(CATEGORY_ID);
        if (category != null) {
            category.createVoiceChannel("🔊 Party - " + leader.getName())
                    .addPermissionOverride(guild.getPublicRole(), null, java.util.EnumSet.of(Permission.VOICE_CONNECT))
                    .queue(vc -> {
                        party.setDiscordVoiceChannelId(vc.getId());
                        for (UUID memberId : party.getMembers()) {
                            grantVoiceAccess(party, memberId);
                        }
                    }, error -> logger.warning("[PartyVoiceManager] VC作成失敗: " + error.getMessage()));
        } else {
            logger.warning("[PartyVoiceManager] カテゴリが見つかりません: " + CATEGORY_ID);
        }
    }

    public void deleteVoiceChannel(Party party) {
        if (!discordAvailable) return;
        String channelId = party.getDiscordVoiceChannelId();
        if (channelId == null) return;

        JDA jda = DiscordSRV.getPlugin().getJda();
        if (jda == null) return;

        VoiceChannel vc = jda.getVoiceChannelById(channelId);
        if (vc != null) {
            vc.delete().queue(success -> party.setDiscordVoiceChannelId(null),
                    error -> party.setDiscordVoiceChannelId(null));
        } else {
            party.setDiscordVoiceChannelId(null);
        }
    }

    public void grantVoiceAccess(Party party, UUID playerUuid) {
        if (!discordAvailable) return;
        String channelId = party.getDiscordVoiceChannelId();
        if (channelId == null) return;

        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(playerUuid);
        if (discordId == null) return;

        JDA jda = DiscordSRV.getPlugin().getJda();
        if (jda == null) return;

        VoiceChannel vc = jda.getVoiceChannelById(channelId);
        if (vc == null) return;

        vc.getGuild().retrieveMemberById(discordId).queue(member -> {
            vc.upsertPermissionOverride(member)
                    .setAllow(java.util.EnumSet.of(Permission.VOICE_CONNECT))
                    .queue();
        }, error -> {});
    }

    public void revokeVoiceAccess(Party party, UUID playerUuid) {
        if (!discordAvailable) return;
        String channelId = party.getDiscordVoiceChannelId();
        if (channelId == null) return;

        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(playerUuid);
        if (discordId == null) return;

        JDA jda = DiscordSRV.getPlugin().getJda();
        if (jda == null) return;

        VoiceChannel vc = jda.getVoiceChannelById(channelId);
        if (vc == null) return;

        vc.getGuild().retrieveMemberById(discordId).queue(member -> {
            vc.putPermissionOverride(member)
                    .setDeny(java.util.EnumSet.of(Permission.VOICE_CONNECT))
                    .queue();
            if (vc.getMembers().contains(member)) {
                vc.getGuild().kickVoiceMember(member).queue();
            }
        }, error -> {});
    }
}
