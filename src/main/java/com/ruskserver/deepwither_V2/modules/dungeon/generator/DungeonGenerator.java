package com.ruskserver.deepwither_V2.modules.dungeon.generator;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.dungeon.definition.DungeonDefinition;
import com.ruskserver.deepwither_V2.modules.dungeon.definition.RoomSlot;
import com.ruskserver.deepwither_V2.modules.dungeon.room.DoorDefinition;
import com.ruskserver.deepwither_V2.modules.dungeon.room.RoomTemplateRegistry;
import com.ruskserver.deepwither_V2.modules.dungeon.room.RoomType;
import com.ruskserver.deepwither_V2.modules.dungeon.schematic.RoomSchematic;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * ダンジョンの逐次生成 + 衝突検出ロジック。
 * <p>
 * 入口ルームだけを即時配置し、以降はプレイヤーがドアに近づくたびに
 * {@link #generateNextRoom} を呼び出して1ルームずつ追加生成します。
 * <p>
 * ルーム選択は {@link RoomSlot#weight()} に基づく重み付きランダム選択、
 * 通路と部屋の配置ペースは {@link DungeonDefinition#minCorridorsBeforeRoom()} /
 * {@link DungeonDefinition#maxCorridorsBeforeRoom()} で制御します。
 */
@Service
public class DungeonGenerator {

    private final RoomTemplateRegistry templateRegistry;
    private final RoomPlacer roomPlacer;
    private final Logger log;

    @Inject
    public DungeonGenerator(RoomTemplateRegistry templateRegistry, Logger log) {
        this.templateRegistry = templateRegistry;
        this.roomPlacer = new RoomPlacer(log);
        this.log = log;
    }

    // ========================================================================
    // エントリー配置（即時）
    // ========================================================================

    /**
     * 入口ルームのみを配置し、初期状態を返します。
     *
     * @param definition ダンジョン定義
     * @param world      対象ワールド
     * @param origin     生成開始座標
     * @return エントリー配置結果
     */
    public EntryResult generateEntry(DungeonDefinition definition, World world, BlockVector3 origin) {
        DungeonLayout layout = new DungeonLayout();
        List<DoorConnection> pendingDoors = new ArrayList<>();

        RoomSlot entrySlot = definition.roomSlots().isEmpty() ? null : definition.roomSlots().getFirst();
        if (entrySlot == null) {
            log.severe("[DungeonGenerator] ルームスロットが定義されていません: " + definition.id());
            return new EntryResult(layout, pendingDoors, 0);
        }

        RoomSchematic entryTemplate = templateRegistry.getTemplateForSlot(definition.id(), entrySlot);
        if (entryTemplate == null) {
            log.severe("[DungeonGenerator] エントリールームが見つかりません: " + definition.id());
            return new EntryResult(layout, pendingDoors, 0);
        }

        PlacementResult entryResult = roomPlacer.placeRoom(entryTemplate, world, origin, 0, null);
        for (DoorDefinition door : entryTemplate.entryDoors()) {
            roomPlacer.sealDoor(world, origin.add(rotatePosition(door.position(), 0)));
        }
        layout.addPlacedRoom(new DungeonLayout.PlacedRoom(entryTemplate, origin, 0, entryResult.entryDoorWorldPos()));
        layout.addMobSpawnPositions(entryResult.mobSpawnWorldPositions());
        layout.addBossSpawnPosition(entryResult.bossSpawnWorldPos());
        layout.addLootPositions(entryResult.lootWorldPositions());

        for (DoorDefinition door : entryTemplate.exitDoors()) {
            BlockVector3 doorWorldPos = origin.add(rotatePosition(door.position(), 0));
            DoorDefinition rotatedDoor = new DoorDefinition(
                    door.position(),
                    door.direction().rotate(0),
                    door.type(),
                    door.accepts()
            );
            pendingDoors.add(new DoorConnection(entryTemplate, rotatedDoor, doorWorldPos, null));
        }

        if (entryTemplate.exitDoors().isEmpty()) {
            log.warning("[DungeonGenerator] エントリールーム出口ドアが0個です: " + entryTemplate.schematicId());
        }

        log.info("[DungeonGenerator] " + definition.id() + " エントリー配置完了: "
                + pendingDoors.size() + " 個の未接続ドア");
        return new EntryResult(layout, pendingDoors, 0);
    }

    public void sealDoor(World world, DoorConnection door) {
        roomPlacer.sealDoor(world, door.worldPosition());
    }

    public record EntryResult(DungeonLayout layout, List<DoorConnection> pendingDoors, int depth) {
    }

    // ========================================================================
    // 逐次生成（疑似乱数 + 重み付き選択 + ペース制御）
    // ========================================================================

    /**
     * 指定された未接続ドアに次のルームを1つ配置します。
     * <p>
     * 重み付きランダム選択と部屋/通路のペース制御を行います。
     *
     * @param definition          ダンジョン定義
     * @param world               対象ワールド
     * @param layout              現在のレイアウト
     * @param fromDoor            接続元の未接続ドア
     * @param currentDepth        現在の深度
     * @param branchingEnabled    分岐が有効か
     * @param rng                 疑似乱数生成器
     * @param consecutiveCorridors 現在の連続通路数
     * @return 生成結果
     */
    public GenerationResult generateNextRoom(
            DungeonDefinition definition,
            World world,
            DungeonLayout layout,
            DoorConnection fromDoor,
            int currentDepth,
            boolean branchingEnabled,
            Random rng,
            int consecutiveCorridors
    ) {
        // 最後の深度は必ずボスルームに予約する。
        // ボスルームは通常のRoomSlotやドアタグには依存せず、専用定義から直接配置する。
        if (definition.hasBossRoom() && currentDepth >= definition.maxDepth() - 1) {
            GenerationResult bossResult = placeBossRoom(definition, world, layout, fromDoor);
            if (bossResult != null) {
                return bossResult;
            }
            log.warning("[DungeonGenerator] 最深部へのボスルーム配置に失敗: "
                    + definition.id() + " door=" + fromDoor.worldPosition());
            return placeDeadEnd(definition, world, layout, fromDoor);
        }

        if (currentDepth >= definition.maxDepth()) {
            return placeDeadEnd(definition, world, layout, fromDoor);
        }

        // 確率的分岐判定（branchChance を実際の確率として使用）
        boolean tryBranching = branchingEnabled
                && fromDoor.door().acceptsAny(List.of("branch", "t", "cross"))
                && rng.nextDouble() < definition.branchChance();

        List<RoomSlot> candidates = buildCandidates(definition, tryBranching, branchingEnabled);
        if (candidates.isEmpty()) {
            return placeDeadEnd(definition, world, layout, fromDoor);
        }

        // ペース制御 + 重み付き選択
        RoomSlot selectedSlot = selectSlot(candidates, definition, consecutiveCorridors, tryBranching, rng);
        if (selectedSlot == null) {
            return placeDeadEnd(definition, world, layout, fromDoor);
        }

        // 衝突フォールバック付き配置試行
        for (RoomSlot slot : orderByPriority(candidates, selectedSlot, rng)) {
            GenerationResult result = tryPlace(slot, definition, world, layout, fromDoor);
            if (result != null) return result;
        }

        return placeDeadEnd(definition, world, layout, fromDoor);
    }

    public record GenerationResult(
            DungeonLayout.PlacedRoom placedRoom,
            List<DoorConnection> newPendingDoors,
            PlacementResult placementResult,
            boolean isTerminal,
            RoomSlot roomSlot
    ) {}

    // ========================================================================
    // デッドエンド / ボス配置
    // ========================================================================

    private GenerationResult placeDeadEnd(
            DungeonDefinition definition, World world, DungeonLayout layout, DoorConnection fromDoor
    ) {
        List<RoomSlot> deadEndSlots = definition.roomSlots().stream()
                .filter(slot -> slot.type() == RoomType.DEAD_END)
                .toList();

        for (RoomSlot deadEndSlot : deadEndSlots) {
            GenerationResult result = tryPlace(deadEndSlot, definition, world, layout, fromDoor);
            if (result != null) {
                for (DoorDefinition exitDoor : result.placedRoom().schematic().exitDoors()) {
                    BlockVector3 pos = result.placedRoom().origin()
                            .add(rotatePosition(exitDoor.position(), result.placedRoom().rotation()));
                    roomPlacer.sealDoor(world, pos);
                }
                return new GenerationResult(
                        result.placedRoom(), List.of(), result.placementResult(), true, deadEndSlot
                );
            }
        }

        log.warning("[DungeonGenerator] 全デッドエンド衝突、ドア封鎖: " + fromDoor.worldPosition());
        roomPlacer.sealDoor(world, fromDoor.worldPosition());
        return new GenerationResult(null, List.of(), null, true, null);
    }

    private GenerationResult placeBossRoom(
            DungeonDefinition definition, World world, DungeonLayout layout, DoorConnection fromDoor
    ) {
        RoomSchematic bossTemplate = templateRegistry.getBossTemplate(definition);
        if (bossTemplate == null) return null;

        DoorDefinition entryDoor = bossTemplate.entryDoors().isEmpty() ? null : bossTemplate.entryDoors().getFirst();
        if (entryDoor == null) return null;

        int rotation = roomPlacer.calculateRotation(fromDoor.door().direction(), entryDoor.direction());
        BlockVector3 rotatedEntryPos = rotatePosition(entryDoor.position(), rotation);
        BlockVector3 placementOrigin = connectionPoint(fromDoor).subtract(rotatedEntryPos);

        BlockVector3[] aabb = DungeonLayout.computeAABB(placementOrigin, bossTemplate, rotation);
        if (layout.hasCollision(aabb[0], aabb[1])) {
            log.fine("[DungeonGenerator] ボスルーム衝突 → スキップ");
            return null;
        }

        PlacementResult result = roomPlacer.placeRoom(bossTemplate, world, placementOrigin, rotation, entryDoor);
        for (DoorDefinition exitDoor : bossTemplate.exitDoors()) {
            roomPlacer.sealDoor(world, placementOrigin.add(rotatePosition(exitDoor.position(), rotation)));
        }
        var placed = new DungeonLayout.PlacedRoom(bossTemplate, placementOrigin, rotation, result.entryDoorWorldPos());
        layout.addPlacedRoom(placed);
        layout.addMobSpawnPositions(result.mobSpawnWorldPositions());
        layout.addBossSpawnPosition(result.bossSpawnWorldPos());
        layout.addLootPositions(result.lootWorldPositions());

        return new GenerationResult(placed, List.of(), result, true, null);
    }

    // ========================================================================
    // 候補構築
    // ========================================================================

    private List<RoomSlot> buildCandidates(DungeonDefinition definition, boolean tryBranching, boolean branchingEnabled) {
        return definition.roomSlots().stream()
                .filter(slot -> slot.isAutoSelectable(branchingEnabled))
                .filter(slot -> {
                    if (tryBranching) {
                        return slot.type() == RoomType.CORRIDOR_T || slot.type() == RoomType.CORRIDOR_CROSS;
                    } else {
                        return slot.type() != RoomType.CORRIDOR_T && slot.type() != RoomType.CORRIDOR_CROSS;
                    }
                })
                .toList();
    }

    // ========================================================================
    // 重み付き選択 + ペース制御
    // ========================================================================

    /**
     * ペース制御に基づき候補から1スロットを重み付きランダム選択します。
     * <p>
     * 連続通路数が maxCorridorsBeforeRoom を超えると部屋を強制、
     * minCorridorsBeforeRoom 未満だと通路を強制します。
     * 中間範囲では重みに応じて確率的に選択します。
     */
    private RoomSlot selectSlot(
            List<RoomSlot> candidates,
            DungeonDefinition definition,
            int consecutiveCorridors,
            boolean tryBranching,
            Random rng
    ) {
        if (candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.getFirst();

        // 分岐モードではペース制御を適用せず重み付き選択
        if (tryBranching) {
            return weightedSelect(candidates, rng);
        }

        List<RoomSlot> rooms = new ArrayList<>();
        List<RoomSlot> corridors = new ArrayList<>();
        for (RoomSlot slot : candidates) {
            if (DungeonDefinition.isCorridorType(slot.type())) {
                corridors.add(slot);
            } else {
                rooms.add(slot);
            }
        }

        // 部屋がない or 通路がない → そのまま全候補から重み付き選択
        if (rooms.isEmpty() || corridors.isEmpty()) {
            return weightedSelect(candidates, rng);
        }

        int roomWeight = totalWeight(rooms);
        int corridorWeight = totalWeight(corridors);

        // maxCorridorsBeforeRoom 超過 → 部屋を強制
        if (consecutiveCorridors >= definition.maxCorridorsBeforeRoom()) {
            if (roomWeight > 0) return weightedSelect(rooms, rng);
            return weightedSelect(corridors, rng);
        }

        // minCorridorsBeforeRoom 未満 → 通路を強制
        if (consecutiveCorridors < definition.minCorridorsBeforeRoom()) {
            if (corridorWeight > 0) return weightedSelect(corridors, rng);
            return weightedSelect(rooms, rng);
        }

        // 中間範囲 → 重みに応じて確率的選択
        int total = roomWeight + corridorWeight;
        if (total <= 0) return weightedSelect(candidates, rng);

        if (rng.nextInt(total) < roomWeight) {
            return weightedSelect(rooms, rng);
        }
        return weightedSelect(corridors, rng);
    }

    /**
     * 衝突フォールバック用の優先順位リスト。
     * 最初に選択されたスロットを先頭に、残りをランダム順で続けます。
     */
    private List<RoomSlot> orderByPriority(List<RoomSlot> candidates, RoomSlot selected, Random rng) {
        List<RoomSlot> ordered = new ArrayList<>(candidates);
        ordered.remove(selected);
        shuffleList(ordered, rng);
        ordered.addFirst(selected);
        return ordered;
    }

    /**
     * 重み付きランダム選択を1回行います。
     */
    private RoomSlot weightedSelect(List<RoomSlot> slots, Random rng) {
        if (slots.isEmpty()) return null;
        if (slots.size() == 1) return slots.getFirst();

        int total = totalWeight(slots);
        if (total <= 0) {
            return slots.get(rng.nextInt(slots.size()));
        }

        int roll = rng.nextInt(total);
        int cumulative = 0;
        for (RoomSlot slot : slots) {
            cumulative += slot.weight();
            if (roll < cumulative) return slot;
        }
        return slots.getLast();
    }

    private static int totalWeight(List<RoomSlot> slots) {
        int sum = 0;
        for (RoomSlot slot : slots) {
            sum += slot.weight();
        }
        return sum;
    }

    private static <T> void shuffleList(List<T> list, Random rng) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }

    // ========================================================================
    // 単一ルーム配置試行
    // ========================================================================

    /**
     * 1つのスロットに対して配置を試行します。
     * 衝突した場合は null を返します。
     */
    private GenerationResult tryPlace(
            RoomSlot slot,
            DungeonDefinition definition,
            World world,
            DungeonLayout layout,
            DoorConnection fromDoor
    ) {
        RoomSchematic template = templateRegistry.getTemplateForSlot(definition.id(), slot);
        if (template == null) return null;

        DoorDefinition entryDoor = template.entryDoors().isEmpty() ? null : template.entryDoors().getFirst();
        if (entryDoor == null) return null;

        int rotation = roomPlacer.calculateRotation(fromDoor.door().direction(), entryDoor.direction());
        BlockVector3 rotatedEntryPos = rotatePosition(entryDoor.position(), rotation);
        BlockVector3 placementOrigin = connectionPoint(fromDoor).subtract(rotatedEntryPos);

        BlockVector3[] aabb = DungeonLayout.computeAABB(placementOrigin, template, rotation);
        if (layout.hasCollision(aabb[0], aabb[1])) {
            return null;
        }

        PlacementResult result = roomPlacer.placeRoom(template, world, placementOrigin, rotation, entryDoor);
        var placed = new DungeonLayout.PlacedRoom(template, placementOrigin, rotation, result.entryDoorWorldPos());
        layout.addPlacedRoom(placed);
        layout.addMobSpawnPositions(result.mobSpawnWorldPositions());
        layout.addBossSpawnPosition(result.bossSpawnWorldPos());
        layout.addLootPositions(result.lootWorldPositions());

        List<DoorConnection> newPending = new ArrayList<>();
        for (DoorDefinition exitDoor : template.exitDoors()) {
            BlockVector3 doorWorldPos = placementOrigin.add(rotatePosition(exitDoor.position(), rotation));
            DoorDefinition rotatedExitDoor = new DoorDefinition(
                    exitDoor.position(),
                    exitDoor.direction().rotate(rotation),
                    exitDoor.type(),
                    exitDoor.accepts()
            );
            newPending.add(new DoorConnection(template, rotatedExitDoor, doorWorldPos, null));
        }

        return new GenerationResult(placed, newPending, result, false, slot);
    }

    // ========================================================================
    // ユーティリティ
    // ========================================================================

    public static BlockVector3 rotatePosition(BlockVector3 pos, int rotation) {
        int normalized = ((rotation % 360) + 360) % 360;
        return switch (normalized) {
            case 90 -> BlockVector3.at(-pos.z(), pos.y(), pos.x());
            case 180 -> BlockVector3.at(-pos.x(), pos.y(), -pos.z());
            case 270 -> BlockVector3.at(pos.z(), pos.y(), -pos.x());
            default -> pos;
        };
    }

    private static BlockVector3 connectionPoint(DoorConnection fromDoor) {
        return fromDoor.worldPosition().add(fromDoor.door().direction().getOffset());
    }
}
