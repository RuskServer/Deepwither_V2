package com.ruskserver.deepwither_V2.modules.dungeon.generator;

import com.ruskserver.deepwither_V2.modules.dungeon.schematic.RoomSchematic;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ダンジョン生成の結果を保持するクラス。
 * <p>
 * 配置されたルーム、ドア、モブスポーン位置を追跡し、ダンジョンインスタンスの状態管理に使用します。
 */
public class DungeonLayout {

    private final List<PlacedRoom> placedRooms = new ArrayList<>();
    private final List<DoorConnection> unconnectedDoors = new ArrayList<>();
    private final List<BlockVector3> allMobSpawnPositions = new ArrayList<>();
    private final List<BlockVector3> bossSpawnPositions = new ArrayList<>();
    private final List<BlockVector3> lootPositions = new ArrayList<>();

    /**
     * ルームを配置済みリストに追加します。
     */
    public void addPlacedRoom(PlacedRoom room) {
        placedRooms.add(room);
    }

    /**
     * モブスポーン位置を追加します。
     */
    public void addMobSpawnPositions(List<BlockVector3> positions) {
        allMobSpawnPositions.addAll(positions);
    }

    /**
     * ボスポーン位置を追加します。
     */
    public void addBossSpawnPosition(BlockVector3 position) {
        if (position != null) {
            bossSpawnPositions.add(position);
        }
    }

    /**
     * 報酬位置を追加します。
     */
    public void addLootPositions(List<BlockVector3> positions) {
        lootPositions.addAll(positions);
    }

    /**
     * 未接続ドアを追加します。
     */
    public void addUnconnectedDoor(DoorConnection door) {
        unconnectedDoors.add(door);
    }

    /**
     * 指定ドアを未接続リストから削除します。
     */
    public void removeUnconnectedDoor(DoorConnection door) {
        unconnectedDoors.remove(door);
    }

    /**
     * 全配置済みルームを返します（読み取り専用）。
     */
    public List<PlacedRoom> getPlacedRooms() {
        return Collections.unmodifiableList(placedRooms);
    }

    /**
     * 未接続ドアのリストを返します（読み取り専用）。
     */
    public List<DoorConnection> getUnconnectedDoors() {
        return Collections.unmodifiableList(unconnectedDoors);
    }

    /**
     * 全モブスポーン位置を返します（読み取り専用）。
     */
    public List<BlockVector3> getAllMobSpawnPositions() {
        return Collections.unmodifiableList(allMobSpawnPositions);
    }

    /**
     * ボスポーン位置を返します（読み取り専用）。
     */
    public List<BlockVector3> getBossSpawnPositions() {
        return Collections.unmodifiableList(bossSpawnPositions);
    }

    /**
     * 報酬位置を返します（読み取り専用）。
     */
    public List<BlockVector3> getLootPositions() {
        return Collections.unmodifiableList(lootPositions);
    }

    /**
     * 未接続ドア数を返します。
     */
    public int getUnconnectedDoorCount() {
        return unconnectedDoors.size();
    }

    /**
     * 配置済みルーム数を返します。
     */
    public int getPlacedRoomCount() {
        return placedRooms.size();
    }

    /**
     * 配置済みルームのワールド空間AABBと、新規ルームのAABBが衝突するかを判定します。
     *
     * @param newWorldMin 新規ルームの最小座標（ワールド座標）
     * @param newWorldMax 新規ルームの最大座標（ワールド座標）
     * @return 衝突する場合 true
     */
    public boolean hasCollision(BlockVector3 newWorldMin, BlockVector3 newWorldMax) {
        for (PlacedRoom room : placedRooms) {
            if (intersectsAABB(newWorldMin, newWorldMax, room.worldMin(), room.worldMax())) {
                return true;
            }
        }
        return false;
    }

    private static boolean intersectsAABB(
            BlockVector3 min1, BlockVector3 max1,
            BlockVector3 min2, BlockVector3 max2
    ) {
        return min1.x() <= max2.x() && max1.x() >= min2.x()
                && min1.y() <= max2.y() && max1.y() >= min2.y()
                && min1.z() <= max2.z() && max1.z() >= min2.z();
    }

    /**
     * 配置原点とテンプレートと回転からワールド空間のAABB（最小・最大座標）を計算します。
     */
    public static BlockVector3[] computeAABB(
            BlockVector3 origin, RoomSchematic schematic, int rotation
    ) {
        BlockVector3 size = schematic.size();
        BlockVector3 maxLocal = BlockVector3.at(size.x() - 1, size.y() - 1, size.z() - 1);

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        int[] xs = {0, maxLocal.x()};
        int[] ys = {0, maxLocal.y()};
        int[] zs = {0, maxLocal.z()};

        for (int x : xs) {
            for (int y : ys) {
                for (int z : zs) {
                    BlockVector3 rotated = DungeonGenerator.rotatePosition(BlockVector3.at(x, y, z), rotation);
                    BlockVector3 worldPos = origin.add(rotated);
                    if (worldPos.x() < minX) minX = worldPos.x();
                    if (worldPos.y() < minY) minY = worldPos.y();
                    if (worldPos.z() < minZ) minZ = worldPos.z();
                    if (worldPos.x() > maxX) maxX = worldPos.x();
                    if (worldPos.y() > maxY) maxY = worldPos.y();
                    if (worldPos.z() > maxZ) maxZ = worldPos.z();
                }
            }
        }

        return new BlockVector3[]{
                BlockVector3.at(minX, minY, minZ),
                BlockVector3.at(maxX, maxY, maxZ)
        };
    }

    /**
     * 配置済みルームを表すレコード。
     * <p>
     * 配置時の回転を考慮したワールド空間のAABB（worldMin / worldMax）も保持します。
     *
     * @param schematic         配置されたルームスケマティック
     * @param origin            配置原点（ワールド座標）
     * @param rotation          回転角度
     * @param entryDoorWorldPos 入口ドアのワールド座標
     * @param worldMin          ワールド空間AABB最小座標
     * @param worldMax          ワールド空間AABB最大座標
     */
    public record PlacedRoom(
            RoomSchematic schematic,
            BlockVector3 origin,
            int rotation,
            BlockVector3 entryDoorWorldPos,
            BlockVector3 worldMin,
            BlockVector3 worldMax
    ) {
        public PlacedRoom(RoomSchematic schematic, BlockVector3 origin, int rotation, BlockVector3 entryDoorWorldPos) {
            BlockVector3[] aabb = DungeonLayout.computeAABB(origin, schematic, rotation);
            this(schematic, origin, rotation, entryDoorWorldPos, aabb[0], aabb[1]);
        }
    }
}
