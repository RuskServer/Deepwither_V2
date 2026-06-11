"""
全武器のステータスを分析し、ランキングを出力するツール
"""
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
from pathlib import Path
from java_parser import scan_items, ItemData

# プロジェクトルート
PROJECT_ROOT = Path(__file__).parent.parent / "src" / "main" / "java" / "com" / "ruskserver" / "deepwither_V2"


def get_weapon_items(items: list) -> list:
    """武器のみをフィルタリング"""
    return [item for item in items if item.weapon_type is not None]


def rank_by_stat(items: list, stat_name: str, descending: bool = True) -> list:
    """特定のステータスでランキング"""
    ranked = sorted(
        [(item, item.base_stats.get(stat_name, 0.0)) for item in items],
        key=lambda x: x[1],
        reverse=descending
    )
    return ranked


def print_ranking(title: str, ranked: list, stat_name: str, top_n: int = 10):
    """ランキングを表示"""
    print(f"\n{'='*60}")
    print(f"  {title} (上位{top_n}件)")
    print(f"{'='*60}")
    
    for i, (item, value) in enumerate(ranked[:top_n], 1):
        print(f"  {i:2d}. {item.display_name:<30} {stat_name}: {value:>8.1f}")
    
    print(f"{'='*60}")


def print_stats_summary(items: list):
    """ステータスの統計情報を表示"""
    print(f"\n{'='*60}")
    print(f"  武器ステータス 統計情報 (全{len(items)}武器)")
    print(f"{'='*60}")
    
    # 主要ステータスの統計
    main_stats = ["ATTACK_DAMAGE", "MAGIC_DAMAGE", "CRITICAL_CHANCE", "CRITICAL_DAMAGE", "ATTACK_SPEED"]
    
    for stat in main_stats:
        values = [item.base_stats.get(stat, 0.0) for item in items]
        if any(v > 0 for v in values):
            avg = sum(values) / len(values) if values else 0
            max_val = max(values) if values else 0
            min_val = min(values) if values else 0
            print(f"  {stat:<20} avg: {avg:>6.1f}  min: {min_val:>6.1f}  max: {max_val:>6.1f}")
    
    print(f"{'='*60}")


def print_weapon_list(items: list):
    """全武器の一覧を表示"""
    print(f"\n{'='*60}")
    print(f"  全武器一覧 (全{len(items)}武器)")
    print(f"{'='*60}")
    
    for item in sorted(items, key=lambda x: x.display_name):
        stats_str = ", ".join(f"{k}:{v}" for k, v in item.base_stats.items())
        print(f"  {item.display_name:<30} [{item.weapon_type}]")
        print(f"    {stats_str}")
    
    print(f"{'='*60}")


def export_to_json(items: list, output_path: Path):
    """JSONファイルにエクスポート"""
    import json
    
    data = []
    for item in items:
        data.append({
            "id": item.id,
            "display_name": item.display_name,
            "weapon_type": item.weapon_type,
            "rarity": item.rarity,
            "base_stats": item.base_stats,
            "file_path": item.file_path
        })
    
    output_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f"\nJSON出力完了: {output_path}")


def main():
    # コマンドライン引数でトップNを指定可能
    top_n = int(sys.argv[1]) if len(sys.argv) > 1 else 10
    
    # アイテムをスキャン
    print("アイテムファイルをスキャン中...")
    all_items = scan_items(PROJECT_ROOT)
    weapons = get_weapon_items(all_items)
    
    print(f"\n全アイテム: {len(all_items)}件")
    print(f"武器のみ: {len(weapons)}件")
    
    if not weapons:
        print("武器が見つかりませんでした。")
        return
    
    # 攻撃力ランキング
    ranked = rank_by_stat(weapons, "ATTACK_DAMAGE")
    print_ranking("攻撃力ランキング", ranked, "攻撃力", top_n)
    
    # 魔法攻撃力ランキング
    ranked = rank_by_stat(weapons, "MAGIC_DAMAGE")
    print_ranking("魔法攻撃力ランキング", ranked, "魔法攻撃力", top_n)
    
    # クリティカル率ランキング
    ranked = rank_by_stat(weapons, "CRITICAL_CHANCE")
    print_ranking("クリティカル率ランキング", ranked, "クリティカル率", top_n)
    
    # 統計情報
    print_stats_summary(weapons)
    
    # JSON出力
    output_path = Path(__file__).parent / "weapons.json"
    export_to_json(weapons, output_path)


if __name__ == "__main__":
    main()
