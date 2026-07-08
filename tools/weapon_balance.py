"""
Tierごとに攻撃と防御のバランスを分析するツール
攻撃Z・防御Zの偏りからキャラクターの方向性を可視化
"""
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
from pathlib import Path
from java_parser import scan_items, scan_traders, ItemData

PROJECT_ROOT = Path(__file__).parent.parent / "src" / "main" / "java" / "com" / "ruskserver" / "deepwither_V2"

ATK_STATS = ["ATTACK_DAMAGE", "MAGIC_DAMAGE", "CRITICAL_CHANCE", "CRITICAL_DAMAGE", "ATTACK_SPEED"]
DEF_STATS = ["DEFENSE", "MAGIC_DEFENSE", "HEALTH"]
ALL_STATS = ATK_STATS + DEF_STATS

TIER_LABELS = {
    1: ("Tier 1", "初心者向け (信用度 0-500)"),
    2: ("Tier 2", "中級者向け (信用度 500-1000)"),
    3: ("Tier 3", "上級者向け (信用度 1000+)"),
}


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


def group_by_tier(items: list, trader_map: dict) -> dict:
    groups = {1: [], 2: [], 3: [], None: []}
    for item in items:
        info = trader_map.get(item.id)
        if info is None:
            groups[None].append(item)
        else:
            groups[classify_tier(info["required_reputation"])].append(item)
    return groups


def has_any_stat(item: ItemData, stats_list: list) -> bool:
    return any(item.base_stats.get(s, 0.0) > 0 for s in stats_list)


def calc_stats(items: list) -> dict:
    stats = {}
    for s in ALL_STATS:
        vals = [i.base_stats.get(s, 0.0) for i in items]
        vals = [v for v in vals if v > 0]
        if not vals:
            stats[s] = {"mean": 0.0, "sd": 0.0, "n": 0}
            continue
        n = len(vals)
        mean = sum(vals) / n
        var = sum((v - mean) ** 2 for v in vals) / n
        stats[s] = {"mean": mean, "sd": var ** 0.5, "n": n}
    return stats


def calc_z_sums(item: ItemData, stats: dict) -> dict:
    result = {}
    for s in ALL_STATS:
        v = item.base_stats.get(s, 0.0)
        if v == 0 or stats[s]["n"] == 0:
            continue
        m = stats[s]["mean"]
        sd = stats[s]["sd"]
        z = (v - m) / sd if sd > 0 else 0.0
        result[s] = {"value": v, "z": z}
    return result


def fmt_name(item: ItemData, w: int = 30) -> str:
    n = item.display_name or item.id
    if len(n) > w:
        n = n[:w-1] + "…"
    return n


def print_tier(tier: int, items: list):
    label, desc = TIER_LABELS.get(tier, ("未分類", "トレーダー未販売"))
    # filter out items with no combat stats
    combat_items = [i for i in items if has_any_stat(i, ALL_STATS)]
    if not combat_items:
        return

    # split weapons vs armor
    weapons = [i for i in combat_items if i.weapon_type]
    armors = [i for i in combat_items if not i.weapon_type]

    # calculate stats separately per group so Z-scores are relative to peers
    weapon_stats = calc_stats(weapons) if weapons else {}
    armor_stats = calc_stats(armors) if armors else {}

    def print_section(title: str, section_items: list, group_stats: dict):
        if not section_items:
            return
        analyzed = []
        for item in section_items:
            zs = calc_z_sums(item, group_stats)
            atk_z = sum(d["z"] for s, d in zs.items() if s in ATK_STATS)
            def_z = sum(d["z"] for s, d in zs.items() if s in DEF_STATS)
            atk_n = sum(1 for s in ATK_STATS if s in zs)
            def_n = sum(1 for s in DEF_STATS if s in zs)
            atk_avg = atk_z / atk_n if atk_n else 0.0
            def_avg = def_z / def_n if def_n else 0.0

            if def_n == 0:
                bias, bias_v = "ATK", abs(atk_avg)
            elif atk_n == 0:
                bias, bias_v = "DEF", abs(def_avg)
            else:
                d = atk_avg - def_avg
                if d > 0.5:
                    bias, bias_v = "ATK", d
                elif d < -0.5:
                    bias, bias_v = "DEF", -d
                else:
                    bias, bias_v = "BAL", 0.0

            analyzed.append((item, zs, atk_z, def_z, bias, bias_v))

        analyzed.sort(key=lambda x: x[4] + str(x[5]), reverse=True)
        # sort: BAL last, then by bias strength
        analyzed.sort(key=lambda x: (2 if x[4] == "BAL" else 0, -x[5] if x[4] == "ATK" else x[5] if x[4] == "DEF" else 0))

        print(f"\n  【{title}】")
        print(f"  {'武器名':<30} {'種別':<8} {'偏向':>5} {'攻Z':>6} {'防Z':>6}", end="")
        for s in ATK_STATS:
            print(f" {s[:5]:>5}", end="")
        if has_any_stat(item := section_items[0], DEF_STATS):
            for s in DEF_STATS:
                print(f" {s[:5]:>5}", end="")
        print()

        print(f"  {'-'*30} {'-'*8} {'-'*5} {'-'*6} {'-'*6}", end="")
        for _ in ATK_STATS:
            print(f" {'-'*5}", end="")
        if has_any_stat(section_items[0], DEF_STATS):
            for _ in DEF_STATS:
                print(f" {'-'*5}", end="")
        print()

        for item, zs, atk_z, def_z, bias, bias_v in analyzed:
            name = fmt_name(item)
            line = f"  {name:<30} {item.weapon_type or '防具':<8} {bias:>3}{bias_v:>+5.1f} {atk_z:>+6.1f} {def_z:>+6.1f}"
            for s in ATK_STATS:
                d = zs.get(s)
                line += f" {d['value']:>5.0f}" if d else f" {'-':>5}"
            if has_any_stat(section_items[0], DEF_STATS):
                for s in DEF_STATS:
                    d = zs.get(s)
                    line += f" {d['value']:>5.0f}" if d else f" {'-':>5}"
            print(line)

    has_def_in_tier = has_any_stat(combat_items[0], DEF_STATS)

    print(f"\n{'='*100}")
    print(f"  [{label}] {desc} ({len(combat_items)}アイテム)")
    print(f"{'='*100}")

    # stats line (weapon stats / armor stats)
    if weapons:
        parts = []
        for s in ATK_STATS:
            st = weapon_stats.get(s, {})
            if st.get("n", 0) > 0:
                parts.append(f"{s[:6]} μ={st['mean']:.1f}")
        print(f"  武器基準: {'  '.join(parts)}")
    if armors:
        parts = []
        for s in DEF_STATS:
            st = armor_stats.get(s, {})
            if st.get("n", 0) > 0:
                parts.append(f"{s[:6]} μ={st['mean']:.1f}")
        print(f"  防具基準: {'  '.join(parts)}")
    print()

    print_section("武器 (攻撃主体)", weapons, weapon_stats)
    if armors:
        print_section("防具 (防御主体)", armors, armor_stats)

    print()
    print(f"  偏向: ATK=攻撃寄り  DEF=防御寄り  BAL=バランス型")
    print(f"  攻Z/防Z: 全攻撃/防御STATのZ-score合計（+が平均より高い）")
    print()


def main():
    print("スキャン中...")
    all_items = scan_items(PROJECT_ROOT)
    traders = scan_traders(PROJECT_ROOT)
    trader_map = build_trader_map(traders)
    groups = group_by_tier(all_items, trader_map)

    total = sum(1 for i in all_items if has_any_stat(i, ALL_STATS))
    print(f"全アイテム: {len(all_items)}件 (戦闘ステータス有: {total}件)")
    for t in [1, 2, 3]:
        n = sum(1 for i in groups[t] if has_any_stat(i, ALL_STATS))
        print(f"  {TIER_LABELS[t][0]}: {len(groups[t])}アイテム (戦闘有: {n})")
    n = sum(1 for i in groups.get(None, []) if has_any_stat(i, ALL_STATS))
    print(f"  未分類: {len(groups.get(None, []))}アイテム (戦闘有: {n})")

    for t in [1, 2, 3]:
        print_tier(t, groups[t])


if __name__ == "__main__":
    main()
