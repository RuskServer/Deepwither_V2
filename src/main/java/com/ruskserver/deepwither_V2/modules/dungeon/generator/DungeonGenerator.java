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
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * ダンジョンの逐次生成 + 衝突検出ロジック。
 * <p>
 * 入口ルームだけを即時配置し、以降はプレイヤーがドアに近づくたびに
 * {@link #generateNextRoom(DungeonDefinition, World, DungeonLayout, DoorConnection, int, boolean)}
 * を呼び出して1ルームずつ追加生成します。
 * <p>
 * 分岐パス（T字路・十字路）の有無は {@link DungeonDefinition#branchChance()} で制御します。
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
     * <p>
     * ダンジョン生成時に一度だけ呼び出します。
     * 残りのルームは {@link #generateNextRoom} で逐次生成します。
     *
     * @param definition ダンジョン定義
     * @param world      対象ワールド
     * @param origin     生成開始座標
     * @return エントリー配置結果（レイアウト・未接続ドア・深度）
     */
    public EntryResult generateEntry(DungeonDefinition definition, World world, BlockVector3 origin) {
        DungeonLayout layout = new DungeonLayout();
        List<DoorConnection> pendingDoors = new ArrayList<>();

        // エントリールームを配置（先頭のルームスロットをエントリーとして使用）
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

        // エントリールームの出口ドアを未接続リストに追加
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

    /**
     * エントリー配置の結果。
     *
     * @param layout       生成途中のレイアウト
     * @param pendingDoors 未接続ドア（次に生成すべきドア群）
     * @param depth        現在の深度（エントリー配置後は0）
     */
    public record EntryResult(DungeonLayout layout, List<DoorConnection> pendingDoors, int depth) {
    }

    // ========================================================================
    // 逐次生成（プレイヤーのドア接近時に1ルームずつ）
    // ========================================================================

    /**
     * 指定された未接続ドアに次のルームを1つ配置します。
     * <p>
     * 衝突検出を行い、候補が全て衝突する場合はデッドエンドで塞ぎます。
     *
     * @param definition     ダンジョン定義
     * @param world          対象ワールド
     * @param layout         現在のレイアウト（既存ルームとの衝突チェックに使用）
     * @param fromDoor       接続元の未接続ドア
     * @param currentDepth   現在の深度（エントリー後 = 0）
     * @param branchingEnabled 分岐が有効か
     * @return 生成結果。配置に失敗した場合は null
     */
    public GenerationResult generateNextRoom(
            DungeonDefinition definition,
            World world,
            DungeonLayout layout,
            DoorConnection fromDoor,
            int currentDepth,
            boolean branchingEnabled
    ) {
        // 最大深度に達したらデッドエンド
        if (currentDepth >= definition.maxDepth()) {
            return placeDeadEnd(definition, world, layout, fromDoor);
        }

        // ボスルーム判定（depth >= maxDepth/2 かつタグが合う場合）
        if (definition.hasBossRoom() && currentDepth >= Math.max(1, definition.maxDepth() / 2)) {
            List<String> bossTags = definition.roomSlots().stream()
                    .filter(slot -> slot.type() == RoomType.BOSS)
                    .findFirst()
                    .map(RoomSlot::tags)
                    .orElse(List.of());
            if (fromDoor.door().acceptsAny(bossTags)) {
                GenerationResult bossResult = placeBossRoom(definition, world, layout, fromDoor);
                if (bossResult != null) return bossResult;
            }
        }

        // 分岐判定
        boolean tryBranching = branchingEnabled
                && fromDoor.door().acceptsAny(List.of("branch", "t", "cross"));

        // 通常ルーム選択 + 衝突検出
        List<RoomSlot> candidates = buildCandidates(definition, tryBranching, branchingEnabled);
        if (candidates.isEmpty()) {
            return placeDeadEnd(definition, world, layout, fromDoor);
        }

        // 重み付きランダム選択 + 衝突フォールバック
        List<RoomSlot> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, new java.util.Random());

        for (RoomSlot slot : shuffled) {
            RoomSchematic template = templateRegistry.getTemplateForSlot(definition.id(), slot);
            if (template == null) continue;

            DoorDefinition entryDoor = template.entryDoors().isEmpty() ? null : template.entryDoors().getFirst();
            if (entryDoor == null) continue;

            int rotation = roomPlacer.calculateRotation(fromDoor.door().direction(), entryDoor.direction());
            BlockVector3 rotatedEntryPos = rotatePosition(entryDoor.position(), rotation);
            BlockVector3 placementOrigin = connectionPoint(fromDoor).subtract(rotatedEntryPos);

            // 衝突チェック
            BlockVector3[] aabb = DungeonLayout.computeAABB(placementOrigin, template, rotation);
            if (!layout.hasCollision(aabb[0], aabb[1])) {
                // 配置実行
                PlacementResult result = roomPlacer.placeRoom(template, world, placementOrigin, rotation, entryDoor);
                var placed = new DungeonLayout.PlacedRoom(template, placementOrigin, rotation, result.entryDoorWorldPos());
                layout.addPlacedRoom(placed);
                layout.addMobSpawnPositions(result.mobSpawnWorldPositions());
                layout.addBossSpawnPosition(result.bossSpawnWorldPos());
                layout.addLootPositions(result.lootWorldPositions());

                // 新しい未接続ドア
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

                return new GenerationResult(placed, newPending, result, false);
            }
        }

        // 全候補が衝突 → デッドエンド
        return placeDeadEnd(definition, world, layout, fromDoor);
    }

    /**
     * 逐次生成の結果。
     *
     * @param placedRoom      配置されたルーム
     * @param newPendingDoors 新たに発生した未接続ドア（空の場合は終端ルーム）
     * @param placementResult 配置結果（モブ・宝・ボスのスポーン情報）
     * @param isTerminal      終端ルーム（デッドエンド/ボス）で以降の接続不可
     */
    public record GenerationResult(
            DungeonLayout.PlacedRoom placedRoom,
            List<DoorConnection> newPendingDoors,
            PlacementResult placementResult,
            boolean isTerminal
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
            RoomSchematic template = templateRegistry.getTemplateForSlot(definition.id(), deadEndSlot);
            if (template == null) continue;

            DoorDefinition entryDoor = template.entryDoors().isEmpty() ? null : template.entryDoors().getFirst();
            if (entryDoor == null) continue;

            int rotation = roomPlacer.calculateRotation(fromDoor.door().direction(), entryDoor.direction());
            BlockVector3 rotatedEntryPos = rotatePosition(entryDoor.position(), rotation);
            BlockVector3 placementOrigin = connectionPoint(fromDoor).subtract(rotatedEntryPos);

            // 衝突チェック
            BlockVector3[] aabb = DungeonLayout.computeAABB(placementOrigin, template, rotation);
            if (!layout.hasCollision(aabb[0], aabb[1])) {
                PlacementResult result = roomPlacer.placeRoom(template, world, placementOrigin, rotation, entryDoor);
                for (DoorDefinition exitDoor : template.exitDoors()) {
                    roomPlacer.sealDoor(world, placementOrigin.add(rotatePosition(exitDoor.position(), rotation)));
                }
                var placed = new DungeonLayout.PlacedRoom(template, placementOrigin, rotation, result.entryDoorWorldPos());
                layout.addPlacedRoom(placed);
                layout.addMobSpawnPositions(result.mobSpawnWorldPositions());
                layout.addBossSpawnPosition(result.bossSpawnWorldPos());
                layout.addLootPositions(result.lootWorldPositions());
                return new GenerationResult(placed, List.of(), result, true);
            }
        }

        // 全デッドエンドが衝突 → ドアを封鎖
        log.warning("[DungeonGenerator] 全デッドエンド衝突、ドア封鎖: " + fromDoor.worldPosition());
        roomPlacer.sealDoor(world, fromDoor.worldPosition());
        return new GenerationResult(null, List.of(), null, true);
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

        // 衝突チェック
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

        return new GenerationResult(placed, List.of(), result, true);
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
    // ユーティリティ
    // ========================================================================

    /**
     * 位置を原点周りに回転させます（Y軸回転、90度単位）。
     */
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
