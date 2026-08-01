package com.ruskserver.deepwither_V2.modules.mining;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.PickaxeItem;
import com.ruskserver.deepwither_V2.modules.item.durability.EquipmentDurabilityService;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.mining.definition.OreDefinition;
import com.ruskserver.deepwither_V2.modules.mining.definition.OreDrop;
import com.ruskserver.deepwither_V2.modules.mining.definition.OreRegistry;
import com.ruskserver.deepwither_V2.modules.mining.repository.DepletedOre;
import com.ruskserver.deepwither_V2.modules.mining.repository.MiningRespawnRepository;
import com.ruskserver.deepwither_V2.modules.profession.ProfessionService;
import com.ruskserver.deepwither_V2.modules.profession.ProfessionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MiningService implements Startable, Stoppable {

    private final JavaPlugin plugin;
    private final OreRegistry oreRegistry;
    private final MiningSkillService skillService;
    private final ProfessionService professionService;
    private final ItemManager itemManager;
    private final ItemPDCUtil itemPDCUtil;
    private final EquipmentDurabilityService durabilityService;
    private final MiningRespawnRepository respawnRepository;

    private final Map<BlockPosition, OreState> activeStates = new HashMap<>();
    private final Map<BlockPosition, DepletedOre> depletedOres = new HashMap<>();
    private final Map<BlockPosition, BukkitTask> respawnTasks = new HashMap<>();

    @Inject
    public MiningService(
            JavaPlugin plugin,
            OreRegistry oreRegistry,
            MiningSkillService skillService,
            ProfessionService professionService,
            ItemManager itemManager,
            ItemPDCUtil itemPDCUtil,
            EquipmentDurabilityService durabilityService,
            MiningRespawnRepository respawnRepository) {
        this.plugin = plugin;
        this.oreRegistry = oreRegistry;
        this.skillService = skillService;
        this.professionService = professionService;
        this.itemManager = itemManager;
        this.itemPDCUtil = itemPDCUtil;
        this.durabilityService = durabilityService;
        this.respawnRepository = respawnRepository;
    }

    @Override
    public void start() {
        for (DepletedOre ore : respawnRepository.findAll()) {
            depletedOres.put(BlockPosition.of(ore), ore);
            scheduleRespawn(ore);
        }
    }

    @Override
    public void stop() {
        respawnTasks.values().forEach(BukkitTask::cancel);
        respawnTasks.clear();
        activeStates.values().forEach(this::removeDisplay);
        activeStates.clear();
        depletedOres.clear();
    }

    public boolean isTrackedOre(Material material) {
        return oreRegistry.contains(material);
    }

    public boolean handleMiningAttempt(Player player, Block block) {
        OreDefinition definition = oreRegistry.get(block.getType());
        PickaxeItem pickaxe = resolvePickaxe(player);
        if (definition == null || pickaxe == null || !pickaxe.getMineableBlocks().contains(block.getType())) {
            return false;
        }

        BlockPosition position = BlockPosition.of(block);
        if (depletedOres.containsKey(position)) {
            return false;
        }

        OreState state = activeStates.compute(position, (ignored, current) -> {
            if (current == null || current.material != block.getType()) {
                return new OreState(position, block.getType(), definition, definition.durability());
            }
            return current;
        });

        MiningSkillService.MiningProfile profile = skillService.resolveProfile(player);
        MiningSkillService.MiningStrike strike = skillService.resolveStrike(profile);
        state.remainingDurability = Math.max(0, state.remainingDurability - strike.damage());
        ensureDisplay(block, state);
        updateDisplay(state);
        durabilityService.damageItem(player, player.getInventory().getItemInMainHand(), 1);

        if (state.remainingDurability <= 0) {
            playBreakFeedback(block, strike.critical());
            completeMining(player, block, state, profile, pickaxe, true);
        } else {
            playStrikeFeedback(block, strike.critical());
        }
        return true;
    }

    public void protectFromExplosion(List<Block> blocks) {
        blocks.removeIf(block -> oreRegistry.contains(block.getType()));
    }

    public void handleChunkLoad(World world, int chunkX, int chunkZ) {
        long now = System.currentTimeMillis();
        for (DepletedOre ore : List.copyOf(depletedOres.values())) {
            if (ore.worldId().equals(world.getUID())
                    && ore.x() >> 4 == chunkX
                    && ore.z() >> 4 == chunkZ
                    && ore.respawnAtMillis() <= now) {
                restoreOre(ore);
            }
        }
    }

    public void handleWorldLoad(World world) {
        for (DepletedOre ore : List.copyOf(depletedOres.values())) {
            if (ore.worldId().equals(world.getUID())) {
                scheduleRespawn(ore);
            }
        }
    }

    private PickaxeItem resolvePickaxe(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        String itemId = itemPDCUtil.getItemId(item);
        if (itemId == null) {
            return null;
        }
        CustomItem customItem = itemManager.getCustomItem(itemId);
        if (!durabilityService.canUse(player, item, true)) {
            return null;
        }
        return customItem instanceof PickaxeItem pickaxe ? pickaxe : null;
    }

    private void completeMining(
            Player player,
            Block block,
            OreState state,
            MiningSkillService.MiningProfile profile,
            PickaxeItem pickaxe,
            boolean allowGeologicalBreak) {
        activeStates.remove(state.position);
        removeDisplay(state);

        Location dropLocation = block.getLocation().add(0.5D, 0.5D, 0.5D);
        for (ItemStack drop : createDrops(state.definition, profile)) {
            block.getWorld().dropItemNaturally(dropLocation, drop);
        }

        block.setType(Material.AIR, false);
        professionService.addExperience(player, ProfessionType.MINING, state.definition.professionExperience());
        DepletedOre depleted = new DepletedOre(
                state.position.worldId,
                state.position.x,
                state.position.y,
                state.position.z,
                state.material,
                System.currentTimeMillis() + state.definition.respawnTime().toMillis()
        );
        depletedOres.put(state.position, depleted);
        respawnRepository.save(depleted);
        scheduleRespawn(depleted);

        if (allowGeologicalBreak) {
            triggerGeologicalBreak(player, block, profile, pickaxe, state.position);
        }
    }

    private List<ItemStack> createDrops(OreDefinition definition, MiningSkillService.MiningProfile profile) {
        List<ItemStack> result = new ArrayList<>();
        for (OreDrop drop : definition.drops()) {
            double chance = skillService.adjustDropChance(profile, drop.chance());
            if (ThreadLocalRandom.current().nextDouble() > chance) {
                continue;
            }

            int amount = ThreadLocalRandom.current().nextInt(drop.minAmount(), drop.maxAmount() + 1);
            ItemStack stack = drop.customItemId() == null
                    ? new ItemStack(drop.material(), amount)
                    : itemManager.generate(drop.customItemId());
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            if (drop.customItemId() != null) {
                stack.setAmount(Math.min(stack.getMaxStackSize(), amount));
            }
            result.add(stack);
        }
        return result;
    }

    private void triggerGeologicalBreak(
            Player player,
            Block source,
            MiningSkillService.MiningProfile profile,
            PickaxeItem pickaxe,
            BlockPosition sourcePosition) {
        MiningSkillService.GeologicalBurst burst = skillService.resolveGeologicalBurst(profile);
        if (!burst.triggered()) {
            return;
        }

        List<Block> candidates = findNearbyOres(source, burst.radius(), pickaxe, sourcePosition);
        int broken = 0;
        for (Block candidate : candidates) {
            if (broken >= burst.maxBlocks()) {
                break;
            }
            if (resolvePickaxe(player) != pickaxe) {
                break;
            }
            OreDefinition definition = oreRegistry.get(candidate.getType());
            if (definition == null || depletedOres.containsKey(BlockPosition.of(candidate))) {
                continue;
            }

            BlockPosition position = BlockPosition.of(candidate);
            OreState state = new OreState(position, candidate.getType(), definition, 0);
            playBreakFeedback(candidate, false);
            durabilityService.damageItem(player, player.getInventory().getItemInMainHand(), 1);
            completeMining(player, candidate, state, profile, pickaxe, false);
            broken++;
        }
    }

    private List<Block> findNearbyOres(
            Block source,
            int radius,
            PickaxeItem pickaxe,
            BlockPosition sourcePosition) {
        List<Block> result = new ArrayList<>();
        World world = source.getWorld();
        for (int x = source.getX() - radius; x <= source.getX() + radius; x++) {
            for (int y = Math.max(world.getMinHeight(), source.getY() - radius);
                 y <= Math.min(world.getMaxHeight() - 1, source.getY() + radius); y++) {
                for (int z = source.getZ() - radius; z <= source.getZ() + radius; z++) {
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                        continue;
                    }
                    Block block = world.getBlockAt(x, y, z);
                    BlockPosition position = BlockPosition.of(block);
                    if (!position.equals(sourcePosition)
                            && pickaxe.getMineableBlocks().contains(block.getType())
                            && oreRegistry.contains(block.getType())) {
                        result.add(block);
                    }
                }
            }
        }
        result.sort(Comparator.comparingDouble(block -> block.getLocation().distanceSquared(source.getLocation())));
        return result;
    }

    private void scheduleRespawn(DepletedOre ore) {
        BlockPosition position = BlockPosition.of(ore);
        BukkitTask previous = respawnTasks.remove(position);
        if (previous != null) {
            previous.cancel();
        }
        if (Bukkit.getWorld(ore.worldId()) == null) {
            return;
        }

        long delayMillis = Math.max(0L, ore.respawnAtMillis() - System.currentTimeMillis());
        long delayTicks = Math.max(1L, (delayMillis + 49L) / 50L);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            respawnTasks.remove(position);
            restoreOre(ore);
        }, delayTicks);
        respawnTasks.put(position, task);
    }

    private void restoreOre(DepletedOre ore) {
        World world = Bukkit.getWorld(ore.worldId());
        if (world == null || !world.isChunkLoaded(ore.x() >> 4, ore.z() >> 4)) {
            return;
        }

        BlockPosition position = BlockPosition.of(ore);
        BukkitTask task = respawnTasks.remove(position);
        if (task != null) {
            task.cancel();
        }
        Block block = world.getBlockAt(ore.x(), ore.y(), ore.z());
        if (block.getType().isAir()) {
            block.setType(ore.material(), false);
        }
        depletedOres.remove(position);
        respawnRepository.delete(ore);
    }

    private void ensureDisplay(Block block, OreState state) {
        TextDisplay display = state.displayId == null ? null : findDisplay(state.displayId);
        if (display == null) {
            Location location = block.getLocation().add(0.5D, 1.15D, 0.5D);
            display = block.getWorld().spawn(location, TextDisplay.class, spawned -> {
                spawned.setBillboard(Display.Billboard.CENTER);
                spawned.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
                spawned.setShadowed(true);
                spawned.setPersistent(false);
                spawned.setViewRange(16.0f);
            });
            state.displayId = display.getUniqueId();
        }
    }

    private void updateDisplay(OreState state) {
        TextDisplay display = state.displayId == null ? null : findDisplay(state.displayId);
        if (display == null) {
            return;
        }

        int segments = state.definition.barSegments();
        int filled = (int) Math.ceil((double) state.remainingDurability / state.definition.durability() * segments);
        filled = Math.max(0, Math.min(segments, filled));
        Component bar = Component.empty();
        for (int index = 0; index < segments; index++) {
            bar = bar.append(Component.text(index < filled ? "■" : "□",
                    index < filled ? state.definition.filledColor() : NamedTextColor.DARK_GRAY));
        }
        display.text(bar);
    }

    private TextDisplay findDisplay(UUID displayId) {
        var entity = Bukkit.getEntity(displayId);
        return entity instanceof TextDisplay display && display.isValid() ? display : null;
    }

    private void removeDisplay(OreState state) {
        if (state.displayId == null) {
            return;
        }
        TextDisplay display = findDisplay(state.displayId);
        if (display != null) {
            display.remove();
        }
        state.displayId = null;
    }

    private void playStrikeFeedback(Block block, boolean critical) {
        Location center = block.getLocation().add(0.5D, 0.5D, 0.5D);
        block.getWorld().playSound(center, Sound.BLOCK_STONE_HIT, 0.55f, critical ? 1.35f : 1.0f);
        block.getWorld().spawnParticle(Particle.BLOCK, center, 5, 0.18, 0.18, 0.18, 0.04,
                block.getBlockData());
        if (critical) {
            block.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.2f);
            block.getWorld().spawnParticle(Particle.CRIT, center, 8, 0.2, 0.2, 0.2, 0.1);
        }
    }

    private void playBreakFeedback(Block block, boolean critical) {
        Location center = block.getLocation().add(0.5D, 0.5D, 0.5D);
        block.getWorld().playSound(center, Sound.BLOCK_STONE_BREAK, 0.9f, 0.9f);
        block.getWorld().playSound(center, Sound.BLOCK_ANVIL_LAND, 0.3f, critical ? 1.5f : 1.25f);
        block.getWorld().spawnParticle(Particle.BLOCK, center, 18, 0.28, 0.28, 0.28, 0.08,
                block.getBlockData());
    }

    private static final class OreState {
        private final BlockPosition position;
        private final Material material;
        private final OreDefinition definition;
        private int remainingDurability;
        private UUID displayId;

        private OreState(
                BlockPosition position,
                Material material,
                OreDefinition definition,
                int remainingDurability) {
            this.position = position;
            this.material = material;
            this.definition = definition;
            this.remainingDurability = remainingDurability;
        }
    }

    private record BlockPosition(UUID worldId, int x, int y, int z) {
        private static BlockPosition of(Block block) {
            return new BlockPosition(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }

        private static BlockPosition of(DepletedOre ore) {
            return new BlockPosition(ore.worldId(), ore.x(), ore.y(), ore.z());
        }
    }
}
