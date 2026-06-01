package com.ruskserver.deepwither_V2.modules.party;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Command(name = "party", aliases = {"p"}, description = "パーティー管理コマンド")
public class PartyCommand implements BasicCommand {

    private final PartyManager partyManager;
    private final PartyTagGUI partyTagGUI;
    private final PartyGUI partyGUI;

    @Inject
    public PartyCommand(PartyManager partyManager, PartyTagGUI partyTagGUI, PartyGUI partyGUI) {
        this.partyManager = partyManager;
        this.partyTagGUI = partyTagGUI;
        this.partyGUI = partyGUI;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("§cこのコマンドはプレイヤーのみ使用できます。"));
            return;
        }

        if (args.length == 0) {
            showHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> handleCreate(player);
            case "invite" -> handleInvite(player, args);
            case "accept" -> partyManager.acceptInvite(player);
            case "leave" -> partyManager.leaveParty(player);
            case "kick" -> handleKick(player, args);
            case "disband" -> partyManager.disbandParty(player);
            case "info" -> handleInfo(player);
            case "public" -> handlePublic(player, args);
            case "private" -> handlePrivate(player);
            case "chat" -> partyManager.toggleChatMode(player);
            case "gui" -> partyGUI.open(player);
            default -> showHelp(player);
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.text("§6===== パーティーコマンド ====="));
        player.sendMessage(Component.text("§e/party create §7- パーティーを作成"));
        player.sendMessage(Component.text("§e/party invite <player> §7- プレイヤーを招待"));
        player.sendMessage(Component.text("§e/party accept §7- 招待を承諾"));
        player.sendMessage(Component.text("§e/party leave §7- パーティーから脱退"));
        player.sendMessage(Component.text("§e/party kick <player> §7- メンバーをキック"));
        player.sendMessage(Component.text("§e/party disband §7- パーティーを解散"));
        player.sendMessage(Component.text("§e/party info §7- パーティー情報を表示"));
        player.sendMessage(Component.text("§e/party public [tags...] §7- 公開募集"));
        player.sendMessage(Component.text("§e/party private §7- 非公開に戻す"));
        player.sendMessage(Component.text("§e/party chat §7- パーティーチャット切替"));
        player.sendMessage(Component.text("§e/party gui §7- パーティーGUIを開く"));
    }

    private void handleCreate(Player player) {
        partyManager.createParty(player);
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("§c使用法: /party invite <player>", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("§cプレイヤーが見つかりません。", NamedTextColor.RED));
            return;
        }
        if (target.equals(player)) {
            player.sendMessage(Component.text("§c自分自身を招待できません。", NamedTextColor.RED));
            return;
        }
        partyManager.invitePlayer(player, target);
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("§c使用法: /party kick <player>", NamedTextColor.RED));
            return;
        }
        partyManager.kickMember(player, args[1]);
    }

    private void handleInfo(Player player) {
        Party party = partyManager.getParty(player);
        if (party == null) {
            player.sendMessage(Component.text("§cパーティーに参加していません。", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("§6===== パーティー情報 ====="));
        player.sendMessage(Component.text("§7リーダー: §f" + getName(party.getLeaderId())));
        player.sendMessage(Component.text("§7メンバー (" + party.getSize() + "/" + party.getMaxMembers() + "):"));
        for (UUID memberId : party.getMembers()) {
            String prefix = memberId.equals(party.getLeaderId()) ? "§6★ " : "§7- ";
            String status;
            Player mp = Bukkit.getPlayer(memberId);
            if (mp != null && mp.isOnline()) {
                status = " §aオンライン";
            } else {
                status = " §cオフライン";
            }
            player.sendMessage(Component.text(prefix + getName(memberId) + status));
        }
        if (party.isPublic()) {
            player.sendMessage(Component.text("§7状態: §a公開募集"));
            if (party.getTags().isEmpty()) {
                player.sendMessage(Component.text("タグ: ", NamedTextColor.GRAY)
                        .append(Component.text("なし", NamedTextColor.GRAY)));
            } else {
                player.sendMessage(Component.text("タグ: ", NamedTextColor.GRAY)
                        .append(Component.join(
                                JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)),
                                party.getTags().stream().map(PartyTag::getComponent).toList()
                        )));
            }
        } else {
            player.sendMessage(Component.text("§7状態: §e非公開"));
        }
    }

    private void handlePublic(Player player, String[] args) {
        Party party = partyManager.getParty(player);
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.sendMessage(Component.text("§cパーティーリーダーのみが公開設定できます。", NamedTextColor.RED));
            return;
        }

        if (args.length > 1) {
            String tagArg = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).toUpperCase();
            for (String tagName : tagArg.split("\\s+")) {
                try {
                    PartyTag tag = PartyTag.valueOf(tagName);
                    party.getTags().add(tag);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(Component.text("§c不明なタグ: " + tagName, NamedTextColor.RED));
                }
            }
            if (!party.getTags().isEmpty()) {
                partyManager.setPartyPublic(player, true);
                return;
            }
        }

        if (party.getTags().isEmpty()) {
            partyTagGUI.open(player);
            return;
        }

        partyManager.setPartyPublic(player, true);
    }

    private void handlePrivate(Player player) {
        partyManager.setPartyPublic(player, false);
    }

    private String getName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        return p != null ? p.getName() : "Unknown";
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return true;
    }

    @Override
    public @NotNull List<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) {
            return List.of("create", "invite", "accept", "leave", "kick", "disband", "info", "public", "private", "chat", "gui");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
