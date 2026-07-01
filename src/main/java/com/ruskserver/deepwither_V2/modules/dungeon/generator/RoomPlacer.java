package com.ruskserver.deepwither_V2.modules.dungeon.generator;

import com.ruskserver.deepwither_V2.modules.dungeon.room.DoorDefinition;
import com.ruskserver.deepwither_V2.modules.dungeon.room.DoorDirection;
import com.ruskserver.deepwither_V2.modules.dungeon.room.DoorType;
import com.ruskserver.deepwither_V2.modules.dungeon.schematic.RoomSchematic;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * ルームスケマティックをワールドに配置するサービス。
 * <p>
 * ドアの位置合わせ、回転、WorldEdit によるブロック配置を行います。
 * 配置後、モブスポーン位置や報酬位置のワールド座標を {@link PlacementResult} で返します。
 */
public class RoomPlacer {

    private final Logger log;

    public RoomPlacer(Logger log) {
        this.log = log;
    }

    /**
     * ルームを配置し、配置結果を返します。
     *
     * @param schematic        配置するルームスケマティック
     * @param world            対象ワールド
     * @param placementOrigin  配置原点（ワールド座標）
     * @param rotation         回転角度（0, 90, 180, 270）
     * @param entryDoor        使用する入口ドア
     * @return 配置結果（入口ドア位置、モブスポーン位置、報酬位置、ボスポーン位置）
     */
    public PlacementResult placeRoom(
            RoomSchematic schematic,
            World world,
            BlockVector3 placementOrigin,
            int rotation,
            DoorDefinition entryDoor
    ) {
        BlockArrayClipboard clipboard = schematic.clipboard();

        try (EditSession editSession = WorldEdit.getInstance()
                .newEditSessionBuilder()
                .world(BukkitAdapter.adapt(world))
                .build()) {

            AffineTransform transform = new AffineTransform();
            if (rotation != 0) {
                transform = transform.rotateY(-rotation);
            }

            ClipboardHolder holder = new ClipboardHolder(clipboard);
            holder.setTransform(transform);

            Operations.complete(holder.createPaste(editSession)
                    .to(placementOrigin)
                    .ignoreAirBlocks(false)
                    .build());

            log.fine("[RoomPlacer] 配置完了: " + schematic.schematicId()
                    + " at (" + placementOrigin.x() + ", " + placementOrigin.y() + ", " + placementOrigin.z() + ")"
                    + " rotation=" + rotation);

        } catch (Exception e) {
            log.severe("[RoomPlacer] ルーム配置失敗: " + schematic.schematicId() + " - " + e.getMessage());
        }

        // 配置された入口ドアのワールド座標を計算
        BlockVector3 doorWorldPos = placementOrigin;
        placeDoors(schematic, world, placementOrigin, rotation, entryDoor);

        if (entryDoor != null) {
            doorWorldPos = placementOrigin.add(rotatePosition(entryDoor.position(), rotation));
        }

        // モブスポーン位置を回転変換してワールド座標に
        List<BlockVector3> mobSpawnWorld = new ArrayList<>();
        for (BlockVector3 localPos : schematic.mobSpawns()) {
            mobSpawnWorld.add(placementOrigin.add(rotatePosition(localPos, rotation)));
        }

        // 報酬位置を回転変換してワールド座標に
        List<BlockVector3> lootWorld = new ArrayList<>();
        for (BlockVector3 localPos : schematic.lootPositions()) {
            lootWorld.add(placementOrigin.add(rotatePosition(localPos, rotation)));
        }

        // ボスポーン位置を回転変換してワールド座標に
        BlockVector3 bossWorld = null;
        if (schematic.bossSpawn() != null) {
            bossWorld = placementOrigin.add(rotatePosition(schematic.bossSpawn(), rotation));
        }

        return new PlacementResult(doorWorldPos, mobSpawnWorld, lootWorld, bossWorld);
    }

    /**
     * 指定入口ドアと接続可能な回転角度を計算します。
     *
     * @param sourceDoorDirection 接続元ドアの開口方向
     * @param entryDoorDirection 選択ルームの入口ドアの開口方向
     * @return 必要な回転角度（0, 90, 180, 270）
     */
    public int calculateRotation(DoorDirection sourceDoorDirection, DoorDirection entryDoorDirection) {
        DoorDirection expected = sourceDoorDirection.opposite();
        return rotationFromTo(entryDoorDirection, expected);
    }

    /**
     * from 方向を to 方向に向けるために必要な回転角度を返します。
     */
    private int rotationFromTo(DoorDirection from, DoorDirection to) {
        return switch (from) {
            case NORTH -> switch (to) {
                case NORTH -> 0;
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
            };
            case EAST -> switch (to) {
                case NORTH -> 270;
                case EAST -> 0;
                case SOUTH -> 90;
                case WEST -> 180;
            };
            case SOUTH -> switch (to) {
                case NORTH -> 180;
                case EAST -> 270;
                case SOUTH -> 0;
                case WEST -> 90;
            };
            case WEST -> switch (to) {
                case NORTH -> 90;
                case EAST -> 180;
                case SOUTH -> 270;
                case WEST -> 0;
            };
        };
    }

    /**
     * 位置を原点周りに回転させます（Y軸回転、90度単位）。
     */
    private BlockVector3 rotatePosition(BlockVector3 pos, int rotation) {
        int normalized = ((rotation % 360) + 360) % 360;
        return switch (normalized) {
            case 90 -> BlockVector3.at(-pos.z(), pos.y(), pos.x());
            case 180 -> BlockVector3.at(-pos.x(), pos.y(), -pos.z());
            case 270 -> BlockVector3.at(pos.z(), pos.y(), -pos.x());
            default -> pos;
        };
    }

    private void placeDoors(RoomSchematic schematic, World world, BlockVector3 placementOrigin, int rotation, DoorDefinition connectedEntryDoor) {
        for (DoorDefinition door : schematic.allDoors()) {
            if (door.equals(connectedEntryDoor)) {
                continue;
            }
            BlockVector3 worldPos = placementOrigin.add(rotatePosition(door.position(), rotation));
            Block lowerBlock = world.getBlockAt(worldPos.x(), worldPos.y(), worldPos.z());
            Block upperBlock = world.getBlockAt(worldPos.x(), worldPos.y() + 1, worldPos.z());

            DoorDirection facing = door.direction().rotate(rotation);
            lowerBlock.setType(Material.OAK_DOOR, false);
            upperBlock.setType(Material.OAK_DOOR, false);
            lowerBlock.setBlockData(createDoorData(facing, Bisected.Half.BOTTOM), false);
            upperBlock.setBlockData(createDoorData(facing, Bisected.Half.TOP), false);
        }
    }

    private BlockData createDoorData(DoorDirection direction, Bisected.Half half) {
        Door data = (Door) Material.OAK_DOOR.createBlockData();
        data.setFacing(toBlockFace(direction));
        data.setHalf(half);
        return data;
    }

    public void sealDoor(World world, BlockVector3 doorPos) {
        Material fill = detectFillMaterial(world, doorPos);
        world.getBlockAt(doorPos.x(), doorPos.y(), doorPos.z()).setType(fill, false);
        world.getBlockAt(doorPos.x(), doorPos.y() + 1, doorPos.z()).setType(fill, false);
    }

    private Material detectFillMaterial(World world, BlockVector3 doorPos) {
        for (BlockFace face : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.DOWN, BlockFace.UP)) {
            Block block = world.getBlockAt(doorPos.x(), doorPos.y(), doorPos.z()).getRelative(face);
            Material material = block.getType();
            if (material.isSolid() && material != Material.OAK_DOOR && material != Material.IRON_DOOR) {
                return material;
            }
        }
        return Material.STONE;
    }

    private BlockFace toBlockFace(DoorDirection direction) {
        return switch (direction) {
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case EAST -> BlockFace.EAST;
            case WEST -> BlockFace.WEST;
        };
    }
}
