package com.ruskserver.deepwither_V2.modules.party;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Category;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.VoiceChannel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PartyVoiceManager {

    private static final String CATEGORY_ID = "1491355316502266006";
    private static final String VC_PREFIX = "🔊 Party - ";
    private static final long EMPTY_VC_TIMEOUT_MILLIS = 3 * 60 * 1000L; // 3分間誰もいない空VCを自動削除

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Map<String, Long> emptyChannelSince = new ConcurrentHashMap<>();
    private boolean discordAvailable;
    private BukkitTask cleanupTask;

    public PartyVoiceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.discordAvailable = Bukkit.getPluginManager().getPlugin("DiscordSRV") != null;
        if (!discordAvailable) {
            logger.info("[PartyVoiceManager] DiscordSRV が見つかりません。VC連携は無効です。");
        }
    }

    public void start() {
        if (!discordAvailable) return;
        // 30秒ごとに空VCクリーンアップをチェック
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupEmptyChannels, 20L * 30, 20L * 30);
        logger.info("[PartyVoiceManager] パーティーVC管理タスクを開始しました（3分遅延クリーンアップ有効）");
    }

    public void stop() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        emptyChannelSince.clear();
    }

    /**
     * パーティーVCを作成、または既存の同名VCを再利用（Re-link）する。
     */
    public void createVoiceChannel(Party party, Player leader) {
        if (!discordAvailable) return;
        if (party.getDiscordVoiceChannelId() != null) return;

        JDA jda = getJda();
        if (jda == null) return;

        Category category = getPartyCategory(jda);
        if (category == null) {
            logger.warning("[PartyVoiceManager] カテゴリが見つかりません: " + CATEGORY_ID);
            return;
        }

        String targetName = VC_PREFIX + leader.getName();

        // 1. 既存の同名VCが存在するか確認し、存在すれば再利用する (Re-link)
        for (VoiceChannel existing : category.getVoiceChannels()) {
            if (existing.getName().equalsIgnoreCase(targetName)) {
                party.setDiscordVoiceChannelId(existing.getId());
                emptyChannelSince.remove(existing.getId());
                logger.info("[PartyVoiceManager] 既存のパーティーVCを再利用しました: " + existing.getName());
                for (UUID memberId : party.getMembers()) {
                    grantVoiceAccess(party, memberId);
                }
                return;
            }
        }

        // 2. 存在しない場合は新規作成
        Guild guild = category.getGuild();
        category.createVoiceChannel(targetName)
                .addPermissionOverride(guild.getPublicRole(), null, java.util.EnumSet.of(Permission.VOICE_CONNECT))
                .queue(vc -> {
                    party.setDiscordVoiceChannelId(vc.getId());
                    emptyChannelSince.remove(vc.getId());
                    for (UUID memberId : party.getMembers()) {
                        grantVoiceAccess(party, memberId);
                    }
                }, error -> logger.warning("[PartyVoiceManager] VC作成失敗: " + error.getMessage()));
    }

    /**
     * パーティー解散時などにVCを整理する。
     * VC内に通話中のプレイヤーがいる場合は即座に切断せず、遅延クリーンアップへ移行する。
     */
    public void deleteVoiceChannel(Party party) {
        if (!discordAvailable) return;
        String channelId = party.getDiscordVoiceChannelId();
        if (channelId == null) return;

        party.setDiscordVoiceChannelId(null);

        JDA jda = getJda();
        if (jda == null) return;

        VoiceChannel vc = jda.getVoiceChannelById(channelId);
        if (vc != null) {
            if (vc.getMembers().isEmpty()) {
                vc.delete().queue(null, error -> {});
                emptyChannelSince.remove(channelId);
            } else {
                // 通話中のプレイヤーがいる場合はタイマーをセットして後から削除
                emptyChannelSince.putIfAbsent(channelId, System.currentTimeMillis());
            }
        }
    }

    public void grantVoiceAccess(Party party, UUID playerUuid) {
        if (!discordAvailable) return;
        String channelId = party.getDiscordVoiceChannelId();
        if (channelId == null) return;

        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(playerUuid);
        if (discordId == null) return;

        JDA jda = getJda();
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

        JDA jda = getJda();
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

    /**
     * 該当カテゴリ内の誰もいない空のパーティーVCを走査し、3分以上経過したものを削除する。
     */
    private void cleanupEmptyChannels() {
        JDA jda = getJda();
        if (jda == null) return;

        Category category = getPartyCategory(jda);
        if (category == null) return;

        long now = System.currentTimeMillis();

        for (VoiceChannel vc : category.getVoiceChannels()) {
            if (!vc.getName().startsWith(VC_PREFIX)) {
                continue;
            }

            if (vc.getMembers().isEmpty()) {
                Long since = emptyChannelSince.putIfAbsent(vc.getId(), now);
                if (since != null && (now - since) >= EMPTY_VC_TIMEOUT_MILLIS) {
                    vc.delete().queue(
                            success -> logger.info("[PartyVoiceManager] 空のパーティーVCを自動削除しました: " + vc.getName()),
                            error -> logger.warning("[PartyVoiceManager] VC削除エラー: " + error.getMessage())
                    );
                    emptyChannelSince.remove(vc.getId());
                }
            } else {
                // メンバーが入っている場合は空タイマーをリセット
                emptyChannelSince.remove(vc.getId());
            }
        }
    }

    private JDA getJda() {
        if (!discordAvailable) return null;
        try {
            return DiscordSRV.getPlugin().getJda();
        } catch (Exception e) {
            return null;
        }
    }

    private Category getPartyCategory(JDA jda) {
        for (Guild g : jda.getGuilds()) {
            Category c = g.getCategoryById(CATEGORY_ID);
            if (c != null) return c;
        }
        return null;
    }
}
