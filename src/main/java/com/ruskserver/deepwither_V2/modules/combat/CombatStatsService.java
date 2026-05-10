package com.ruskserver.deepwither_V2.modules.combat;

import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import org.bukkit.entity.Player;

@Service
public class CombatStatsService {

    private final PlayerDataRepository repository;

    @Inject
    public CombatStatsService(PlayerDataRepository repository) {
        this.repository = repository;
    }

    public void recordAttack(Player player) {
        repository.get(player.getUniqueId()).ifPresent(data -> {
            CombatStatsProvider.CombatStatsData stats = data.get(CombatStatsProvider.KEY);
            stats.incrementAttack();
            data.markDirty(CombatStatsProvider.KEY);
            repository.save(player.getUniqueId(), data);
        });
    }

    public void recordHit(Player player, CombatWeaponType type, double damage) {
        repository.get(player.getUniqueId()).ifPresent(data -> {
            CombatStatsProvider.CombatStatsData stats = data.get(CombatStatsProvider.KEY);
            stats.registerHit(type, damage);
            data.markDirty(CombatStatsProvider.KEY);
            repository.save(player.getUniqueId(), data);
        });
    }
}
