"""
Javaソースファイルをパースし、アイテム・スキルの情報を抽出するモジュール
"""
import re
from pathlib import Path
from dataclasses import dataclass, field
from typing import Optional


@dataclass
class ItemData:
    """アイテムのデータ"""
    id: str = ""
    display_name: str = ""
    base_stats: dict = field(default_factory=dict)
    rarity: str = ""
    weapon_type: Optional[str] = None
    flavor_text: str = ""
    sell_price: float = 0.0
    file_path: str = ""


@dataclass
class TraderProductData:
    """トレーダー商品のデータ"""
    item_id: str = ""
    buy_price: float = 0.0
    required_reputation: int = 0


@dataclass
class TraderData:
    """トレーダーのデータ"""
    npc_name: str = ""
    display_name: str = ""
    products: list = field(default_factory=list)
    file_path: str = ""


@dataclass
class SkillData:
    """スキルのデータ"""
    id: str = ""
    display_name: str = ""
    description: list = field(default_factory=list)
    mana_cost: float = 0.0
    cooldown_seconds: float = 0.0
    roles: list = field(default_factory=list)
    scalings: list = field(default_factory=list)
    tags: list = field(default_factory=list)
    file_path: str = ""


def extract_string_value(content: str, method_name: str) -> str:
    """メソッドの戻り値から文字列を抽出"""
    pattern = rf'@Override\s+public\s+String\s+{method_name}\(\)\s*\{{\s*return\s+"([^"]+)";'
    match = re.search(pattern, content)
    return match.group(1) if match else ""


def extract_list_of_strings(content: str, method_name: str) -> list:
    """メソッドの戻り値からList<String>を抽出"""
    pattern = rf'@Override\s+public\s+List<String>\s+{method_name}\(\)\s*\{{\s*return\s+List\.of\((.*?)\);'
    match = re.search(pattern, content, re.DOTALL)
    if not match:
        return []
    
    list_content = match.group(1)
    return re.findall(r'"([^"]*)"', list_content)


def extract_set_of_enums(content: str, method_name: str, enum_type: str) -> list:
    """メソッドの戻り値からSet<EnumType>を抽出"""
    pattern = rf'@Override\s+public\s+Set<{enum_type}>\s+{method_name}\(\)\s*\{{\s*return\s+Set\.of\((.*?)\);'
    match = re.search(pattern, content, re.DOTALL)
    if not match:
        return []
    
    set_content = match.group(1)
    return re.findall(rf'{enum_type}\.(\w+)', set_content)


def extract_set_of_strings(content: str, method_name: str) -> list:
    """メソッドの戻り値からSet<String>を抽出"""
    pattern = rf'@Override\s+public\s+Set<String>\s+{method_name}\(\)\s*\{{\s*return\s+Set\.of\((.*?)\);'
    match = re.search(pattern, content, re.DOTALL)
    if not match:
        return []
    
    set_content = match.group(1)
    return re.findall(r'"([^"]*)"', set_content)


def extract_double_value(content: str, method_name: str) -> float:
    """メソッドの戻り値から数値を抽出"""
    pattern = rf'@Override\s+public\s+double\s+{method_name}\(SkillContext\s+\w+\)\s*\{{\s*return\s+([\d.]+);'
    match = re.search(pattern, content)
    return float(match.group(1)) if match else 0.0


def extract_duration_seconds(content: str, method_name: str) -> float:
    """Duration.ofSeconds(N)から秒数を抽出"""
    pattern = rf'@Override\s+public\s+Duration\s+{method_name}\(SkillContext\s+\w+\)\s*\{{\s*return\s+Duration\.ofSeconds\((\d+)\);'
    match = re.search(pattern, content)
    return float(match.group(1)) if match else 0.0


def extract_base_stats(content: str) -> dict:
    """コンストラクタ内のbaseStats.put()からステータスを抽出"""
    stats = {}
    pattern = r'this\.baseStats\.put\(StatType\.(\w+),\s*([\d.]+)\);'
    for match in re.finditer(pattern, content):
        stat_name = match.group(1)
        stat_value = float(match.group(2))
        stats[stat_name] = stat_value
    return stats


def parse_item_file(file_path: Path) -> Optional[ItemData]:
    """アイテムファイルをパース"""
    try:
        content = file_path.read_text(encoding='utf-8')
    except Exception:
        return None
    
    # CustomItemを実装していない場合はスキップ
    if 'implements CustomItem' not in content:
        return None
    
    item = ItemData()
    item.file_path = str(file_path)
    item.id = extract_string_value(content, "getId")
    item.display_name = extract_string_value(content, "getDisplayName")
    item.base_stats = extract_base_stats(content)
    item.weapon_type = extract_string_value(content, "getWeaponType") or None
    
    # レアリティを抽出
    rarity_match = re.search(r'return\s+ItemRarity\.(\w+);', content)
    if rarity_match:
        item.rarity = rarity_match.group(1)
    
    # 売却価格を抽出
    sell_match = re.search(r'@Override\s+public\s+double\s+getSellPrice\(\)\s*\{\s*return\s+([\d.]+);', content)
    if sell_match:
        item.sell_price = float(sell_match.group(1))
    
    return item if item.id else None


def parse_skill_file(file_path: Path) -> Optional[SkillData]:
    """スキルファイルをパース"""
    try:
        content = file_path.read_text(encoding='utf-8')
    except Exception:
        return None
    
    # Skillを実装していない場合はスキップ
    if 'implements Skill' not in content:
        return None
    
    skill = SkillData()
    skill.file_path = str(file_path)
    skill.id = extract_string_value(content, "getId")
    skill.display_name = extract_string_value(content, "getDisplayName")
    skill.description = extract_list_of_strings(content, "getDescription")
    skill.mana_cost = extract_double_value(content, "getManaCost")
    skill.cooldown_seconds = extract_duration_seconds(content, "getCooldown")
    skill.roles = extract_set_of_enums(content, "getRoles", "SkillTag.Role")
    skill.scalings = extract_set_of_enums(content, "getScalings", "SkillTag.Scaling")
    skill.tags = extract_set_of_strings(content, "getTags")
    
    return skill if skill.id else None


def scan_java_files(base_dir: Path, subdirectory: str, parser_func) -> list:
    """指定ディレクトリ配下のJavaファイルをスキャン"""
    results = []
    target_dir = base_dir / subdirectory
    
    if not target_dir.exists():
        return results
    
    for java_file in target_dir.rglob("*.java"):
        parsed = parser_func(java_file)
        if parsed:
            results.append(parsed)
    
    return results


def scan_items(base_dir: Path) -> list:
    """全アイテムファイルをスキャン"""
    return scan_java_files(base_dir, "modules/item/definitions", parse_item_file)


def parse_trader_file(file_path: Path) -> Optional[TraderData]:
    """トレーダーファイルをパース"""
    try:
        content = file_path.read_text(encoding='utf-8')
    except Exception:
        return None
    
    # TraderDefinitionを実装していない場合はスキップ
    if 'implements TraderDefinition' not in content:
        return None
    
    trader = TraderData()
    trader.file_path = str(file_path)
    trader.npc_name = extract_string_value(content, "getNpcName")
    trader.display_name = extract_string_value(content, "getDisplayName")
    
    # TraderProductのリストを抽出
    # new TraderProduct("item_id", buyPrice, requiredReputation) のパターン
    product_pattern = r'new TraderProduct\("([^"]+)",\s*([\d.]+)(?:,\s*(\d+))?\)'
    for match in re.finditer(product_pattern, content):
        item_id = match.group(1)
        buy_price = float(match.group(2))
        required_rep = int(match.group(3)) if match.group(3) else 0
        trader.products.append(TraderProductData(item_id, buy_price, required_rep))
    
    return trader if trader.npc_name else None


def scan_traders(base_dir: Path) -> list:
    """全トレーダーファイルをスキャン"""
    return scan_java_files(base_dir, "modules/trader/definitions", parse_trader_file)
