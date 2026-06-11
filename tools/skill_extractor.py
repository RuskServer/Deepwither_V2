"""
全スキルの説明を抽出し、一覧表示するツール
"""
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
import json
from pathlib import Path
from java_parser import scan_skills, SkillData

# プロジェクトルート
PROJECT_ROOT = Path(__file__).parent.parent / "src" / "main" / "java" / "com" / "ruskserver" / "deepwither_V2"


def group_by_role(skills: list) -> dict:
    """ロール別にスキルを分類"""
    groups = {}
    for skill in skills:
        for role in skill.roles:
            if role not in groups:
                groups[role] = []
            groups[role].append(skill)
    return groups


def group_by_scaling(skills: list) -> dict:
    """スケーリング別にスキルを分類"""
    groups = {}
    for skill in skills:
        for scaling in skill.scalings:
            if scaling not in groups:
                groups[scaling] = []
            groups[scaling].append(skill)
    return groups


def print_skill_list(skills: list, title: str = "全スキル一覧"):
    """スキル一覧を表示"""
    print(f"\n{'='*80}")
    print(f"  {title} (全{len(skills)}スキル)")
    print(f"{'='*80}")
    
    for skill in sorted(skills, key=lambda x: x.display_name):
        print(f"\n  ■ {skill.display_name} ({skill.id})")
        
        # 説明文
        if skill.description:
            for desc in skill.description:
                print(f"    {desc}")
        
        # ステータス
        tags_str = ", ".join(skill.tags) if skill.tags else "-"
        roles_str = ", ".join(skill.roles) if skill.roles else "-"
        scalings_str = ", ".join(skill.scalings) if skill.scalings else "-"
        
        print(f"    コスト: {skill.mana_cost:.0f}マナ / クールダウン: {skill.cooldown_seconds:.0f}秒")
        print(f"    タグ: {tags_str}")
        print(f"    ロール: {roles_str} / スケーリング: {scalings_str}")
    
    print(f"\n{'='*80}")


def print_skills_by_group(skills: list, group_name: str):
    """グループ別のスキルを表示"""
    groups = group_by_role(skills) if group_name == "role" else group_by_scaling(skills)
    
    print(f"\n{'='*80}")
    print(f"  {group_name.upper()}別スキル分類")
    print(f"{'='*80}")
    
    for group, group_skills in sorted(groups.items()):
        print(f"\n  【{group}】 ({len(group_skills)}スキル)")
        for skill in group_skills:
            desc_preview = skill.description[0][:50] + "..." if skill.description else "（説明なし）"
            print(f"    - {skill.display_name}: {desc_preview}")
    
    print(f"{'='*80}")


def export_to_json(skills: list, output_path: Path):
    """JSONファイルにエクスポート"""
    data = []
    for skill in skills:
        data.append({
            "id": skill.id,
            "display_name": skill.display_name,
            "description": skill.description,
            "mana_cost": skill.mana_cost,
            "cooldown_seconds": skill.cooldown_seconds,
            "roles": skill.roles,
            "scalings": skill.scalings,
            "tags": skill.tags,
            "file_path": skill.file_path
        })
    
    output_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f"\nJSON出力完了: {output_path}")


def export_descriptions_only(skills: list, output_path: Path):
    """説明文のみを抽出してエクスポート"""
    data = []
    for skill in skills:
        data.append({
            "id": skill.id,
            "name": skill.display_name,
            "description": skill.description
        })
    
    output_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f"\n説明文JSON出力完了: {output_path}")


def search_skills(skills: list, keyword: str) -> list:
    """キーワードでスキルを検索"""
    results = []
    keyword_lower = keyword.lower()
    
    for skill in skills:
        # ID、名前、説明文で検索
        if (keyword_lower in skill.id.lower() or
            keyword_lower in skill.display_name.lower() or
            any(keyword_lower in desc.lower() for desc in skill.description)):
            results.append(skill)
    
    return results


def main():
    # スキルをスキャン
    print("スキルファイルをスキャン中...")
    skills = scan_skills(PROJECT_ROOT)
    
    print(f"\n全スキル: {len(skills)}件")
    
    if not skills:
        print("スキルが見つかりませんでした。")
        return
    
    # 引数でモードを選択
    mode = sys.argv[1] if len(sys.argv) > 1 else "all"
    
    if mode == "all":
        # 全スキル一覧
        print_skill_list(skills)
        
        # ロール別分類
        print_skills_by_group(skills, "role")
        
        # スケーリング別分類
        print_skills_by_group(skills, "scaling")
        
        # JSON出力
        output_path = Path(__file__).parent / "skills.json"
        export_to_json(skills, output_path)
        
        # 説明文のみ出力
        desc_path = Path(__file__).parent / "skill_descriptions.json"
        export_descriptions_only(skills, desc_path)
    
    elif mode == "desc":
        # 説明文のみ表示
        print_skill_list(skills, "スキル説明一覧")
        desc_path = Path(__file__).parent / "skill_descriptions.json"
        export_descriptions_only(skills, desc_path)
    
    elif mode == "search" and len(sys.argv) > 2:
        # キーワード検索
        keyword = sys.argv[2]
        results = search_skills(skills, keyword)
        print(f"\n「{keyword}」の検索結果: {len(results)}件")
        print_skill_list(results, f"検索結果: {keyword}")
    
    elif mode == "role":
        # ロール別分類のみ
        print_skills_by_group(skills, "role")
    
    elif mode == "scaling":
        # スケーリング別分類のみ
        print_skills_by_group(skills, "scaling")
    
    else:
        print("使い方:")
        print("  python skill_extractor.py all      # 全スキル一覧 + JSON出力")
        print("  python skill_extractor.py desc     # 説明文のみ表示")
        print("  python skill_extractor.py role     # ロール別分類")
        print("  python skill_extractor.py scaling  # スケーリング別分類")
        print("  python skill_extractor.py search <keyword>  # キーワード検索")


if __name__ == "__main__":
    main()
