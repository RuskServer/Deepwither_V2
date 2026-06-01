package com.ruskserver.deepwither_V2.modules.party;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.party.event.PartyDisbandEvent;
import com.ruskserver.deepwither_V2.modules.party.event.PartyJoinEvent;
import com.ruskserver.deepwither_V2.modules.party.event.PartyLeaveEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class PartyManager implements Startable, Stoppable, Listener {

    private final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    private final Map<UUID, Party> playerPartyMap = new ConcurrentHashMap<>();
    private final Map<UUID, Invite> pendingInvites = new ConcurrentHashMap<>();
    private final Set<UUID> partyChatModePlayers = ConcurrentHashMap.newKeySet();

    private final PartyVoiceManager voiceManager;
    private final JavaPlugin plugin;
    private final Logger logger;

    @Inject
    public PartyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.voiceManager = new PartyVoiceManager(plugin);
    }

    @Override
    public void start() {
        logger.info("[PartyManager] パーティーシステムを開始しました");
    }

    @Override
    public void stop() {
        for (Party party : parties.values()) {
            voiceManager.deleteVoiceChannel(party);
        }
        parties.clear();
        playerPartyMap.clear();
        pendingInvites.clear();
        partyChatModePlayers.clear();
    }

    public Party createParty(Player leader) {
        UUID leaderId = leader.getUniqueId();
        if (playerPartyMap.containsKey(leaderId)) {
            return playerPartyMap.get(leaderId);
        }
        Party party = new Party(leaderId);
        parties.put(party.getId(), party);
        playerPartyMap.put(leaderId, party);
        leader.sendMessage(Component.text("§aパーティーを作成しました！", NamedTextColor.GREEN));
        return party;
    }

    public void invitePlayer(Player leader, Player target) {
        UUID targetId = target.getUniqueId();
        if (pendingInvites.containsKey(targetId)) {
            leader.sendMessage(Component.text("§cそのプレイヤーは既に招待を受けています。", NamedTextColor.RED));
            return;
        }

        Party party = playerPartyMap.get(leader.getUniqueId());
        if (party == null) {
            party = createParty(leader);
        }
        if (!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(Component.text("§c招待権限がありません（リーダーのみ）。", NamedTextColor.RED));
            return;
        }
        if (party.isFull()) {
            leader.sendMessage(Component.text("§cパーティーが満員です。", NamedTextColor.RED));
            return;
        }
        if (party.isMember(targetId)) {
            leader.sendMessage(Component.text("§cそのプレイヤーは既にメンバーです。", NamedTextColor.RED));
            return;
        }

        Invite invite = new Invite(targetId, leader.getUniqueId(), party.getId());
        pendingInvites.put(targetId, invite);

        invite.expiryTask = new BukkitRunnable() {
            @Override
            public void run() {
                Invite current = pendingInvites.get(targetId);
                if (current != null && current.inviterId.equals(leader.getUniqueId())) {
                    pendingInvites.remove(targetId);
                    if (leader.isOnline()) {
                        leader.sendMessage(Component.text("§e" + target.getName() + " への招待が期限切れになりました。", NamedTextColor.YELLOW));
                    }
                    if (target.isOnline()) {
                        target.sendMessage(Component.text("§e" + leader.getName() + " からの招待が期限切れになりました。", NamedTextColor.YELLOW));
                    }
                }
            }
        }.runTaskLater(plugin, 20L * 60);

        target.sendMessage(Component.text("§e" + leader.getName() + " があなたをパーティーに招待しました！", NamedTextColor.YELLOW));
        target.sendMessage(
                Component.text("§a[ここをクリックして参加]", NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/party accept"))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                Component.text("§7クリックでパーティーに参加", NamedTextColor.GRAY)))
        );
        leader.sendMessage(Component.text("§a" + target.getName() + " を招待しました。", NamedTextColor.GREEN));
    }

    public void acceptInvite(Player player) {
        UUID playerId = player.getUniqueId();
        Invite invite = pendingInvites.remove(playerId);
        if (invite == null) {
            player.sendMessage(Component.text("§c招待を受けていません。", NamedTextColor.RED));
            return;
        }
        if (invite.expiryTask != null) {
            invite.expiryTask.cancel();
        }

        Party party = parties.get(invite.partyId);
        if (party == null) {
            Player inviter = Bukkit.getPlayer(invite.inviterId);
            if (inviter == null || !inviter.isOnline()) {
                player.sendMessage(Component.text("§c招待者がオフラインのため参加できませんでした。", NamedTextColor.RED));
                return;
            }
            party = createParty(inviter);
        }

        joinPartyLogic(player, party);
    }

    public void joinPartyLogic(Player player, Party party) {
        UUID playerId = player.getUniqueId();
        if (playerPartyMap.containsKey(playerId)) {
            player.sendMessage(Component.text("§c既にパーティーに参加しています。先に脱退してください。", NamedTextColor.RED));
            return;
        }
        if (party.isFull()) {
            player.sendMessage(Component.text("§cこのパーティーは満員です (" + party.getMaxMembers() + "名)。", NamedTextColor.RED));
            return;
        }

        party.addMember(playerId);
        playerPartyMap.put(playerId, party);

        Bukkit.getPluginManager().callEvent(new PartyJoinEvent(party, player));

        voiceManager.grantVoiceAccess(party, playerId);

        Component joinMsg = Component.text("§a" + player.getName() + " がパーティーに参加しました！", NamedTextColor.GREEN);
        for (Player member : party.getOnlineMembers()) {
            member.sendMessage(joinMsg);
        }
    }

    public void leaveParty(Player player) {
        UUID playerId = player.getUniqueId();
        Party party = playerPartyMap.get(playerId);
        if (party == null) {
            player.sendMessage(Component.text("§cパーティーに参加していません。", NamedTextColor.RED));
            return;
        }

        if (party.isLeader(playerId)) {
            disbandParty(player);
            return;
        }

        voiceManager.revokeVoiceAccess(party, playerId);
        party.removeMember(playerId);
        playerPartyMap.remove(playerId);
        partyChatModePlayers.remove(playerId);

        Bukkit.getPluginManager().callEvent(new PartyLeaveEvent(party, player, PartyLeaveEvent.Reason.LEAVE));

        player.sendMessage(Component.text("§eパーティーから脱退しました。", NamedTextColor.YELLOW));
        Component leaveMsg = Component.text("§e" + player.getName() + " が脱退しました。", NamedTextColor.YELLOW);
        for (Player member : party.getOnlineMembers()) {
            member.sendMessage(leaveMsg);
        }
    }

    public void kickMember(Player leader, String targetName) {
        UUID leaderId = leader.getUniqueId();
        Party party = playerPartyMap.get(leaderId);
        if (party == null || !party.isLeader(leaderId)) {
            leader.sendMessage(Component.text("§cあなたはパーティーリーダーではありません。", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            leader.sendMessage(Component.text("§cプレイヤーが見つかりません。", NamedTextColor.RED));
            return;
        }

        UUID targetId = target.getUniqueId();
        if (!party.isMember(targetId)) {
            leader.sendMessage(Component.text("§cそのプレイヤーはメンバーではありません。", NamedTextColor.RED));
            return;
        }
        if (party.isLeader(targetId)) {
            leader.sendMessage(Component.text("§c自分自身はキックできません。解散を使用してください。", NamedTextColor.RED));
            return;
        }

        voiceManager.revokeVoiceAccess(party, targetId);
        party.removeMember(targetId);
        playerPartyMap.remove(targetId);
        partyChatModePlayers.remove(targetId);

        Bukkit.getPluginManager().callEvent(new PartyLeaveEvent(party, target, PartyLeaveEvent.Reason.KICK));

        target.sendMessage(Component.text("§cパーティーから追放されました。", NamedTextColor.RED));
        Component kickMsg = Component.text("§e" + target.getName() + " が追放されました。", NamedTextColor.YELLOW);
        for (Player member : party.getOnlineMembers()) {
            member.sendMessage(kickMsg);
        }
    }

    public void disbandParty(Player leader) {
        UUID leaderId = leader.getUniqueId();
        Party party = playerPartyMap.get(leaderId);
        if (party == null || !party.isLeader(leaderId)) {
            leader.sendMessage(Component.text("§c解散権限がありません。", NamedTextColor.RED));
            return;
        }

        Bukkit.getPluginManager().callEvent(new PartyDisbandEvent(party, leader));

        voiceManager.deleteVoiceChannel(party);

        Component disbandMsg = Component.text("§cパーティーが解散されました。", NamedTextColor.RED);
        for (UUID memberId : party.getMembers()) {
            playerPartyMap.remove(memberId);
            partyChatModePlayers.remove(memberId);
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(disbandMsg);
            }
        }

        parties.remove(party.getId());
    }

    public void setPartyPublic(Player leader, boolean isPublic) {
        UUID leaderId = leader.getUniqueId();
        Party party = playerPartyMap.get(leaderId);
        if (party == null || !party.isLeader(leaderId)) {
            leader.sendMessage(Component.text("§cパーティーを所有していません。", NamedTextColor.RED));
            return;
        }

        party.setPublic(isPublic);

        if (isPublic) {
            if (party.getTags().contains(PartyTag.DISCORD)) {
                voiceManager.createVoiceChannel(party, leader);
            }

            Component separator = Component.text("======================================", NamedTextColor.GOLD)
                    .decorate(TextDecoration.STRIKETHROUGH);
            Component title = Component.text("»» [パーティー募集] ««", NamedTextColor.AQUA)
                    .decorate(TextDecoration.BOLD);
            Component desc = Component.text("リーダー: ", NamedTextColor.GRAY)
                    .append(Component.text(leader.getName(), NamedTextColor.WHITE));
            Component tagsLine = Component.text("タグ: ", NamedTextColor.GRAY)
                    .append(Component.join(
                            JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)),
                            party.getTags().stream().map(PartyTag::getComponent).toList()
                    ));
            Component joinHint = Component.text("§7/party gui から参加できます", NamedTextColor.GRAY);

            Bukkit.getServer().sendMessage(separator);
            Bukkit.getServer().sendMessage(title);
            Bukkit.getServer().sendMessage(desc);
            Bukkit.getServer().sendMessage(tagsLine);
            Bukkit.getServer().sendMessage(joinHint);
            Bukkit.getServer().sendMessage(separator);

            leader.sendMessage(Component.text("§aパーティーを公開しました！", NamedTextColor.GREEN));
            leader.sendMessage(Component.text("§7/party gui から募集タグを管理できます。", NamedTextColor.GRAY));
        } else {
            leader.sendMessage(Component.text("§eパーティーを非公開にしました。", NamedTextColor.YELLOW));
        }
    }

    public List<Party> getPublicParties() {
        return new ArrayList<>(new HashSet<>(parties.values())).stream()
                .filter(Party::isPublic)
                .collect(Collectors.toList());
    }

    public void joinPublicParty(Player player, UUID partyId) {
        UUID playerId = player.getUniqueId();
        if (playerPartyMap.containsKey(playerId)) {
            player.sendMessage(Component.text("§c既にパーティーに参加しています。", NamedTextColor.RED));
            return;
        }

        Party party = parties.get(partyId);
        if (party == null) {
            player.sendMessage(Component.text("§cそのパーティーは既に解散したか、見つかりません。", NamedTextColor.RED));
            return;
        }
        if (!party.isPublic()) {
            player.sendMessage(Component.text("§cそのパーティーは現在非公開です。", NamedTextColor.RED));
            return;
        }

        joinPartyLogic(player, party);
    }

    public Party getParty(Player player) {
        return playerPartyMap.get(player.getUniqueId());
    }

    public boolean isInParty(Player player) {
        return playerPartyMap.containsKey(player.getUniqueId());
    }

    public void toggleChatMode(Player player) {
        UUID playerId = player.getUniqueId();
        if (!playerPartyMap.containsKey(playerId)) {
            player.sendMessage(Component.text("§cパーティーに参加していないためチャットモードを切り替えられません。", NamedTextColor.RED));
            return;
        }

        if (partyChatModePlayers.contains(playerId)) {
            partyChatModePlayers.remove(playerId);
            player.sendMessage(Component.text("§eチャットモードを [グローバル] に切り替えました。", NamedTextColor.YELLOW));
        } else {
            partyChatModePlayers.add(playerId);
            player.sendMessage(Component.text("§aチャットモードを [パーティー] に切り替えました。", NamedTextColor.GREEN));
        }
    }

    public boolean isInPartyChatMode(UUID playerId) {
        return partyChatModePlayers.contains(playerId);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Party party = playerPartyMap.get(playerId);
        if (party == null) return;

        if (party.isLeader(playerId)) {
            disbandParty(player);
        } else {
            voiceManager.revokeVoiceAccess(party, playerId);
            party.removeMember(playerId);
            playerPartyMap.remove(playerId);
            partyChatModePlayers.remove(playerId);
            Bukkit.getPluginManager().callEvent(new PartyLeaveEvent(party, player, PartyLeaveEvent.Reason.DISCONNECT));

            Component leaveMsg = Component.text("§e" + player.getName() + " がログアウトしました。", NamedTextColor.YELLOW);
            for (Player member : party.getOnlineMembers()) {
                member.sendMessage(leaveMsg);
            }
        }
    }

    private static class Invite {
        final UUID targetId;
        final UUID inviterId;
        final UUID partyId;
        final long createdAt;
        BukkitTask expiryTask;

        Invite(UUID targetId, UUID inviterId, UUID partyId) {
            this.targetId = targetId;
            this.inviterId = inviterId;
            this.partyId = partyId;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
