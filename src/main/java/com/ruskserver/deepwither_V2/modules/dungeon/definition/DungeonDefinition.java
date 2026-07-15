package com.ruskserver.deepwither_V2.modules.dungeon.definition;

import com.ruskserver.deepwither_V2.modules.dungeon.room.RoomType;

import java.util.Collections;
import java.util.List;

/**
 * ダンジョンの定義クラス。
 * <p>
 * 各ダンジョンタイプはこのクラスを継承し、コンストラクタでパラメータを渡します。
 * Javaハードコーディングにより定義し、スケマティックは各ダンジョンの専用フォルダから読みます。
 * <p>
 * 分岐パス（T字路・十字路）の有無は {@code branchChance} で制御します。
 * 0.0 を指定すると一本道のダンジョンになり、正の値を指定すると分岐探索型になります。
 *
 * @param id                ダンジョン識別子（一意。例: "simple_dungeon"）
 * @param displayName       表示名（例: "Simple Dungeon"）
 * @param schematicFolder   スケマティック格納フォルダ名（plugins/Deepwither_V2/dungeon/ 配下）
 * @param maxDepth          最大深度（配置するルームの上限）
 * @param timeLimitMinutes  制限時間（分）
 * @param lives             パーティ共通ライフ数
 * @param branchChance      分岐確率（0.0 で分岐なし、0.0〜1.0）
 * @param maxBranches       同時未接続ドア数の上限（分岐の広がりを制御）
 * @param roomSlots         使用するルームスロットのリスト
 * @param bossRoomSchematic  ボスルームのスケマティック名（空文字の場合、ボスルームなし）
 * @param seed              乱数シード（0の場合はランダムシードを使用）
 * @param minCorridorsBeforeRoom  部屋の間に最低限配置する通路の数
 * @param maxCorridorsBeforeRoom  部屋の前に最大で配置できる通路の数（超えると強制部屋）
 */
public class DungeonDefinition {

    private final String id;
    private final String displayName;
    private final String schematicFolder;
    private final int maxDepth;
    private final int timeLimitMinutes;
    private final int lives;
    private final double branchChance;
    private final int maxBranches;
    private final List<RoomSlot> roomSlots;
    private final String bossRoomSchematic;
    private final String lootTableId;
    private final String mobId;
    private final String bossMobId;
    private final long seed;
    private final int minCorridorsBeforeRoom;
    private final int maxCorridorsBeforeRoom;

    protected DungeonDefinition(
            String id,
            String displayName,
            String schematicFolder,
            int maxDepth,
            int timeLimitMinutes,
            int lives,
            double branchChance,
            int maxBranches,
            List<RoomSlot> roomSlots,
            String bossRoomSchematic
    ) {
        this(
                id,
                displayName,
                schematicFolder,
                maxDepth,
                timeLimitMinutes,
                lives,
                branchChance,
                maxBranches,
                roomSlots,
                bossRoomSchematic,
                "ghoul_nest",
                "ghoul",
                null,
                0L,
                1,
                3
        );
    }

    protected DungeonDefinition(
            String id,
            String displayName,
            String schematicFolder,
            int maxDepth,
            int timeLimitMinutes,
            int lives,
            double branchChance,
            int maxBranches,
            List<RoomSlot> roomSlots,
            String bossRoomSchematic,
            String lootTableId,
            String mobId
    ) {
        this(
                id,
                displayName,
                schematicFolder,
                maxDepth,
                timeLimitMinutes,
                lives,
                branchChance,
                maxBranches,
                roomSlots,
                bossRoomSchematic,
                lootTableId,
                mobId,
                null,
                0L,
                1,
                3
        );
    }

    protected DungeonDefinition(
            String id,
            String displayName,
            String schematicFolder,
            int maxDepth,
            int timeLimitMinutes,
            int lives,
            double branchChance,
            int maxBranches,
            List<RoomSlot> roomSlots,
            String bossRoomSchematic,
            String lootTableId,
            String mobId,
            long seed,
            int minCorridorsBeforeRoom,
            int maxCorridorsBeforeRoom
    ) {
        this(
                id,
                displayName,
                schematicFolder,
                maxDepth,
                timeLimitMinutes,
                lives,
                branchChance,
                maxBranches,
                roomSlots,
                bossRoomSchematic,
                lootTableId,
                mobId,
                null,
                seed,
                minCorridorsBeforeRoom,
                maxCorridorsBeforeRoom
        );
    }

    protected DungeonDefinition(
            String id,
            String displayName,
            String schematicFolder,
            int maxDepth,
            int timeLimitMinutes,
            int lives,
            double branchChance,
            int maxBranches,
            List<RoomSlot> roomSlots,
            String bossRoomSchematic,
            String lootTableId,
            String mobId,
            String bossMobId,
            long seed,
            int minCorridorsBeforeRoom,
            int maxCorridorsBeforeRoom
    ) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id は空にできません");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName は空にできません");
        if (schematicFolder == null || schematicFolder.isBlank()) throw new IllegalArgumentException("schematicFolder は空にできません");
        if (maxDepth <= 0) throw new IllegalArgumentException("maxDepth は1以上でなければなりません");
        if (timeLimitMinutes <= 0) throw new IllegalArgumentException("timeLimitMinutes は1以上でなければなりません");
        if (lives <= 0) throw new IllegalArgumentException("lives は1以上でなければなりません");
        if (branchChance < 0.0 || branchChance > 1.0) throw new IllegalArgumentException("branchChance は0.0〜1.0の範囲でなければなりません");
        if (maxBranches < 0) throw new IllegalArgumentException("maxBranches は0以上でなければなりません");
        if (roomSlots == null) throw new IllegalArgumentException("roomSlots は null にできません");
        if (minCorridorsBeforeRoom < 0) throw new IllegalArgumentException("minCorridorsBeforeRoom は0以上でなければなりません");
        if (maxCorridorsBeforeRoom < minCorridorsBeforeRoom) throw new IllegalArgumentException("maxCorridorsBeforeRoom は minCorridorsBeforeRoom 以上でなければなりません");

        this.id = id;
        this.displayName = displayName;
        this.schematicFolder = schematicFolder;
        this.maxDepth = maxDepth;
        this.timeLimitMinutes = timeLimitMinutes;
        this.lives = lives;
        this.branchChance = branchChance;
        this.maxBranches = maxBranches;
        this.roomSlots = List.copyOf(roomSlots);
        this.bossRoomSchematic = bossRoomSchematic != null ? bossRoomSchematic : "";
        this.lootTableId = lootTableId != null && !lootTableId.isBlank() ? lootTableId : "ghoul_nest";
        this.mobId = mobId != null && !mobId.isBlank() ? mobId : "ghoul";
        this.bossMobId = bossMobId != null && !bossMobId.isBlank() ? bossMobId : null;
        this.seed = seed;
        this.minCorridorsBeforeRoom = minCorridorsBeforeRoom;
        this.maxCorridorsBeforeRoom = maxCorridorsBeforeRoom;
    }

    // --- Getters ---

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String schematicFolder() { return schematicFolder; }
    public int maxDepth() { return maxDepth; }
    public int timeLimitMinutes() { return timeLimitMinutes; }
    public int lives() { return lives; }
    public double branchChance() { return branchChance; }
    public int maxBranches() { return maxBranches; }
    public List<RoomSlot> roomSlots() { return roomSlots; }
    public String bossRoomSchematic() { return bossRoomSchematic; }
    public String lootTableId() { return lootTableId; }
    public String mobId() { return mobId; }
    public String bossMobId() { return bossMobId; }
    public long seed() { return seed; }
    public int minCorridorsBeforeRoom() { return minCorridorsBeforeRoom; }
    public int maxCorridorsBeforeRoom() { return maxCorridorsBeforeRoom; }

    /**
     * 分岐パスが有効か（branchChance > 0 かつ分岐ルームが1つ以上定義されている）を判定します。
     */
    public boolean isBranchingEnabled() {
        if (branchChance <= 0.0) return false;
        return roomSlots.stream().anyMatch(slot ->
                slot.type() == RoomType.CORRIDOR_T || slot.type() == RoomType.CORRIDOR_CROSS
        );
    }

    /**
     * ボスルームが定義されているかを判定します。
     */
    public boolean hasBossRoom() {
        return bossRoomSchematic != null && !bossRoomSchematic.isBlank();
    }

    /**
     * 指定タグを持つルームスロットの合計重みを返します。
     */
    public int getTotalWeight(boolean branchingEnabled) {
        return roomSlots.stream()
                .filter(slot -> slot.isAutoSelectable(branchingEnabled))
                .mapToInt(RoomSlot::weight)
                .sum();
    }

    /**
     * 指定タグと接続可能なルームスロットを返します（読み取り専用）。
     */
    public List<RoomSlot> getAcceptableSlots(List<String> acceptTags, boolean branchingEnabled) {
        return Collections.unmodifiableList(
                roomSlots.stream()
                        .filter(slot -> slot.isAutoSelectable(branchingEnabled))
                        .filter(slot -> acceptTags.stream().anyMatch(slot.tags()::contains))
                        .toList()
        );
    }

    /**
     * 指定のルーム種別が通路系かを判定します。
     */
    public static boolean isCorridorType(RoomType type) {
        return type == RoomType.CORRIDOR_STRAIGHT
                || type == RoomType.CORRIDOR_TURN
                || type == RoomType.CORRIDOR_T
                || type == RoomType.CORRIDOR_CROSS;
    }

    @Override
    public String toString() {
        return "DungeonDefinition{" + id + " (" + displayName + "), depth=" + maxDepth
                + ", lives=" + lives + ", branch=" + branchChance + "}";
    }
}
