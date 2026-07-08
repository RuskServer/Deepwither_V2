"""
Tierごとに最強装備の組み合わせTop5を算出するツール
武器＋防具4部位の全組み合わせからスコア順に表示
"""
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
from pathlib import Path
from java_parser import scan_items, scan_traders, ItemData

PROJECT_ROOT = Path(__file__).parent.parent / "src" / "main" / "java" / "com" / "ruskserver" / "deepwither_V2"

ATK_STATS = ["ATTACK_DAMAGE", "MAGIC_DAMAGE", "CRITICAL_CHANCE", "CRITICAL_DAMAGE", "ATTACK_SPEED"]
DEF_STATS = ["DEFENSE", "MAGIC_DEFENSE", "HEALTH"]

SLOT_KEYWORDS = {
    "helmet": ["helmet", "hood", "visor", "headguard"],
    "chestplate": ["chestplate", "jacket", "tunic", "upper", "plate", "suit"],
    "leggings": ["leggings", "stride", "lower", "greaves", "legmodule"],
    "boots": ["boots", "tread", "footactuator"],
}

TIER_LABELS = {
    1: ("Tier 1", "初心者向け (信用度 0-500)"),
    2: ("Tier 2", "中級者向け (信用度 500-1000)"),
    3: ("Tier 3", "上級者向け (信用度 1000+)"),
}

EXCLUDE_IDS = {"ark_eclipse"}


def build_trader_map(traders: list) -> dict:
    m = {}
    for t in traders:
        for p in t.products:
            if p.item_id not in m or p.required_reputation > m[p.item_id]["required_reputation"]:
                m[p.item_id] = {"buy_price": p.buy_price, "required_reputation": p.required_reputation, "trader_name": t.npc_name}
    return m


def classify_tier(rep: int) -> int:
    if rep <= 500:
        return 1
    elif rep <= 1000:
        return 2
    return 3


def detect_slot(item: ItemData) -> str:
    if item.weapon_type:
        return "weapon"
    iid = item.id.lower()
    for slot, keywords in SLOT_KEYWORDS.items():
        for kw in keywords:
            if kw in iid:
                return slot
    return None


def group_by_tier(items: list, trader_map: dict) -> dict:
    groups = {1: [], 2: [], 3: [], None: []}
    for item in items:
        if item.id in EXCLUDE_IDS:
            continue
        info = trader_map.get(item.id)
        if info is None:
            groups[None].append(item)
        else:
            groups[classify_tier(info["required_reputation"])].append(item)
    return groups


def categorize_slots(items: list) -> dict:
    slots = {"weapon": [], "helmet": [], "chestplate": [], "leggings": [], "boots": [], "unknown": []}
    for item in items:
        s = detect_slot(item)
        if s is None:
            slots["unknown"].append(item)
        else:
            slots[s].append(item)
    return slots


def calc_phys_dps(atk: float, aspd: float, crit_chance: float, crit_dmg: float) -> float:
    base_dps = atk * aspd
    crit_mult = 1.0 + (crit_chance / 100.0) * (crit_dmg / 100.0)
    return base_dps * crit_mult


def calc_combo_stats(items: list) -> dict:
    stats = {}
    for s in ATK_STATS + DEF_STATS:
        stats[s] = sum(i.base_stats.get(s, 0.0) for i in items)
    stats["PHYS_DPS"] = calc_phys_dps(
        stats.get("ATTACK_DAMAGE", 0),
        stats.get("ATTACK_SPEED", 1.0),
        stats.get("CRITICAL_CHANCE", 0),
        stats.get("CRITICAL_DAMAGE", 100),
    )
    stats["TOTAL_ATK"] = stats["PHYS_DPS"] + stats.get("MAGIC_DAMAGE", 0)
    stats["TOTAL_DEF"] = stats.get("DEFENSE", 0) + stats.get("MAGIC_DEFENSE", 0) + stats.get("HEALTH", 0) * 0.5
    stats["SCORE"] = stats["TOTAL_ATK"] + stats["TOTAL_DEF"]
    return stats


def fmt_name(item: ItemData, w: int = 24) -> str:
    n = item.display_name or item.id
    if len(n) > w:
        n = n[:w-1] + "…"
    return n


def print_tier_builds(tier: int, all_items: list):
    label, desc = TIER_LABELS.get(tier, ("未分類", "トレーダー未販売"))
    slots = categorize_slots(all_items)

    weapons = slots["weapon"]
    helmets = slots["helmet"]
    chestplates = slots["chestplate"]
    leggings = slots["leggings"]
    boots_list = slots["boots"]
    unknowns = slots["unknown"]

    if not weapons:
        return

    print(f"\n{'='*110}")
    print(f"  [{label}] {desc}")
    print(f"{'='*110}")
    print(f"  武器: {len(weapons)} | 兜: {len(helmets)} | 胴: {len(chestplates)} | 脚: {len(leggings)} | 靴: {len(boots_list)}")

    # create fillers for missing slots
    class DummyItem:
        def __init__(self):
            self.id = "none"
            self.display_name = "なし"
            self.base_stats = {}

    dummy = DummyItem()

    if not helmets:
        helmets = [dummy]
    if not chestplates:
        chestplates = [dummy]
    if not leggings:
        leggings = [dummy]
    if not boots_list:
        boots_list = [dummy]

    combos = []
    for w in weapons:
        for h in helmets:
            for c in chestplates:
                for l in leggings:
                    for b in boots_list:
                        combo_items = [i for i in [w, h, c, l, b] if i.id != "none"]
                        stats = calc_combo_stats(combo_items)
                        combos.append(((w, h, c, l, b), stats))

    combos.sort(key=lambda x: x[1]["SCORE"], reverse=True)

    print(f"\n  【トップビルド】(全{len(combos)}組み合わせ)")
    print()
    for rank in range(min(5, len(combos))):
        (w, h, c, l, b), stats = combos[rank]
        print(f"  ── #{rank+1} スコア: {stats['SCORE']:>8.1f} ──")
        print(f"    武器: {fmt_name(w)}")
        print(f"    兜:   {fmt_name(h)}")
        print(f"    胴:   {fmt_name(c)}")
        print(f"    脚:   {fmt_name(l)}")
        print(f"    靴:   {fmt_name(b)}")
        print(f"    ATK={stats['ATTACK_DAMAGE']:<5.0f} MATK={stats['MAGIC_DAMAGE']:<5.0f}"
              f" 会心={stats['CRITICAL_CHANCE']:<4.1f}% 会心D={stats['CRITICAL_DAMAGE']:<4.0f}%"
              f" 攻速={stats['ATTACK_SPEED']:<.2f}")
        print(f"    DEF={stats['DEFENSE']:<5.0f} MDEF={stats['MAGIC_DEFENSE']:<5.0f}"
              f" HP={stats['HEALTH']:<5.0f}")
        print(f"    物理DPS={stats['PHYS_DPS']:<7.1f}  総ATK={stats['TOTAL_ATK']:<7.1f}"
              f"  総DEF={stats['TOTAL_DEF']:<7.1f}")
        print()

    if unknowns:
        print(f"  ※ 未分類スロット: {len(unknowns)}アイテム")
        for u in unknowns:
            print(f"    {fmt_name(u, 30)} [{u.weapon_type or '?'}]")
        print()


def main():
    print("スキャン中...")
    all_items = scan_items(PROJECT_ROOT)
    traders = scan_traders(PROJECT_ROOT)
    trader_map = build_trader_map(traders)
    groups = group_by_tier(all_items, trader_map)

    excl_found = sum(1 for i in all_items if i.id in EXCLUDE_IDS)
    print(f"全アイテム: {len(all_items)}件 (除外: {excl_found}件)")
    for t in [1, 2, 3]:
        print(f"  {TIER_LABELS[t][0]}: {len(groups[t])}アイテム")
    print(f"  未分類: {len(groups[None])}アイテム")

    for t in [1, 2, 3]:
        print_tier_builds(t, groups[t])


if __name__ == "__main__":
    main()
