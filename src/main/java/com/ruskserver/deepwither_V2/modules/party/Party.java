package com.ruskserver.deepwither_V2.modules.party;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Party {

    private final UUID id;
    private UUID leaderId;
    private final Set<UUID> members;
    private boolean isPublic;
    private final Set<PartyTag> tags;
    private String discordVoiceChannelId;
    private int maxMembers;

    public Party(UUID leaderId) {
        this.id = UUID.randomUUID();
        this.leaderId = leaderId;
        this.members = new HashSet<>();
        this.members.add(leaderId);
        this.isPublic = false;
        this.tags = EnumSet.noneOf(PartyTag.class);
        this.discordVoiceChannelId = null;
        this.maxMembers = 4;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Set<PartyTag> getTags() {
        return tags;
    }

    public String getDiscordVoiceChannelId() {
        return discordVoiceChannelId;
    }

    public void setDiscordVoiceChannelId(String discordVoiceChannelId) {
        this.discordVoiceChannelId = discordVoiceChannelId;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = Math.max(2, Math.min(6, maxMembers));
    }

    public void addMember(UUID playerId) {
        members.add(playerId);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    public boolean isLeader(UUID playerId) {
        return leaderId.equals(playerId);
    }

    public int getSize() {
        return members.size();
    }

    public boolean isFull() {
        return members.size() >= maxMembers;
    }

    public Set<Player> getOnlineMembers() {
        return members.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .collect(Collectors.toSet());
    }
}
