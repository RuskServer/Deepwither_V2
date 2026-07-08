"""
Tierごとに平均からの乖離が大きい武器を検出するツール
"""
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
from pathlib import Path
from java_parser import scan_items, scan_traders, ItemData

PROJECT_ROOT = Path(__file__).parent.parent / "src" / "main" / "java" / "com" / "ruskserver" / "deepwither_V2"
TARGET_STATS = ["ATTACK_DAMAGE", "MAGIC_DAMAGE", "CRITICAL_CHANCE", "CRITICAL_DAMAGE", "ATTACK_SPEED"]

TIER_LABELS = {
    1: ("Tier 1", "初心者向け (信用度 0-500)"),
    2: ("Tier 2", "中級者向け (信用度 500-1000)"),
    3: ("Tier 3", "上級者向け (信用度 1000+)"),
}


def get_weapon_items(items: list) -> list:
    return [item for item in items if item.weapon_type is not None]


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


def group_by_tier(weapons: list, trader_map: dict) -> dict:
    groups = {1: [], 2: [], 3: [], None: []}
    for w in weapons:
        info = trader_map.get(w.id)
        if info is None:
            groups[None].append(w)
        else:
            t = classify_tier(info["required_reputation"])
            groups[t].append(w)
    return groups


def calc_stats(weapons: list) -> dict:
    stats = {}
    for s in TARGET_STATS:
        vals = [w.base_stats.get(s, 0.0) for w in weapons]
        vals = [v for v in vals if v > 0]
        if not vals:
            stats[s] = {"mean": 0.0, "sd": 0.0, "n": 0, "min": 0, "max": 0}
            continue
        n = len(vals)
        mean = sum(vals) / n
        var = sum((v - mean) ** 2 for v in vals) / n
        stats[s] = {"mean": mean, "sd": var ** 0.5, "n": n, "min": min(vals), "max": max(vals)}
    return stats


def analyze(weapons: list, stats: dict) -> list:
    results = []
    for w in weapons:
        total = 0.0
        details = {}
        for s in TARGET_STATS:
            v = w.base_stats.get(s, 0.0)
            if v == 0:
                continue
            m = stats[s]["mean"]
            sd = stats[s]["sd"]
            z = (v - m) / sd if sd > 0 else 0.0
            total += abs(z)
            details[s] = {"value": v, "mean": m, "z": z}
        results.append({"item": w, "details": details, "total_dev": total})
    return results


def fmt_name(item: ItemData, w: int = 30) -> str:
    n = item.display_name or item.id
    if len(n) > w:
        n = n[:w-1] + "…"
    return n


def print_tier(tier: int, weapons: list):
    label, desc = TIER_LABELS.get(tier, ("未分類", "トレーダー未販売"))
    n = len(weapons)
    if n == 0:
        return

    stats = calc_stats(weapons)
    analyzed = analyze(weapons, stats)
    analyzed.sort(key=lambda x: x["total_dev"], reverse=True)

    print(f"\n{'='*96}")
    print(f"  [{label}] {desc} ({n}武器)")
    print(f"{'='*96}")

    # stats line
    parts = []
    for s in TARGET_STATS:
        st = stats[s]
        if st["n"] > 0:
            parts.append(f"{s[:6]} avg={st['mean']:.1f} sd={st['sd']:.1f}")
    print(f"  {' | '.join(parts)}")
    print()

    # outlier table
    outliers = [r for r in analyzed if r["total_dev"] > 1.5]
    if not outliers:
        print("  (平均から大きく乖離した武器なし)")
        print()
        return

    print(f"  {'武器名':<30} {'種別':<8} {'乖離':>5}", end="")
    for s in TARGET_STATS:
        print(f" {s[:7]:>7}", end="")
    print(f"  内訳(Z-score)")
    print(f"  {'-'*30} {'-'*8} {'-'*5}", end="")
    for _ in TARGET_STATS:
        print(f" {'-'*7}", end="")
    print(f"  {'-'*35}")

    for r in outliers:
        w = r["item"]
        name = fmt_name(w, 29)
        z_parts = []
        line = f"  {name:<30} {w.weapon_type or '':<8} {r['total_dev']:>5.1f}"
        for s in TARGET_STATS:
            d = r["details"].get(s)
            if d:
                line += f" {d['value']:>7.1f}"
                z_parts.append(f"{d['z']:+.1f}")
            else:
                line += f" {'-':>7}"
        line += f"  {' '.join(z_parts):<35}"
        print(line)
    print()


def print_ungrouped(weapons: list):
    if not weapons:
        return
    print(f"\n{'='*96}")
    print(f"  [未分類] トレーダー未販売 ({len(weapons)}武器)")
    print(f"{'='*96}")
    stats = calc_stats(weapons)
    parts = []
    for s in TARGET_STATS:
        st = stats[s]
        if st["n"] > 0:
            parts.append(f"{s[:6]} avg={st['mean']:.1f}")
    print(f"  {' | '.join(parts)}")
    print()

    for w in sorted(weapons, key=lambda x: x.display_name or x.id):
        name = fmt_name(w, 29)
        vals = []
        for s in TARGET_STATS:
            v = w.base_stats.get(s, 0.0)
            if v > 0:
                vals.append(f"{s[:4]}={v:.0f}")
        print(f"  {name:<30} [{w.weapon_type}]  {' '.join(vals)}")
    print()


def main():
    print("スキャン中...")
    all_items = scan_items(PROJECT_ROOT)
    traders = scan_traders(PROJECT_ROOT)
    weapons = get_weapon_items(all_items)
    trader_map = build_trader_map(traders)
    groups = group_by_tier(weapons, trader_map)

    print(f"全武器: {len(weapons)}件")
    for t in [1, 2, 3]:
        print(f"  {TIER_LABELS[t][0]}: {len(groups[t])}武器")
    print(f"  未分類: {len(groups[None])}武器")

    for t in [1, 2, 3]:
        print_tier(t, groups[t])
    print_ungrouped(groups[None])


if __name__ == "__main__":
    main()
