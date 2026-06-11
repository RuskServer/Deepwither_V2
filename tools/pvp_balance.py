"""
PvPバランス分析ツール
攻撃力 vs 防御力のバランスを数値化する
"""
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
from pathlib import Path
from java_parser import scan_items, ItemData

# プロジェクトルート
PROJECT_ROOT = Path(__file__).parent.parent / "src" / "main" / "java" / "com" / "ruskserver" / "deepwither_V2"

# ダメージ軽減式（Java側と同じ）
DEFENSE_FACTOR = 250.0


def calc_damage_reduction(defense: float) -> float:
    """防御力によるダメージ軽減率を計算"""
    return DEFENSE_FACTOR / (DEFENSE_FACTOR + defense)


def calc_effective_damage(attack: float, defense: float) -> float:
    """実効ダメージを計算（防御軽減後）"""
    reduction = calc_damage_reduction(defense)
    return attack * reduction


def calc_dps(attack: float, attack_speed: float, defense: float) -> float:
    """DPS（秒間ダメージ）を計算"""
    effective = calc_effective_damage(attack, defense)
    return effective * attack_speed


def calc_ttk(hp: float, dps: float) -> float:
    """TTK（キルまでの時間）を計算（秒）"""
    if dps <= 0:
        return float('inf')
    return hp / dps


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


def analyze_pvp_balance(weapons: list, armors: list, test_defenses: list = None):
    """PvPバランス分析"""
    if test_defenses is None:
        test_defenses = [0, 50, 100, 150, 200, 250, 300]
    
    print(f"\n{'='*80}")
    print(f"  PvPバランス分析")
    print(f"{'='*80}")
    
    # 武器ごとの分析
    print(f"\n  【武器別 実効ダメージ】")
    print(f"  {'武器名':<30} {'攻撃力':>8} ", end="")
    for def_val in test_defenses:
        print(f"  防御{def_val:>3}", end="")
    print()
    print(f"  {'-'*30} {'-'*8} ", end="")
    for _ in test_defenses:
        print(f"  {'-'*6}", end="")
    print()
    
    for weapon in sorted(weapons, key=lambda x: x.base_stats.get("ATTACK_DAMAGE", 0), reverse=True):
        attack = weapon.base_stats.get("ATTACK_DAMAGE", 0)
        print(f"  {weapon.display_name:<30} {attack:>8.1f} ", end="")
        for def_val in test_defenses:
            effective = calc_effective_damage(attack, def_val)
            print(f"  {effective:>6.1f}", end="")
        print()
    
    # 防御力の軽減率を表示
    print(f"\n  【防御力 軽減率】")
    print(f"  {'防御力':>8}  {'軽減率':>8}  {'実ダメージ倍率':>12}")
    for def_val in test_defenses:
        reduction = calc_damage_reduction(def_val)
        print(f"  {def_val:>8}  {reduction:>8.1%}  {(1-reduction):>12.1%}")
    
    print(f"{'='*80}")


def analyze_weapon_vs_weapon(weapons: list):
    """武器同士の対戦シミュレーション"""
    print(f"\n{'='*80}")
    print(f"  武器対武器 シミュレーション (HP=100想定)")
    print(f"{'='*80}")
    
    test_hp = 100.0
    
    # 武器を攻撃力順にソート
    sorted_weapons = sorted(weapons, key=lambda x: x.base_stats.get("ATTACK_DAMAGE", 0), reverse=True)
    
    print(f"\n  {'攻め側':<25} {'受け側':<25} {'TTK(秒)':>10} {'結果':>10}")
    print(f"  {'-'*25} {'-'*25} {'-'*10} {'-'*10}")
    
    # 上位5武器同士の対戦
    top_weapons = sorted_weapons[:5]
    
    for attacker in top_weapons:
        atk = attacker.base_stats.get("ATTACK_DAMAGE", 0)
        aspd = attacker.base_stats.get("ATTACK_SPEED", 1.0)
        
        for defender in top_weapons:
            dfn = defender.base_stats.get("DEFENSE", 0)  # 武器には防御力がないので0
            
            dps = calc_dps(atk, aspd, dfn)
            ttk = calc_ttk(test_hp, dps)
            
            # 結果判定
            if ttk < 3:
                result = "速攻"
            elif ttk < 5:
                result = "標準"
            else:
                result = "遅い"
            
            print(f"  {attacker.display_name:<25} {defender.display_name:<25} {ttk:>10.2f} {result:>10}")
    
    print(f"{'='*80}")


def generate_balance_report(weapons: list, armors: list):
    """バランスレポートを生成"""
    print(f"\n{'='*80}")
    print(f"  バランス調整レポート")
    print(f"{'='*80}")
    
    # 武器の攻撃力範囲
    attacks = [w.base_stats.get("ATTACK_DAMAGE", 0) for w in weapons]
    if attacks:
        avg_atk = sum(attacks) / len(attacks)
        max_atk = max(attacks)
        min_atk = min(atk for atk in attacks if atk > 0) if any(a > 0 for a in attacks) else 0
        
        print(f"\n  【武器攻撃力】")
        print(f"    平均: {avg_atk:.1f}")
        print(f"    最小: {min_atk:.1f}")
        print(f"    最大: {max_atk:.1f}")
        print(f"    レンジ: {max_atk - min_atk:.1f}")
        
        # 攻撃力の偏りをチェック
        if max_atk > avg_atk * 2:
            print(f"    ⚠ 警告: 最大攻撃力が平均の2倍を超えています")
    
    # DPS計算（防御力0の場合）
    print(f"\n  【DPS (防御力=0)】")
    for weapon in sorted(weapons, key=lambda x: x.base_stats.get("ATTACK_DAMAGE", 0) * x.base_stats.get("ATTACK_SPEED", 1.0), reverse=True)[:10]:
        atk = weapon.base_stats.get("ATTACK_DAMAGE", 0)
        aspd = weapon.base_stats.get("ATTACK_SPEED", 1.0)
        dps = atk * aspd
        print(f"    {weapon.display_name:<30} DPS: {dps:>8.1f}")
    
    print(f"{'='*80}")


def main():
    # アイテムをスキャン
    print("アイテムファイルをスキャン中...")
    all_items = scan_items(PROJECT_ROOT)
    weapons = get_weapon_items(all_items)
    armors = get_armor_items(all_items)
    
    print(f"\n全アイテム: {len(all_items)}件")
    print(f"武器: {len(weapons)}件")
    print(f"防具: {len(armors)}件")
    
    if not weapons:
        print("武器が見つかりませんでした。")
        return
    
    # PvPバランス分析
    analyze_pvp_balance(weapons, armors)
    
    # 武器対武器シミュレーション
    analyze_weapon_vs_weapon(weapons)
    
    # バランスレポート
    generate_balance_report(weapons, armors)


if __name__ == "__main__":
    main()
