"""
全アイテム統合分析ツール
武器の火力、防具の防御力、入手難易度とステータスの関係を分析する
"""
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
from pathlib import Path
from java_parser import scan_items, scan_traders, ItemData, TraderData

# プロジェクトルート
PROJECT_ROOT = Path(__file__).parent.parent / "src" / "main" / "java" / "com" / "ruskserver" / "deepwither_V2"

# レアリティ重み付け
RARITY_WEIGHT = {
    "COMMON": 1,
    "UNCOMMON": 2,
    "RARE": 3,
    "EPIC": 4,
    "LEGENDARY": 5
}

# ダメージ軽減式
DEFENSE_FACTOR = 250.0


def get_weapon_items(items: list) -> list:
    """武器のみをフィルタリング"""
    return [item for item in items if item.weapon_type is not None]


def get_armor_items(items: list) -> list:
    """防具のみをフィルタリング（IDにarmor系キーワードを含む）"""
    armor_keywords = ["helmet", "hood", "headguard", "chestplate", "leggings", "boots", "footactuator"]
    return [
        item for item in items
        if any(kw in item.id.lower() for kw in armor_keywords)
    ]


def calc_dps(item: ItemData) -> float:
    """DPS（秒間ダメージ）を計算"""
    atk = item.base_stats.get("ATTACK_DAMAGE", 0)
    aspd = item.base_stats.get("ATTACK_SPEED", 1.0)
    return atk * aspd


def calc_total_defense(item: ItemData) -> float:
    """総防御力を計算"""
    defense = item.base_stats.get("DEFENSE", 0)
    magic_def = item.base_stats.get("MAGIC_DEFENSE", 0)
    return defense + magic_def


def calc_damage_reduction(defense: float) -> float:
    """防御力によるダメージ軽減率を計算"""
    return DEFENSE_FACTOR / (DEFENSE_FACTOR + defense)


def build_trader_map(traders: list) -> dict:
    """トレーダーデータからアイテムID -> (価格, 信用度) のマップを作成"""
    trader_map = {}
    for trader in traders:
        for product in trader.products:
            if product.item_id not in trader_map:
                trader_map[product.item_id] = {
                    "buy_price": product.buy_price,
                    "required_reputation": product.required_reputation,
                    "trader_name": trader.npc_name
                }
    return trader_map


def calc_acquisition_difficulty(item: ItemData, trader_info: dict = None) -> dict:
    """入手難易度を計算"""
    rarity_score = RARITY_WEIGHT.get(item.rarity, 0)
    sell_price = item.sell_price
    
    # トレーダー情報
    has_trader = trader_info is not None
    buy_price = trader_info["buy_price"] if trader_info else 0
    required_rep = trader_info["required_reputation"] if trader_info else 0
    
    # 難易度スコア（0-100）
    # レアリティ: 20点
    # 売却価格（高価=希少）: 30点
    # トレーダー信用度: 30点
    # トレーダー購入価格: 20点
    
    rarity_score_norm = (rarity_score / 5) * 20
    
    # 売却価格をスコアに変換（最大10000で20点）
    price_score = min(sell_price / 10000, 1.0) * 30
    
    # 信用度をスコアに変換（最大1000で30点）
    rep_score = (required_rep / 1000) * 30 if has_trader else 0
    
    # 購入価格をスコアに変換（最大100000で20点）
    buy_score = min(buy_price / 100000, 1.0) * 20 if has_trader else 0
    
    total_score = rarity_score_norm + price_score + rep_score + buy_score
    
    return {
        "total_score": total_score,
        "rarity_score": rarity_score_norm,
        "price_score": price_score,
        "rep_score": rep_score,
        "buy_score": buy_score,
        "has_trader": has_trader,
        "buy_price": buy_price,
        "required_reputation": required_rep
    }


def print_weapon_analysis(weapons: list, trader_map: dict):
    """武器分析を表示"""
    print(f"\n{'='*100}")
    print(f"  武器火力分析")
    print(f"{'='*100}")
    
    # DPSでソート
    sorted_weapons = sorted(weapons, key=lambda x: calc_dps(x), reverse=True)
    
    print(f"\n  {'武器名':<35} {'攻撃力':>8} {'攻速':>6} {'DPS':>8} {'レアリティ':<12} {'売値':>10} {'難易度':>8}")
    print(f"  {'-'*35} {'-'*8} {'-'*6} {'-'*8} {'-'*12} {'-'*10} {'-'*8}")
    
    for weapon in sorted_weapons:
        atk = weapon.base_stats.get("ATTACK_DAMAGE", 0)
        aspd = weapon.base_stats.get("ATTACK_SPEED", 1.0)
        dps = calc_dps(weapon)
        trader_info = trader_map.get(weapon.id)
        difficulty = calc_acquisition_difficulty(weapon, trader_info)
        
        trader_mark = "◎" if difficulty["has_trader"] else "  "
        
        print(f"  {weapon.display_name:<35} {atk:>8.1f} {aspd:>6.2f} {dps:>8.1f} {weapon.rarity:<12} {weapon.sell_price:>10.0f} {difficulty['total_score']:>6.1f}{trader_mark}")
    
    # DPS統計
    dps_values = [calc_dps(w) for w in weapons]
    avg_dps = sum(dps_values) / len(dps_values) if dps_values else 0
    max_dps = max(dps_values) if dps_values else 0
    min_dps = min(dps_values) if dps_values else 0
    
    print(f"\n  DPS統計: 平均={avg_dps:.1f}, 最小={min_dps:.1f}, 最大={max_dps:.1f}")
    
    print(f"  ◎ = トレーダーで購入可能")
    print(f"{'='*100}")


def print_armor_analysis(armors: list, trader_map: dict):
    """防具分析を表示"""
    print(f"\n{'='*100}")
    print(f"  防具防御力分析")
    print(f"{'='*100}")
    
    # 総防御力でソート
    sorted_armors = sorted(armors, key=lambda x: calc_total_defense(x), reverse=True)
    
    print(f"\n  {'防具名':<40} {'物理防御':>8} {'魔法防御':>8} {'HP':>6} {'レアリティ':<12} {'売値':>10} {'難易度':>8}")
    print(f"  {'-'*40} {'-'*8} {'-'*8} {'-'*6} {'-'*12} {'-'*10} {'-'*8}")
    
    for armor in sorted_armors:
        defense = armor.base_stats.get("DEFENSE", 0)
        magic_def = armor.base_stats.get("MAGIC_DEFENSE", 0)
        health = armor.base_stats.get("HEALTH", 0)
        trader_info = trader_map.get(armor.id)
        difficulty = calc_acquisition_difficulty(armor, trader_info)
        
        trader_mark = "◎" if difficulty["has_trader"] else "  "
        
        print(f"  {armor.display_name:<40} {defense:>8.1f} {magic_def:>8.1f} {health:>6.0f} {armor.rarity:<12} {armor.sell_price:>10.0f} {difficulty['total_score']:>6.1f}{trader_mark}")
    
    # 防御力統計
    defense_values = [calc_total_defense(a) for a in armors]
    avg_def = sum(defense_values) / len(defense_values) if defense_values else 0
    max_def = max(defense_values) if defense_values else 0
    min_def = min(defense_values) if defense_values else 0
    
    print(f"\n  防御力統計: 平均={avg_def:.1f}, 最小={min_def:.1f}, 最大={max_def:.1f}")
    print(f"  ◎ = トレーダーで購入可能")
    print(f"{'='*100}")


def print_difficulty_ranking(items: list, trader_map: dict, top_n: int = 15):
    """入手難易度ランキングを表示"""
    print(f"\n{'='*100}")
    print(f"  入手難易度ランキング (上位{top_n}件)")
    print(f"{'='*100}")
    
    # 難易度スコアでソート
    items_with_difficulty = []
    for item in items:
        trader_info = trader_map.get(item.id)
        difficulty = calc_acquisition_difficulty(item, trader_info)
        items_with_difficulty.append((item, difficulty))
    
    sorted_items = sorted(items_with_difficulty, key=lambda x: x[1]["total_score"], reverse=True)
    
    print(f"\n  {'順位':>4} {'アイテム名':<40} {'難易度':>8} {'レアリティ':<12} {'売値':>10} {'トレーダー価格':>12} {'必要信用度':>10}")
    print(f"  {'-'*4} {'-'*40} {'-'*8} {'-'*12} {'-'*10} {'-'*12} {'-'*10}")
    
    for i, (item, difficulty) in enumerate(sorted_items[:top_n], 1):
        trader_price_str = f"{difficulty['buy_price']:>10.0f}" if difficulty["has_trader"] else "      -    "
        rep_str = f"{difficulty['required_reputation']:>8}" if difficulty["has_trader"] else "    -   "
        
        print(f"  {i:>4} {item.display_name:<40} {difficulty['total_score']:>8.1f} {item.rarity:<12} {item.sell_price:>10.0f} {trader_price_str} {rep_str}")
    
    print(f"{'='*100}")


def print_status_vs_difficulty(items: list, trader_map: dict):
    """ステータスと入手難易度の関係を表示"""
    print(f"\n{'='*100}")
    print(f"  ステータス vs 入手難易度 関係")
    print(f"{'='*100}")
    
    # レアリティ別にグループ化
    rarity_groups = {}
    for item in items:
        trader_info = trader_map.get(item.id)
        difficulty = calc_acquisition_difficulty(item, trader_info)
        rarity = item.rarity
        
        if rarity not in rarity_groups:
            rarity_groups[rarity] = []
        rarity_groups[rarity].append((item, difficulty))
    
    for rarity in ["COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY"]:
        if rarity not in rarity_groups:
            continue
        
        group = rarity_groups[rarity]
        print(f"\n  【{rarity}】 ({len(group)}アイテム)")
        
        # 主要ステータスの平均を計算
        stat_totals = {}
        for item, _ in group:
            for stat, value in item.base_stats.items():
                if stat not in stat_totals:
                    stat_totals[stat] = []
                stat_totals[stat].append(value)
        
        # 重要なステータスのみ表示
        important_stats = ["ATTACK_DAMAGE", "DEFENSE", "MAGIC_DEFENSE", "HEALTH", "CRITICAL_CHANCE"]
        for stat in important_stats:
            if stat in stat_totals and stat_totals[stat]:
                values = stat_totals[stat]
                avg = sum(values) / len(values)
                print(f"    {stat:<20} 平均: {avg:>8.1f}  (件数: {len(values)})")
    
    print(f"{'='*100}")


def print_balance_summary(weapons: list, armors: list, trader_map: dict):
    """バランスサマリーを表示"""
    print(f"\n{'='*100}")
    print(f"  バランス調整サマリー")
    print(f"{'='*100}")
    
    # 武器の火力レンジ
    dps_values = [calc_dps(w) for w in weapons]
    avg_dps = sum(dps_values) / len(dps_values) if dps_values else 0
    max_dps = max(dps_values) if dps_values else 0
    
    print(f"\n  【武器】")
    print(f"    武器数: {len(weapons)}")
    print(f"    DPS範囲: {min(dps_values):.1f} ~ {max_dps:.1f} (平均: {avg_dps:.1f})")
    
    if max_dps > avg_dps * 2:
        print(f"    ⚠ 警告: 最大DPSが平均の2倍を超えています ({max_dps/avg_dps:.1f}倍)")
    
    # 防具の防御力レンジ
    defense_values = [calc_total_defense(a) for a in armors]
    avg_def = sum(defense_values) / len(defense_values) if defense_values else 0
    max_def = max(defense_values) if defense_values else 0
    
    print(f"\n  【防具】")
    print(f"    防具数: {len(armors)}")
    print(f"    総防御力範囲: {min(defense_values):.1f} ~ {max_def:.1f} (平均: {avg_def:.1f})")
    
    if max_def > avg_def * 2:
        print(f"    ⚠ 警告: 最大防御力が平均の2倍を超えています ({max_def/avg_def:.1f}倍)")
    
    # トレーダー販売アイテムの統計
    trader_items = [item for item in weapons + armors if item.id in trader_map]
    print(f"\n  【トレーダー】")
    print(f"    トレーダー販売アイテム数: {len(trader_items)}")
    
    if trader_items:
        trader_dps = [calc_dps(i) for i in trader_items if i.weapon_type]
        trader_def = [calc_total_defense(i) for i in trader_items if not i.weapon_type]
        
        if trader_dps:
            print(f"      武器DPS平均: {sum(trader_dps)/len(trader_dps):.1f}")
        if trader_def:
            print(f"      防具防御力平均: {sum(trader_def)/len(trader_def):.1f}")
    
    print(f"{'='*100}")


def main():
    # アイテムとトレーダーをスキャン
    print("ファイルをスキャン中...")
    all_items = scan_items(PROJECT_ROOT)
    traders = scan_traders(PROJECT_ROOT)
    
    weapons = get_weapon_items(all_items)
    armors = get_armor_items(all_items)
    trader_map = build_trader_map(traders)
    
    print(f"\n全アイテム: {len(all_items)}件")
    print(f"武器: {len(weapons)}件")
    print(f"防具: {len(armors)}件")
    print(f"トレーダー: {len(traders)}店")
    print(f"トレーダー販売商品: {len(trader_map)}件")
    
    # 引数でモードを選択
    mode = sys.argv[1] if len(sys.argv) > 1 else "all"
    
    if mode == "all":
        print_weapon_analysis(weapons, trader_map)
        print_armor_analysis(armors, trader_map)
        print_difficulty_ranking(all_items, trader_map)
        print_status_vs_difficulty(all_items, trader_map)
        print_balance_summary(weapons, armors, trader_map)
    
    elif mode == "weapon":
        print_weapon_analysis(weapons, trader_map)
    
    elif mode == "armor":
        print_armor_analysis(armors, trader_map)
    
    elif mode == "difficulty":
        top_n = int(sys.argv[2]) if len(sys.argv) > 2 else 15
        print_difficulty_ranking(all_items, trader_map, top_n)
    
    elif mode == "relation":
        print_status_vs_difficulty(all_items, trader_map)
    
    elif mode == "summary":
        print_balance_summary(weapons, armors, trader_map)
    
    else:
        print("使い方:")
        print("  python item_analyzer.py all        # 全分析")
        print("  python item_analyzer.py weapon     # 武器分析のみ")
        print("  python item_analyzer.py armor      # 防具分析のみ")
        print("  python item_analyzer.py difficulty  # 入手難易度ランキング")
        print("  python item_analyzer.py relation   # ステータスvs難易度関係")
        print("  python item_analyzer.py summary    # バランスサマリー")


if __name__ == "__main__":
    main()
