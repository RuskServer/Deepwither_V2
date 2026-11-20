package com.ruskserver.deepwither_V2.modules.combat.melee;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;

import java.util.concurrent.ThreadLocalRandom;

/**
 * ソウルライク／Dark and Darker 級の重厚な手応えを演出する近接戦闘専用サウンドエンジン。
 * <p>
 * 単発のバニラ SE ではなく、武器種ごとの「振りかぶり（衣擦れ・抜刀）」「振り抜き（風切り＋金属残響）」
 * 「ヒット手応え（斬撃／刺突／打撃の完全差別化）」「叩きつけ地面衝撃」「ガード」「ジャストパリィ（SEKIRO 風金属弾き）」
 * 「致命の一撃（リポスト）」を多層レイヤー＋微小ピッチ揺らぎで再生する。
 */
public final class MeleeSoundService {

    private MeleeSoundService() {
    }

    private static float jitter(float base, float spread) {
        return Math.max(0.5f, Math.min(2.0f, base + (float) ((ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * spread)));
    }

    /**
     * 振りかぶり開始時の予備動作音（武器重量で衣擦れ・金属音が変わる）。
     */
    public static void playWindup(Location loc, MeleeWeaponArchetype archetype) {
        World world = loc.getWorld();
        if (world == null) return;
        switch (archetype) {
            case GREATSWORD, HAMMER -> {
                world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 0.55f, jitter(0.62f, 0.05f));
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.40f, jitter(0.58f, 0.04f));
            }
            case AXE -> {
                world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_IRON, 0.48f, jitter(0.75f, 0.05f));
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.45f, jitter(0.72f, 0.04f));
            }
            case SCYTHE, SPEAR -> {
                world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.55f, jitter(0.92f, 0.06f));
                world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_CHAIN, 0.30f, jitter(1.15f, 0.06f));
            }
            case DAGGER -> {
                world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_CHAIN, 0.42f, jitter(1.75f, 0.08f));
            }
            case SWORD -> {
                world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_CHAIN, 0.38f, jitter(1.35f, 0.06f));
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.35f, jitter(1.18f, 0.05f));
            }
        }
    }

    /**
     * 振り抜き／突き開始時のメインスイング音（武器種別＋コンボ段数ピッチ上昇＋空気裂きレイヤー）。
     */
    public static void playSwing(Location loc, MeleeWeaponArchetype archetype, int stepIndex, boolean thrust) {
        World world = loc.getWorld();
        if (world == null) return;
        float comboLift = stepIndex * 0.07f;

        if (thrust) {
            world.playSound(loc, Sound.ITEM_TRIDENT_THROW, 0.90f, jitter(1.22f + comboLift, 0.05f));
            world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.65f, jitter(1.52f + comboLift, 0.06f));
            world.playSound(loc, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.38f, jitter(1.85f, 0.08f));
            return;
        }

        switch (archetype) {
            case SWORD -> {
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.90f, jitter(1.14f + comboLift, 0.05f));
                world.playSound(loc, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.34f, jitter(1.70f + comboLift, 0.07f));
                world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.18f, jitter(1.92f, 0.06f));
            }
            case GREATSWORD -> {
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.00f, jitter(0.56f + comboLift * 0.5f, 0.04f));
                world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.55f, jitter(0.70f, 0.04f));
                world.playSound(loc, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.45f, jitter(0.82f, 0.05f));
            }
            case AXE -> {
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.95f, jitter(0.70f + comboLift, 0.05f));
                world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.45f, jitter(0.92f, 0.05f));
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.38f, jitter(0.78f, 0.05f));
            }
            case SPEAR -> {
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.92f, jitter(0.95f + comboLift, 0.05f));
                world.playSound(loc, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.42f, jitter(1.38f, 0.06f));
            }
            case DAGGER -> {
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.78f, jitter(1.68f + comboLift, 0.07f));
                world.playSound(loc, Sound.ENTITY_PHANTOM_FLAP, 0.45f, jitter(1.90f, 0.06f));
            }
            case HAMMER -> {
                world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.65f, jitter(0.56f, 0.04f));
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.75f, jitter(0.62f, 0.04f));
            }
            case SCYTHE -> {
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.95f, jitter(0.80f + comboLift, 0.05f));
                world.playSound(loc, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.46f, jitter(1.12f, 0.06f));
                world.playSound(loc, Sound.BLOCK_SOUL_SAND_BREAK, 0.35f, jitter(0.75f, 0.06f));
            }
        }
    }

    /**
     * 攻撃ヒット時の重厚な手応え音（斬撃／刺突／打撃で完全に音色を分離）。
     */
    public static void playHit(Location hitLoc, MeleeWeaponArchetype archetype, boolean thrust, boolean isRiposte) {
        World world = hitLoc.getWorld();
        if (world == null) return;

        if (isRiposte) {
            world.playSound(hitLoc, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 0.90f, jitter(1.12f, 0.04f));
            world.playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.00f, jitter(0.82f, 0.04f));
            world.playSound(hitLoc, Sound.BLOCK_BELL_RESONATE, 0.48f, jitter(1.55f, 0.05f));
            return;
        }

        if (thrust) {
            world.playSound(hitLoc, Sound.ITEM_TRIDENT_HIT, 0.95f, jitter(1.18f, 0.06f));
            world.playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.75f, jitter(1.02f, 0.05f));
            world.playSound(hitLoc, Sound.ENTITY_ARROW_HIT, 0.45f, jitter(1.38f, 0.06f));
            return;
        }

        switch (archetype) {
            case HAMMER -> {
                world.playSound(hitLoc, Sound.ITEM_MACE_SMASH_GROUND, 0.85f, jitter(0.78f, 0.05f));
                world.playSound(hitLoc, Sound.ENTITY_IRON_GOLEM_DAMAGE, 0.70f, jitter(0.68f, 0.05f));
                world.playSound(hitLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.42f, jitter(0.58f, 0.04f));
            }
            case GREATSWORD, AXE -> {
                world.playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.00f, jitter(0.74f, 0.05f));
                world.playSound(hitLoc, Sound.ITEM_TRIDENT_HIT, 0.65f, jitter(0.88f, 0.05f));
                world.playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.60f, jitter(0.86f, 0.05f));
            }
            case DAGGER -> {
                world.playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.78f, jitter(1.48f, 0.07f));
                world.playSound(hitLoc, Sound.ITEM_TRIDENT_HIT, 0.55f, jitter(1.65f, 0.07f));
            }
            default -> { // SWORD, SPEAR sweep, SCYTHE
                world.playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.92f, jitter(1.04f, 0.05f));
                world.playSound(hitLoc, Sound.ITEM_TRIDENT_HIT, 0.60f, jitter(1.28f, 0.06f));
                world.playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.48f, jitter(1.18f, 0.06f));
            }
        }
    }

    /**
     * 頭上叩きつけ（Slam）の地面着弾衝撃音。
     */
    public static void playGroundSlam(Location slamLoc) {
        World world = slamLoc.getWorld();
        if (world == null) return;
        world.playSound(slamLoc, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.00f, jitter(0.84f, 0.05f));
        world.playSound(slamLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.42f, jitter(0.72f, 0.05f));
        world.playSound(slamLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.85f, jitter(0.65f, 0.06f));
    }

    /**
     * ガード構え開始音。
     */
    public static void playGuardRaise(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_IRON, 0.70f, jitter(1.42f, 0.05f));
        world.playSound(loc, Sound.BLOCK_CHAIN_PLACE, 0.45f, jitter(1.32f, 0.06f));
    }

    /**
     * 通常ガード受け止め音（重厚な鉄と鉄の激突）。
     */
    public static void playGuardBlock(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.00f, jitter(0.92f, 0.05f));
        world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.45f, jitter(1.25f, 0.05f));
        world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 0.35f, jitter(1.35f, 0.06f));
    }

    /**
     * ジャストパリィ成功音（澄んだ超高音金属弾き＋鐘残響）。
     */
    public static void playJustParry(Location clashLoc) {
        World world = clashLoc.getWorld();
        if (world == null) return;
        world.playSound(clashLoc, Sound.BLOCK_ANVIL_PLACE, 0.78f, jitter(1.92f, 0.04f));
        world.playSound(clashLoc, Sound.BLOCK_BELL_USE, 0.58f, jitter(1.85f, 0.04f));
        world.playSound(clashLoc, Sound.ITEM_TRIDENT_RETURN, 0.85f, jitter(1.60f, 0.05f));
        world.playSound(clashLoc, Sound.ITEM_SHIELD_BLOCK, 0.90f, jitter(1.42f, 0.05f));
    }

    /**
     * パリィで弾かれた攻撃側のよろけ金属音。
     */
    public static void playParriedRecoil(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        world.playSound(loc, Sound.ITEM_SHIELD_BREAK, 0.85f, jitter(0.80f, 0.05f));
        world.playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.48f, jitter(0.70f, 0.04f));
    }
}
