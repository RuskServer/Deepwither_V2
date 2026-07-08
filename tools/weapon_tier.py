"""
武器を信用度(Reputation)の要求値でTier分類するツール
  Tier 1: 信用度 0-500
  Tier 2: 信用度 500-1000
  Tier 3: 信用度 1000+
"""
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
from pathlib import Path
from java_parser import scan_items, scan_traders, ItemData

PROJECT_ROOT = Path(__file__).parent.parent / "src" / "main" / "java" / "com" / "ruskserver" / "deepwither_V2"


def get_weapon_items(items: list) -> list:
    return [item for item in items if item.weapon_type is not None]


def build_trader_map(traders: list) -> dict:
    trader_map = {}
    for trader in traders:
        for product in trader.products:
            if product.item_id not in trader_map or product.required_reputation > trader_map[product.item_id]["required_reputation"]:
                trader_map[product.item_id] = {
                    "buy_price": product.buy_price,
                    "required_reputation": product.required_reputation,
                    "trader_name": trader.npc_name
                }
    return trader_map


def classify_tier(required_reputation: int) -> tuple:
    if required_reputation <= 500:
        return (1, "Tier 1", "初心者向け (信用度 0-500)")
    elif required_reputation <= 1000:
        return (2, "Tier 2", "中級者向け (信用度 500-1000)")
    else:
        return (3, "Tier 3", "上級者向け (信用度 1000+)")


def build_weapon_tier_list(weapons: list, trader_map: dict) -> dict:
    tiers = {1: [], 2: [], 3: []}
    unclassified = []

    for item in weapons:
        info = trader_map.get(item.id)
        if info is None:
            unclassified.append(item)
            continue
        tier_num, _, _ = classify_tier(info["required_reputation"])
        tiers[tier_num].append((item, info))

    return tiers, unclassified


def print_tier_summary(tiers: dict, unclassified: list):
    print(f"\n{'='*80}")
    print(f"  武器 Tier 分類")
    print(f"{'='*80}\n")

    total_classified = sum(len(v) for v in tiers.values())

    for tier_num in [1, 2, 3]:
        items = tiers[tier_num]
        _, label, desc = classify_tier(tier_num * 500 if tier_num < 3 else 1500)
        print(f"  [{label}] {desc} ({len(items)}武器)")
        print(f"  {'-'*70}")
        if items:
            for item, info in sorted(items, key=lambda x: x[0].display_name or x[0].id):
                name = item.display_name or item.id
                stats_str = ", ".join(f"{k}:{v}" for k, v in item.base_stats.items())
                print(f"    {name:<35} [{item.weapon_type}]")
                print(f"      {stats_str}")
                print(f"      販売: {info['trader_name']} | 価格: {info['buy_price']:>8,.0f} | 必要信用度: {info['required_reputation']}")
        else:
            print(f"    (該当なし)")
        print()

    if unclassified:
        print(f"  [未分類] トレーダー未販売 ({len(unclassified)}武器)")
        print(f"  {'-'*70}")
        for item in sorted(unclassified, key=lambda x: x.display_name or x.id):
            name = item.display_name or item.id
            stats_str = ", ".join(f"{k}:{v}" for k, v in item.base_stats.items())
            print(f"    {name:<35} [{item.weapon_type}]")
            print(f"      {stats_str}")
        print()

    total_weapons = total_classified + len(unclassified)
    print(f"  {'='*70}")
    print(f"  合計: {total_weapons}武器 (分類済み: {total_classified}, 未分類: {len(unclassified)})")
    print(f"  {'='*70}")


def print_tier_by_weapon_type(tiers: dict):
    print(f"\n{'='*80}")
    print(f"  武器種別 × Tier クロス集計")
    print(f"{'='*80}\n")

    all_types = set()
    for tier_num in [1, 2, 3]:
        for item, _ in tiers[tier_num]:
            if item.weapon_type:
                all_types.add(item.weapon_type)

    sorted_types = sorted(all_types)
    header = f"  {'武器種':<15}"
    for t in [1, 2, 3]:
        header += f" {'Tier'+str(t):>8}"
    header += f" {'計':>6}"
    print(header)
    print(f"  {'-'*15} {'-'*8} {'-'*8} {'-'*8} {'-'*6}")

    for wtype in sorted_types:
        row = f"  {wtype:<15}"
        total = 0
        for t in [1, 2, 3]:
            count = sum(1 for item, _ in tiers[t] if item.weapon_type == wtype)
            row += f" {count:>8}"
            total += count
        row += f" {total:>6}"
        print(row)

    total_row = f"  {'合計':<15}"
    grand_total = 0
    for t in [1, 2, 3]:
        count = len(tiers[t])
        total_row += f" {count:>8}"
        grand_total += count
    total_row += f" {grand_total:>6}"
    print(f"  {'-'*15} {'-'*8} {'-'*8} {'-'*8} {'-'*6}")
    print(total_row)
    print()


def print_stats_by_tier(tiers: dict):
    print(f"{'='*80}")
    print(f"  Tier別 ステータス平均")
    print(f"{'='*80}\n")

    main_stats = ["ATTACK_DAMAGE", "MAGIC_DAMAGE", "CRITICAL_CHANCE", "CRITICAL_DAMAGE", "ATTACK_SPEED"]

    header = f"  {'ステータス':<20}"
    for t in [1, 2, 3]:
        header += f" {'Tier'+str(t):>12}"
    print(header)
    print(f"  {'-'*20} {'-'*12} {'-'*12} {'-'*12}")

    for stat in main_stats:
        row = f"  {stat:<20}"
        for t in [1, 2, 3]:
            values = [item.base_stats.get(stat, 0.0) for item, _ in tiers[t]]
            values = [v for v in values if v > 0]
            if values:
                avg = sum(values) / len(values)
                row += f" {avg:>12.1f}"
            else:
                row += f" {'-':>12}"
        print(row)
    print()


def main():
    print("アイテム・トレーダーファイルをスキャン中...")
    all_items = scan_items(PROJECT_ROOT)
    traders = scan_traders(PROJECT_ROOT)
    weapons = get_weapon_items(all_items)
    trader_map = build_trader_map(traders)

    print(f"全アイテム: {len(all_items)}件")
    print(f"武器: {len(weapons)}件")
    print(f"トレーダー: {len(traders)}店")
    print(f"トレーダー販売商品: {len(trader_map)}件")

    tiers, unclassified = build_weapon_tier_list(weapons, trader_map)

    mode = sys.argv[1] if len(sys.argv) > 1 else "all"

    if mode == "all":
        print_tier_summary(tiers, unclassified)
        print_tier_by_weapon_type(tiers)
        print_stats_by_tier(tiers)
    elif mode == "list":
        print_tier_summary(tiers, unclassified)
    elif mode == "cross":
        print_tier_by_weapon_type(tiers)
    elif mode == "stats":
        print_stats_by_tier(tiers)
    else:
        print("使い方:")
        print("  python weapon_tier.py           # 全表示")
        print("  python weapon_tier.py list      # Tier一覧")
        print("  python weapon_tier.py cross     # 武器種×Tierクロス")
        print("  python weapon_tier.py stats     # Tier別ステータス平均")


if __name__ == "__main__":
    main()
