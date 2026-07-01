"""
スキル間の相性・アンチ相性・カウンター関係を分析するツール
"""
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
import json
from pathlib import Path
from collections import defaultdict
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.colors as mcolors
import matplotlib.font_manager as fm
import numpy as np

# 日本語フォント設定
plt.rcParams["font.family"] = "sans-serif"
plt.rcParams["font.sans-serif"] = ["MS Gothic", "Yu Gothic", "Noto Sans JP", "Meiryo", "BIZ UDGothic"]
plt.rcParams["axes.unicode_minus"] = False

SKILLS_JSON = Path(__file__).parent / "skills.json"


class SkillObj:
    def __init__(self, d):
        self.id = d["id"]
        self.display_name = d["display_name"]
        self.description = d["description"]
        self.mana_cost = d["mana_cost"]
        self.cooldown_seconds = d["cooldown_seconds"]
        self.roles = d["roles"]
        self.scalings = d["scalings"]
        self.tags = d["tags"]
        self.file_path = d["file_path"]


def load_skills() -> list:
    with open(SKILLS_JSON, encoding='utf-8') as f:
        return [SkillObj(d) for d in json.load(f)]


def find_skill(skills: list, skill_id: str):
    for s in skills:
        if s.id == skill_id or s.display_name == skill_id:
            return s
    return None


def get_element(tags: list) -> str:
    tag_set = set(tags)
    if "fire" in tag_set: return "fire"
    if "ice" in tag_set: return "ice"
    if "lightning" in tag_set: return "lightning"
    if "holy" in tag_set: return "holy"
    return "physical"


def get_category(tags: list) -> str:
    tag_set = set(tags)
    if "archer" in tag_set: return "archer"
    if "warrior" in tag_set: return "warrior"
    if "magic" in tag_set: return "mage"
    if "holy" in tag_set: return "holy"
    return "other"


ROLE_SYNERGY_PAIRS = {
    ("ATTACK", "CONTROL"): "CC + burst の基本コンボ",
    ("ATTACK", "UTILITY"): "機動力を活かした攻撃",
    ("CONTROL", "DEFENSE"): "CCタンク運用",
    ("DEFENSE", "SUPPORT"): "鉄壁の持久戦",
    ("CONTROL", "SUPPORT"): "CCで味方を守るピール",
    ("SUPPORT", "UTILITY"): "機動支援型サポート",
    ("ATTACK", "SUPPORT"): "サステイン付き攻勢",
    ("CONTROL", "UTILITY"): "位置操作+CCのハメ性能",
}

ROLE_OVERLAP_WARNING = {
    "ATTACK": 4,
    "DEFENSE": 3,
    "SUPPORT": 3,
    "CONTROL": 3,
    "UTILITY": 3,
}

ELEMENT_SYNERGY = {
    "fire": "火属性統一 - 継続火力とAoEに特化",
    "ice": "氷属性統一 - スロウ・CCで敵を完全に制圧",
    "lightning": "雷属性統一 - 瞬間火力の高い電撃ビルド",
    "holy": "聖属性統一 - ヒール・バリアでチームを支える",
    "physical": "物理統一 - シンプルな火力特化ビルド",
}

ELEMENT_ANTI_SYNERGY = {
    ("fire", "ice"): "火と氷の混在は属性統一ボーナスが得られず中途半端",
}

COMBO_PAIRS = [
    {
        "skills": ("seismic_stomp", "colossus", "thunder_blast", "thunder_strike"),
        "reason": "足止めCC → 高倍率AoE burst の確定コンボ",
        "min_count": 2,
    },
    {
        "skills": ("blink", "charge", "evade"),
        "reason": "複数移動スキルで超機動ビルド — ヒットアンドアウェイの要",
        "min_count": 2,
    },
    {
        "skills": ("divine_shield", "shield_wall", "fortress"),
        "reason": "防御バフスタック — ダメージ軽減率が飛躍的に上昇",
        "min_count": 2,
    },
    {
        "skills": ("renewal", "sanctuary", "mass_heal"),
        "reason": "継続回復 + 範囲回復 + 即時回復の完全ヒールセット",
        "min_count": 2,
    },
    {
        "skills": ("blizzard", "flame_pillar", "ice_spike"),
        "reason": "持続AoE複数設置 — ゾーニング性能が極めて高い",
        "min_count": 2,
    },
    {
        "skills": ("battle_cry", "fortress", "colossus"),
        "reason": "自己強化 + 範囲火力 + 回復のウォーリアー黄金構成",
        "min_count": 2,
    },
    {
        "skills": ("rain_of_arrows", "blizzard", "thunder_strike"),
        "reason": "広範囲制圧コンボ — 複数Ultimate級AoEで戦場掌握",
        "min_count": 2,
    },
    {
        "skills": ("sacrificial_light", "first_aid", "holy_light"),
        "reason": "シングルヒール複数搭載 — 対象を問わず回復可能",
        "min_count": 2,
    },
    {
        "skills": ("holy_resurrection", "guardian_angel", "mass_heal"),
        "reason": "クラッチ性能の高い聖ビルド — 蘇生 + 全体バリア + 全体回復",
        "min_count": 2,
    },
    {
        "skills": ("triple_shot", "endless_volley", "barrage"),
        "reason": "射手連射ビルド — 手数で押す鬼畜火力",
        "min_count": 2,
    },
    {
        "skills": ("power_strike", "multi_slash", "whirlwind", "executioner"),
        "reason": "近接攻撃スキル集中 — 常に何かしら火力が出せる",
        "min_count": 2,
    },
]

COUNTER_PAIRS = [
    ("purify", None, "デバフ/CC除去 — スロウ・耐性低下スキル全般へのカウンター"),
    ("evade", "blizzard", "回避スキルで設置型AoEをかわす"),
    ("evade", "thunder_strike", "回避スキルで設置型AoEをかわす"),
    ("evade", "flame_pillar", "回避スキルで設置型AoEをかわす"),
    ("evade", "ice_spike", "回避スキルで設置型AoEをかわす"),
    ("true_shot", None, "確定ダメージで防御バフ・バリアを貫通"),
    ("divine_shield", None, "ダメージ吸収バリアでburst耐性"),
    ("guardian_angel", None, "全体バリアでAoE burstを無効化"),
    ("martyrdom", None, "ダメージ肩代わりで味方の被burstを軽減"),
    ("sanctuary", None, "範囲持続回復でAoE継続ダメージを相殺"),
    ("blink", None, "瞬間移動でCC・burstの両方を回避"),
    ("executioner", None, "HP50%未満に追加ダメージ — タンクキラー"),
    ("seismic_stomp", None, "スロウIIIで近接の接近を妨害"),
    ("taunt", None, "敵の攻撃を強制 — ranged/archerメタ"),
    ("focused_shot", "evade", "必中の射撃で回避を貫通"),
    ("purify", "blizzard", "スロウIIを解除"),
    ("purify", "seismic_stomp", "スロウIIIを解除"),
    ("shield_wall", None, "耐性IIでCC耐性を得てburstを軽減"),
    ("fortress", None, "耐性IIIでより強力なCC耐性"),
    ("battle_cry", None, "攻撃力上昇 + 回復で継戦能力向上"),
    ("colossus", None, "自身回復 + burst + CCの万能スキル"),
]

ANTI_COMBO_PAIRS = [
    {
        "skills": ("charge", "blink"),
        "reason": "移動スキルの重複 — 両方あると枠の無駄になる可能性",
        "severity": "warning",
    },
    {
        "skills": ("blink", "evade"),
        "reason": "回避スキルの重複 — 必要に応じて選択を",
        "severity": "info",
    },
    {
        "skills": ("charge", "evade"),
        "reason": "移動スキルの重複",
        "severity": "info",
    },
    {
        "skills": ("colossus", "thunder_blast"),
        "reason": "高マナコストスキルの重複（50 + 70）— マナ管理困難",
        "severity": "warning",
    },
    {
        "skills": ("rain_of_arrows", "thunder_blast", "mass_heal"),
        "reason": "高マナ or 長CD の重複 — ローテーションが回らない",
        "severity": "warning",
        "min_count": 2,
    },
    {
        "skills": ("holy_resurrection", "guardian_angel"),
        "reason": "超長CDスキルの重複（180s + 60s）— 普段使えない枠が増える",
        "severity": "warning",
    },
    {
        "skills": ("first_aid", "holy_light", "sacrificial_light"),
        "reason": "単体ヒールの重複 — 役割がかぶる",
        "severity": "info",
        "min_count": 2,
    },
    {
        "skills": ("divine_shield", "guardian_angel"),
        "reason": "バリアスキルの重複 — 状況に応じて選択",
        "severity": "info",
    },
    {
        "skills": ("fortress", "shield_wall"),
        "reason": "防御バフの重複 — 重ねがけに意味がある場合とない場合がある",
        "severity": "info",
    },
    {
        "skills": ("battle_cry", "fortress", "colossus"),
        "reason": "ウォーリアー系バフ + 自己回復 + burst の好相性",
        "severity": "synergy",
    },
]


def analyze_role_combo(skills: list, player_skills: list) -> list:
    role_counts = defaultdict(int)
    for sid in player_skills:
        s = find_skill(skills, sid)
        if s:
            for r in s.roles:
                role_counts[r] += 1
    results = []
    roles_present = [r for r, c in role_counts.items() if c > 0]
    for (r1, r2), reason in ROLE_SYNERGY_PAIRS.items():
        if r1 in roles_present and r2 in roles_present:
            results.append(("synergy", f"ロール相性◯: {r1} × {r2} — {reason}"))
    for role, limit in ROLE_OVERLAP_WARNING.items():
        if role_counts[role] >= limit:
            results.append(("warning", f"ロール偏重⚠: {role}が{role_counts[role]}個 — バランス注意"))
    return results


def analyze_scaling_synergy(skills: list, player_skills: list) -> list:
    scaling_counts = defaultdict(int)
    for sid in player_skills:
        s = find_skill(skills, sid)
        if s:
            for sc in s.scalings:
                scaling_counts[sc] += 1
    results = []
    total = sum(scaling_counts.values())
    if scaling_counts.get("CDR_HEAVY", 0) >= 2:
        results.append(("synergy", f"高回転シナジー: CDR_HEAVY {scaling_counts['CDR_HEAVY']}個 — クールダウン短縮の恩恵大"))
    for sc, count in scaling_counts.items():
        if sc == "CDR_HEAVY":
            continue
        if count >= 3 and total >= 4:
            results.append(("synergy", f"統一スケーリング: {sc} {count}個 — ステータスの指向性が明確"))
    if scaling_counts.get("HYBRID", 0) > 0:
        if scaling_counts.get("PHYSICAL", 0) > 0 or scaling_counts.get("MAGICAL", 0) > 0:
            results.append(("synergy", f"HYBRID + 単一スケーリング: 両方の恩恵を受けられる"))
    pure_scalings = [s for s in scaling_counts if s != "CDR_HEAVY"]
    if len(pure_scalings) >= 2:
        has_cdr = scaling_counts.get("CDR_HEAVY", 0) > 0
        if not has_cdr:
            results.append(("warning", f"スケーリング混在⚠: {', '.join(pure_scalings)} — ビルドの方向性が分散"))
    return results


def analyze_element_synergy(skills: list, player_skills: list) -> list:
    element_counts = defaultdict(int)
    for sid in player_skills:
        s = find_skill(skills, sid)
        if s:
            elem = get_element(s.tags)
            element_counts[elem] += 1
    results = []
    present = [e for e, c in element_counts.items() if c > 0]
    if len(present) == 1:
        elem = present[0]
        comment = ELEMENT_SYNERGY.get(elem, "統一属性ビルド")
        results.append(("synergy", f"属性統一: {elem.upper()} — {comment}"))
    elif len(present) >= 2:
        for (e1, e2), reason in ELEMENT_ANTI_SYNERGY.items():
            if e1 in present and e2 in present:
                results.append(("warning", f"属性相反⚠: {reason}"))
        if len(present) >= 3:
            results.append(("warning", f"属性過多⚠: {len(present)}属性混在 — 一貫性なし"))
    return results


def analyze_combo_potential(skills: list, player_skills: list) -> list:
    skill_set = set(player_skills)
    results = []
    for combo in COMBO_PAIRS:
        combo_set = set(combo["skills"])
        overlap = combo_set & skill_set
        min_count = combo.get("min_count", 2)
        if len(overlap) >= min_count:
            matched = list(overlap)
            matched_names = []
            for sid in matched:
                s = find_skill(skills, sid)
                matched_names.append(s.display_name if s else sid)
            results.append(("combo", f"[コンボ] {combo['reason']} ({', '.join(matched_names)})"))
    return results


def analyze_anti_combo(skills: list, player_skills: list) -> list:
    skill_set = set(player_skills)
    results = []
    for anti in ANTI_COMBO_PAIRS:
        anti_set = set(anti["skills"])
        overlap = anti_set & skill_set
        min_count = anti.get("min_count", 2)
        if len(overlap) >= min_count:
            matched = list(overlap)
            matched_names = []
            for sid in matched:
                s = find_skill(skills, sid)
                matched_names.append(s.display_name if s else sid)
            if anti["severity"] == "synergy":
                results.append(("synergy", f"[シナジー] {anti['reason']} ({', '.join(matched_names)})"))
            else:
                severity_tag = "warning" if anti["severity"] == "warning" else "info"
                results.append((severity_tag, f"[非推奨{anti['severity']}] {anti['reason']} ({', '.join(matched_names)})"))
    return results


def analyze_counter_relationships(skills: list, player_skills: list) -> list:
    skill_set = set(player_skills)
    results = []
    # 敵側のスキルを仮定したカウンター分析も欲しい場合は、全スキルを敵と仮定
    all_skill_ids = set(s.id for s in skills)
    for skill_id, target_id, reason in COUNTER_PAIRS:
        if skill_id in skill_set:
            if target_id is None:
                s = find_skill(skills, skill_id)
                name = s.display_name if s else skill_id
                results.append(("counter", f"[カウンター◉] {name}: {reason}"))
            elif target_id in skill_set:
                s1 = find_skill(skills, skill_id)
                s2 = find_skill(skills, target_id)
                name1 = s1.display_name if s1 else skill_id
                name2 = s2.display_name if s2 else target_id
                results.append(("counter", f"[カウンター◉] {name1} → {name2}: {reason}"))
        # 逆方向: 自分のスキルが相手のカウンター対象になる場合
        if target_id and skill_id in all_skill_ids and target_id in skill_set:
            # target_id が味方スキルにあり、skill_id が敵のカウンターの場合
            if skill_id not in skill_set:
                s_enemy = find_skill(skills, skill_id)
                s_friend = find_skill(skills, target_id)
                if s_enemy and s_friend:
                    results.append(("counter", f"[被カウンター▼] {s_friend.display_name} → {s_enemy.display_name}: {reason}"))
    return results


def analyze_all(skills: list, player_skills: list) -> dict:
    results = {"synergy": [], "combo": [], "counter": [], "warning": [], "info": []}
    for analyzer in [analyze_role_combo, analyze_scaling_synergy, analyze_element_synergy]:
        for tag, msg in analyzer(skills, player_skills):
            results[tag].append(msg)
    for tag, msg in analyze_combo_potential(skills, player_skills):
        results[tag].append(msg)
    for tag, msg in analyze_anti_combo(skills, player_skills):
        results[tag].append(msg)
    for tag, msg in analyze_counter_relationships(skills, player_skills):
        results[tag].append(msg)
    return results


def format_results(results: dict, title: str = "分析結果"):
    lines = []
    lines.append(f"\n{'='*70}")
    lines.append(f"  {title}")
    lines.append(f"{'='*70}")
    sections = [
        ("combo", "【コンボ相性】"),
        ("synergy", "【シナジー】"),
        ("counter", "【カウンター関係】"),
        ("warning", "【注意点】"),
        ("info", "【情報】"),
    ]
    for key, header in sections:
        lines.append(f"\n  {header}")
        lines.append(f"  {'-'*60}")
        if results[key]:
            for msg in results[key]:
                lines.append(f"    • {msg}")
        else:
            lines.append(f"    （該当なし）")
    lines.append(f"\n{'='*70}")
    return "\n".join(lines)


def print_all_skills_summary(skills: list):
    elem_groups = defaultdict(list)
    for s in skills:
        elem_groups[get_element(s.tags)].append(s)
    cat_groups = defaultdict(list)
    for s in skills:
        cat_groups[get_category(s.tags)].append(s)
    lines = []
    lines.append(f"\n{'='*70}")
    lines.append(f"  全スキル {len(skills)} タグ別サマリー")
    lines.append(f"{'='*70}")
    lines.append(f"\n  元素属性別:")
    for elem in ["fire", "ice", "lightning", "holy", "physical"]:
        group = elem_groups.get(elem, [])
        if group:
            names = ", ".join(s.display_name for s in group)
            lines.append(f"    {elem.upper()} ({len(group)}): {names}")
    lines.append(f"\n  カテゴリ別:")
    for cat in ["warrior", "mage", "archer", "holy", "other"]:
        group = cat_groups.get(cat, [])
        if group:
            names = ", ".join(s.display_name for s in group)
            lines.append(f"    {cat.upper()} ({len(group)}): {names}")
    lines.append(f"\n  コスト分布:")
    costs = sorted([(s.display_name, s.mana_cost) for s in skills], key=lambda x: x[1])
    lines.append(f"    最小マナ: {costs[0][0]} ({costs[0][1]:.0f})")
    lines.append(f"    最大マナ: {costs[-1][0]} ({costs[-1][1]:.0f})")
    avg = sum(c for _, c in costs) / len(costs)
    lines.append(f"    平均マナ: {avg:.1f}")
    lines.append(f"\n{'='*70}")
    return "\n".join(lines)


def print_comprehensive_reference(skills: list):
    lines = []
    lines.append(f"\n{'='*70}")
    lines.append(f"  スキルビルド設計リファレンス")
    lines.append(f"{'='*70}")
    lines.append(f"\n  [クラス別コアスキル提案]")
    lines.append(f"  {'-'*60}")
    class_cores = {
        "メイジ (Magical)": {
            "core": ["arcane_bolt", "fireball", "ice_shard"],
            "burst": ["fire_nova", "thunder_blast", "thunder_strike"],
            "control": ["blizzard", "chain_lightning"],
            "mobility": ["blink", "evade"],
        },
        "メイジ (CDR)": {
            "core": ["fireball", "arcane_bolt", "ice_shard"],
            "sustain": ["chain_lightning", "lightning_storm"],
            "mobility": ["evade"],
        },
        "ウォーリアー (Physical)": {
            "core": ["power_strike", "multi_slash", "whirlwind"],
            "burst": ["colossus", "executioner"],
            "defense": ["shield_wall", "fortress"],
            "engage": ["charge", "seismic_stomp"],
        },
        "ウォーリアー (Tank)": {
            "core": ["shield_wall", "fortress", "taunt"],
            "burst": ["colossus", "hammer_slam"],
            "heal": ["battle_cry", "first_aid"],
        },
        "アーチャー (Physical)": {
            "core": ["power_shot", "triple_shot", "focused_shot"],
            "aoe": ["barrage", "explosion_arrow", "rain_of_arrows"],
            "burst": ["true_shot"],
            "mobility": ["evade"],
        },
        "アーチャー (連射)": {
            "core": ["endless_volley", "triple_shot", "barrage"],
            "burst": ["focused_shot", "true_shot"],
            "utility": ["evade"],
        },
        "ヒーラー/サポート": {
            "core": ["holy_light", "mass_heal", "renewal"],
            "defense": ["divine_shield", "guardian_angel"],
            "utility": ["purify", "sanctuary"],
            "revive": ["holy_resurrection"],
        },
        "パラディン (Hybrid)": {
            "core": ["fortress", "holy_light", "shield_wall"],
            "burst": ["colossus"],
            "support": ["battle_cry", "purify"],
        },
    }
    for build_name, slots in class_cores.items():
        lines.append(f"\n    ■ {build_name}")
        for role, skill_ids in slots.items():
            details = []
            for sid in skill_ids:
                s = find_skill(skills, sid)
                if s:
                    details.append(f"{s.display_name}({s.mana_cost:.0f}m/{s.cooldown_seconds:.0f}s)")
            lines.append(f"      {role}: {', '.join(details)}")
    return "\n".join(lines)


# ─────────────────────────────────────────────
# 全スキルペアワイズ分析
# ─────────────────────────────────────────────

def generate_synergy_matrix(skills: list) -> list:
    """すべてのスキルペアの相性スコアとコメントを生成"""
    pairs = []
    for i in range(len(skills)):
        for j in range(i + 1, len(skills)):
            a, b = skills[i], skills[j]
            score = 0
            comments = []

            # Role synergy
            a_roles = set(a.roles)
            b_roles = set(b.roles)
            for (r1, r2), reason in ROLE_SYNERGY_PAIRS.items():
                if (r1 in a_roles and r2 in b_roles) or (r2 in a_roles and r1 in b_roles):
                    score += 1
                    comments.append(f"ロール相性: {reason}")

            # Scaling match
            a_sc = set(a.scalings)
            b_sc = set(b.scalings)
            common_sc = a_sc & b_sc
            if common_sc and common_sc != {"CDR_HEAVY"}:
                score += 1
                comments.append(f"スケーリング一致: {', '.join(common_sc)}")
            if "CDR_HEAVY" in a_sc and "CDR_HEAVY" in b_sc:
                score += 1
                comments.append("両方CDR_HEAVYで高回転シナジー")

            # Element match
            elem_a = get_element(a.tags)
            elem_b = get_element(b.tags)
            if elem_a == elem_b:
                score += 1
                comments.append(f"属性一致: {elem_a.upper()}")

            # Element anti-synergy
            if (elem_a, elem_b) in ELEMENT_ANTI_SYNERGY:
                score -= 1
                comments.append(ELEMENT_ANTI_SYNERGY[(elem_a, elem_b)])
            elif (elem_b, elem_a) in ELEMENT_ANTI_SYNERGY:
                score -= 1
                comments.append(ELEMENT_ANTI_SYNERGY[(elem_b, elem_a)])

            # Category match
            cat_a = get_category(a.tags)
            cat_b = get_category(b.tags)
            if cat_a == cat_b:
                score += 1
                comments.append(f"カテゴリ一致: {cat_a.upper()}")

            # Same tags (elemental schools)
            a_tag_set = set(a.tags)
            b_tag_set = set(b.tags)
            school_tags = {"fire", "ice", "lightning", "holy", "ranged", "melee", "magic", "heal", "defense", "barrier", "mobility", "aoe", "rapid"}
            common_school = a_tag_set & b_tag_set & school_tags
            for tag in common_school:
                score += 1
                comments.append(f"タグ一致: {tag}")

            # Mana cost check (both high cost = anti-synergy)
            if a.mana_cost >= 45 and b.mana_cost >= 45:
                score -= 1
                comments.append(f"高マナ重複（{a.mana_cost:.0f}+{b.mana_cost:.0f}）")

            pairs.append({
                "skill_a": a.id,
                "skill_b": b.id,
                "name_a": a.display_name,
                "name_b": b.display_name,
                "score": score,
                "comments": comments,
            })
    return pairs


def analyze_pvp_team_composition(skills: list):
    """PvP想定のチーム相性分析"""
    lines = []
    lines.append(f"\n{'='*70}")
    lines.append(f"  PvP 対抗相性マップ")
    lines.append(f"{'='*70}")
    
    # 戦術カテゴリごとに分類
    categories = {
        "Burst Mage": [],
        "Control Mage": [],
        "Warrior Engage": [],
        "Warrior Tank": [],
        "Archer Sniper": [],
        "Archer Harass": [],
        "Healer": [],
        "Support/Utility": [],
    }
    
    for s in skills:
        roles = set(s.roles)
        tags = set(s.tags)
        if "holy" in tags and "heal" in tags:
            categories["Healer"].append(s)
        elif "holy" in tags:
            categories["Support/Utility"].append(s)
        elif "warrior" in tags and "defense" in tags:
            categories["Warrior Tank"].append(s)
        elif "warrior" in tags and "heavy" in tags:
            categories["Warrior Engage"].append(s)
        elif "archer" in tags and ("focused" in tags or "true_shot" in tags):
            categories["Archer Sniper"].append(s)
        elif "archer" in tags:
            categories["Archer Harass"].append(s)
        elif "magic" in tags and "control" in roles:
            categories["Control Mage"].append(s)
        elif "magic" in tags:
            categories["Burst Mage"].append(s)
        else:
            categories["Support/Utility"].append(s)
    
    for cat_name, cat_skills in categories.items():
        if cat_skills:
            names = ", ".join(s.display_name for s in cat_skills)
            lines.append(f"\n  ■ {cat_name} ({len(cat_skills)})")
            lines.append(f"    {names}")

    # 対抗関係サマリー
    lines.append(f"\n  【対抗関係サマリー】")
    lines.append(f"  {'-'*60}")
    counter_summary = [
        ("Healer/Shielder", "Burst Mage/Warrior Engage", "ヒール/バリアでburstを無効化"),
        ("Archer Sniper", "Healer/Utility", "長距離スナイプで後衛を先に落とす"),
        ("Control Mage", "Warrior Engage", "スロウ/CCで接近を妨害"),
        ("Warrior Tank", "Archer Sniper", "耐久力でスナイプを耐える"),
        ("Purify user", "Control Mage", "浄化でCCを解除しカウンター"),
        ("Blink user", "Control Mage", "瞬間移動でAoE/CCを回避"),
        ("TrueShot user", "Warrior Tank/Shielder", "確定ダメージで防御無視"),
        ("Executioner user", "Warrior Tank", "HP50%以下追加ダメでタンクキラー"),
        ("Mobility build", "AoE control build", "機動力で設置AoEをかわす"),
    ]
    for attacker, target, reason in counter_summary:
        lines.append(f"    {attacker:25s} ▶ {target:25s} | {reason}")

    lines.append(f"\n{'='*70}")
    return "\n".join(lines)


# ─────────────────────────────────────────────
# 可視化
# ─────────────────────────────────────────────

OUTPUT_DIR = Path(__file__).parent / "viz"


def compute_full_matrix(skills):
    """全スキルのNxN相性スコア行列を生成"""
    n = len(skills)
    matrix = np.zeros((n, n), dtype=int)
    for i in range(n):
        for j in range(n):
            if i == j:
                matrix[i][j] = 0
                continue
            a, b = skills[i], skills[j]
            score = 0
            a_roles = set(a.roles)
            b_roles = set(b.roles)
            for (r1, r2), _ in ROLE_SYNERGY_PAIRS.items():
                if (r1 in a_roles and r2 in b_roles) or (r2 in a_roles and r1 in b_roles):
                    score += 1
            a_sc = set(a.scalings)
            b_sc = set(b.scalings)
            common_sc = a_sc & b_sc
            if common_sc and common_sc != {"CDR_HEAVY"}:
                score += 1
            if "CDR_HEAVY" in a_sc and "CDR_HEAVY" in b_sc:
                score += 1
            elem_a = get_element(a.tags)
            elem_b = get_element(b.tags)
            if elem_a == elem_b:
                score += 1
            if (elem_a, elem_b) in ELEMENT_ANTI_SYNERGY or (elem_b, elem_a) in ELEMENT_ANTI_SYNERGY:
                score -= 1
            if get_category(a.tags) == get_category(b.tags):
                score += 1
            a_tag_set = set(a.tags)
            b_tag_set = set(b.tags)
            school_tags = {"fire", "ice", "lightning", "holy", "ranged", "melee", "magic", "heal", "defense", "barrier", "mobility", "aoe", "rapid"}
            common_school = a_tag_set & b_tag_set & school_tags
            score += len(common_school)
            if a.mana_cost >= 45 and b.mana_cost >= 45:
                score -= 1
            matrix[i][j] = score
    return matrix


def get_category_color(cat):
    colors = {"warrior": "#E74C3C", "mage": "#9B59B6", "archer": "#F39C12", "holy": "#2ECC71", "other": "#95A5A6"}
    return colors.get(cat, "#95A5A6")


def plot_heatmap(skills, matrix, title, filename):
    """ヒートマップを描画して保存"""
    n = len(skills)
    labels = [s.display_name for s in skills]
    categories = [get_category(s.tags) for s in skills]

    fig, ax = plt.subplots(figsize=(28, 24))
    vmax = max(abs(matrix.min()), abs(matrix.max()))
    im = ax.imshow(matrix, cmap="RdYlGn", vmin=-vmax, vmax=vmax, aspect="auto")

    ax.set_xticks(range(n))
    ax.set_yticks(range(n))
    ax.set_xticklabels(labels, fontsize=5, rotation=90)
    ax.set_yticklabels(labels, fontsize=5)

    # カテゴリ別のカラーバー
    cat_colors = [get_category_color(c) for c in categories]
    for i, color in enumerate(cat_colors):
        ax.add_patch(plt.Rectangle((-0.5, i - 0.5), 0.3, 1, color=color, clip_on=False, transform=ax.get_yaxis_transform()))
        ax.add_patch(plt.Rectangle((i - 0.5, n - 0.5), 1, 0.3, color=color, clip_on=False, transform=ax.get_xaxis_transform()))

    # 凡例
    legend_elements = [
        plt.Rectangle((0, 0), 1, 1, color=color, label=cat.upper())
        for cat, color in [("warrior", "#E74C3C"), ("mage", "#9B59B6"), ("archer", "#F39C12"), ("holy", "#2ECC71"), ("other", "#95A5A6")]
    ]
    ax.legend(handles=legend_elements, loc="upper left", fontsize=8, title="カテゴリ")

    plt.colorbar(im, ax=ax, label="相性スコア", shrink=0.6)
    ax.set_title(title, fontsize=14, pad=20)
    plt.tight_layout()
    output_path = OUTPUT_DIR / filename
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    fig.savefig(output_path, dpi=200)
    plt.close(fig)
    print(f"  保存: {output_path}")


def plot_clustered_matrix(skills, matrix, title, filename):
    """カテゴリ順にソートしたクラスタ化ヒートマップ"""
    n = len(skills)
    categories_order = ["warrior", "mage", "archer", "holy", "other"]
    cat_skills = defaultdict(list)
    for s in skills:
        cat_skills[get_category(s.tags)].append(s)

    ordered = []
    for cat in categories_order:
        ordered.extend(cat_skills.get(cat, []))
    # 元の順番→新しい順番のインデックスマッピング
    idx_map = {s.id: i for i, s in enumerate(ordered)}
    orig_indices = [next(i for i, s in enumerate(skills) if s.id == sid) for sid in (s.id for s in ordered)]
    clustered = matrix[np.ix_(orig_indices, orig_indices)]

    labels = [s.display_name for s in ordered]
    categories = [get_category(s.tags) for s in ordered]

    fig, ax = plt.subplots(figsize=(28, 24))
    vmax = max(abs(clustered.min()), abs(clustered.max()))
    im = ax.imshow(clustered, cmap="RdYlGn", vmin=-vmax, vmax=vmax, aspect="auto")

    ax.set_xticks(range(n))
    ax.set_yticks(range(n))
    ax.set_xticklabels(labels, fontsize=5, rotation=90)
    ax.set_yticklabels(labels, fontsize=5)

    # カテゴリ境界線
    cat_counts = {cat: len(cat_skills.get(cat, [])) for cat in categories_order}
    cumsum = 0
    for cat in categories_order:
        cnt = cat_counts[cat]
        if cnt > 0:
            color = get_category_color(cat)
            ax.axhline(y=cumsum - 0.5, color="black", linewidth=0.5)
            ax.axvline(x=cumsum - 0.5, color="black", linewidth=0.5)
            # カテゴリラベル
            mid = cumsum + cnt / 2
            ax.text(-2.5, mid, cat.upper(), fontsize=7, fontweight="bold", va="center", ha="right", color=color)
            ax.text(mid, n + 1.5, cat.upper(), fontsize=7, fontweight="bold", ha="center", va="bottom", color=color, rotation=0)
            cumsum += cnt
    ax.axhline(y=n - 0.5, color="black", linewidth=0.5)
    ax.axvline(x=n - 0.5, color="black", linewidth=0.5)

    plt.colorbar(im, ax=ax, label="相性スコア", shrink=0.6)
    ax.set_title(title, fontsize=14, pad=20)
    plt.tight_layout()
    output_path = OUTPUT_DIR / filename
    fig.savefig(output_path, dpi=200)
    plt.close(fig)
    print(f"  保存: {output_path}")


def plot_category_submatrices(skills, matrix):
    """カテゴリ別のサブマトリックスを個別に描画"""
    categories_order = ["warrior", "mage", "archer", "holy", "other"]
    cat_skills = defaultdict(list)
    for s in skills:
        cat_skills[get_category(s.tags)].append(s)

    for cat in categories_order:
        group = cat_skills.get(cat, [])
        if len(group) < 2:
            continue
        indices = [next(i for i, s in enumerate(skills) if s.id == s2.id) for s2 in group]
        sub = matrix[np.ix_(indices, indices)]
        labels = [s.display_name for s in group]
        color = get_category_color(cat)

        fig, ax = plt.subplots(figsize=(max(6, len(group) * 0.8), max(6, len(group) * 0.8)))
        vmax = max(abs(sub.min()), abs(sub.max())) or 1
        im = ax.imshow(sub, cmap="RdYlGn", vmin=-vmax, vmax=vmax, aspect="auto")

        ax.set_xticks(range(len(group)))
        ax.set_yticks(range(len(group)))
        ax.set_xticklabels(labels, fontsize=7, rotation=45, ha="right")
        ax.set_yticklabels(labels, fontsize=7)

        for i in range(len(group)):
            for j in range(len(group)):
                ax.text(j, i, str(sub[i][j]), ha="center", va="center", fontsize=6,
                        color="white" if abs(sub[i][j]) > vmax * 0.5 else "black")

        plt.colorbar(im, ax=ax, label="相性スコア", shrink=0.8)
        ax.set_title(f"{cat.upper()} ({len(group)}スキル)", fontsize=12)
        plt.tight_layout()
        output_path = OUTPUT_DIR / f"submatrix_{cat}.png"
        fig.savefig(output_path, dpi=200)
        plt.close(fig)
        print(f"  保存: {output_path}")


def plot_multi_synergy_bars(skills, matrix):
    """各スキルの平均・最大・最小相性スコアを棒グラフに"""
    n = len(skills)
    names = [s.display_name for s in skills]
    categories = [get_category(s.tags) for s in skills]
    cat_color_map = {"warrior": "#E74C3C", "mage": "#9B59B6", "archer": "#F39C12", "holy": "#2ECC71", "other": "#95A5A6"}
    colors = [cat_color_map.get(c, "#95A5A6") for c in categories]

    means = [np.mean([matrix[i][j] for j in range(n) if j != i]) for i in range(n)]
    maxs = [max(matrix[i][j] for j in range(n) if j != i) for i in range(n)]
    mins = [min(matrix[i][j] for j in range(n) if j != i) for i in range(n)]

    fig, axes = plt.subplots(3, 1, figsize=(28, 18), sharex=True)

    axes[0].bar(names, means, color=colors)
    axes[0].set_ylabel("平均相性スコア", fontsize=10)
    axes[0].set_title("各スキルの他スキルとの平均相性", fontsize=12)
    axes[0].tick_params(axis="x", rotation=90, labelsize=5)
    axes[0].axhline(y=0, color="black", linewidth=0.5)

    axes[1].bar(names, maxs, color=colors)
    axes[1].set_ylabel("最大相性スコア", fontsize=10)
    axes[1].set_title("各スキルの最高相性パートナー", fontsize=12)
    axes[1].tick_params(axis="x", rotation=90, labelsize=5)
    axes[1].axhline(y=0, color="black", linewidth=0.5)

    axes[2].bar(names, mins, color=colors)
    axes[2].set_ylabel("最小相性スコア", fontsize=10)
    axes[2].set_title("各スキルの最低相性パートナー", fontsize=12)
    axes[2].tick_params(axis="x", rotation=90, labelsize=5)
    axes[2].axhline(y=0, color="black", linewidth=0.5)

    legend_elements = [
        plt.Rectangle((0, 0), 1, 1, color=color, label=cat.upper())
        for cat, color in [("warrior", "#E74C3C"), ("mage", "#9B59B6"), ("archer", "#F39C12"), ("holy", "#2ECC71"), ("other", "#95A5A6")]
    ]
    axes[0].legend(handles=legend_elements, loc="upper right", fontsize=8)

    plt.tight_layout()
    output_path = OUTPUT_DIR / "synergy_bars.png"
    fig.savefig(output_path, dpi=200)
    plt.close(fig)
    print(f"  保存: {output_path}")


def plot_network_graph(skills, matrix):
    """相性の高いペアをエッジで結んだネットワーク図"""
    try:
        import networkx as nx
    except ImportError:
        print("  networkx がインストールされていません。スキップします。")
        return

    n = len(skills)
    G = nx.Graph()
    for s in skills:
        G.add_node(s.id, label=s.display_name, category=get_category(s.tags))

    # 高相性エッジ (スコア >= 3)
    cat_color_map = {"warrior": "#E74C3C", "mage": "#9B59B6", "archer": "#F39C12", "holy": "#2ECC71", "other": "#95A5A6"}
    for i in range(n):
        for j in range(i + 1, n):
            if matrix[i][j] >= 3:
                G.add_edge(skills[i].id, skills[j].id, weight=matrix[i][j])

    if len(G.edges) == 0:
        print("  ネットワーク: 高相性エッジがありません")
        return

    pos = nx.spring_layout(G, k=2, iterations=50, seed=42)
    fig, ax = plt.subplots(figsize=(20, 16))

    node_categories = [G.nodes[n]["category"] for n in G.nodes]
    node_colors = [cat_color_map.get(c, "#95A5A6") for c in node_categories]
    node_labels = {n: G.nodes[n]["label"] for n in G.nodes}

    edge_weights = [G.edges[e]["weight"] for e in G.edges]
    edge_widths = [max(0.5, w * 0.8) for w in edge_weights]
    edge_colors = [plt.cm.RdYlGn(w / max(edge_weights)) for w in edge_weights]

    nx.draw_networkx_nodes(G, pos, node_color=node_colors, node_size=300, ax=ax)
    nx.draw_networkx_labels(G, pos, labels=node_labels, font_size=6, ax=ax)
    nx.draw_networkx_edges(G, pos, width=edge_widths, edge_color=edge_colors, alpha=0.6, ax=ax)

    ax.set_title("スキル相性ネットワーク（スコア3以上のエッジ）", fontsize=14)
    ax.axis("off")

    legend_elements = [
        plt.Rectangle((0, 0), 1, 1, color=color, label=cat.upper())
        for cat, color in [("warrior", "#E74C3C"), ("mage", "#9B59B6"), ("archer", "#F39C12"), ("holy", "#2ECC71"), ("other", "#95A5A6")]
    ]
    ax.legend(handles=legend_elements, loc="upper right", fontsize=8)

    plt.tight_layout()
    output_path = OUTPUT_DIR / "network_graph.png"
    fig.savefig(output_path, dpi=200)
    plt.close(fig)
    print(f"  保存: {output_path}")


def generate_all_visualizations(skills):
    """全可視化を一括生成"""
    print(f"\n{'='*70}")
    print(f"  スキル相性可視化を生成中...")
    print(f"{'='*70}")

    matrix = compute_full_matrix(skills)

    print(f"\n  1/5 全体ヒートマップ (47x47)")
    plot_heatmap(skills, matrix, "全スキル相性ヒートマップ", "heatmap_full.png")

    print(f"\n  2/5 カテゴリクラスタ化ヒートマップ")
    plot_clustered_matrix(skills, matrix, "カテゴリ別クラスタ化相性マップ", "heatmap_clustered.png")

    print(f"\n  3/5 カテゴリ別サブマトリックス")
    plot_category_submatrices(skills, matrix)

    print(f"\n  4/5 相性スコア棒グラフ")
    plot_multi_synergy_bars(skills, matrix)

    print(f"\n  5/5 ネットワーク図")
    plot_network_graph(skills, matrix)

    print(f"\n  {'='*70}")
    print(f"  完了: 全画像を {OUTPUT_DIR.resolve()} に保存しました")
    print(f"  {'='*70}")


def main():
    skills = load_skills()
    if not skills:
        print("スキルデータが見つかりません。")
        return

    if len(sys.argv) == 1:
        skill_index = {s.id: s for s in skills}
        print(f"\n全 {len(skills)} スキルが読み込まれました。")
        for s in sorted(skills, key=lambda x: x.id):
            print(f"  {s.id:25s} | {s.display_name}")
        print("\n分析したいスキルIDをスペース区切りで入力:")
        print("  all      全スキルサマリー + ビルドリファレンス")
        print("  ref      ビルド設計リファレンス")
        print("  summary  全スキルタグ別サマリー")
        print("  matrix   全ペアワイズ相性スコア")
        print("  pvp      PvP対抗相性マップ")
        print("  viz      全可視化を生成 (heatmap/network/bars)")
        line = input("> ").strip()
        if not line:
            return
        elif line == "all":
            print(print_all_skills_summary(skills))
            print(print_comprehensive_reference(skills))
            return
        elif line == "ref":
            print(print_comprehensive_reference(skills))
            return
        elif line == "summary":
            print(print_all_skills_summary(skills))
            return
        elif line == "pvp":
            print(analyze_pvp_team_composition(skills))
            return
        elif line == "viz":
            generate_all_visualizations(skills)
            return
        elif line == "matrix":
            pairs = generate_synergy_matrix(skills)
            sorted_pairs = sorted(pairs, key=lambda x: -x["score"])
            print(f"\n{'='*70}")
            print(f"  全スキルペア相性スコア（{len(sorted_pairs)}ペア）")
            print(f"{'='*70}")
            print(f"\n  【高相性 TOP 20】")
            top = [p for p in sorted_pairs if p["score"] >= 2][:20]
            for p in top:
                print(f"\n    {p['name_a']:15s} × {p['name_b']:15s} | スコア: {p['score']}")
                for c in p["comments"][:3]:
                    print(f"      └ {c}")
            print(f"\n  【低相性 (アンチ) BOTTOM 10】")
            bottom = [p for p in sorted_pairs if p["score"] < 0][:10]
            if not bottom:
                # スコア0のものから表示
                bottom = [p for p in sorted_pairs if p["score"] <= 0][:10]
            for p in bottom:
                print(f"\n    {p['name_a']:15s} × {p['name_b']:15s} | スコア: {p['score']}")
                for c in p["comments"][:3]:
                    print(f"      └ {c}")
            print()
            return
        else:
            player_skills = line.split()
            unknown = [s for s in player_skills if s not in skill_index]
            if unknown:
                print(f"\n不明なスキルID: {', '.join(unknown)}")
                return
    else:
        mode = sys.argv[1]
        if mode == "all":
            print(print_all_skills_summary(skills))
            print(print_comprehensive_reference(skills))
            return
        elif mode == "ref":
            print(print_comprehensive_reference(skills))
            return
        elif mode == "summary":
            print(print_all_skills_summary(skills))
            return
        elif mode == "pvp":
            print(analyze_pvp_team_composition(skills))
            return
        elif mode == "viz":
            generate_all_visualizations(skills)
            return
        elif mode == "matrix":
            pairs = generate_synergy_matrix(skills)
            sorted_pairs = sorted(pairs, key=lambda x: -x["score"])
            print(f"\n{'='*70}")
            print(f"  全スキルペア相性スコア（{len(sorted_pairs)}ペア）")
            print(f"{'='*70}")
            print(f"\n  【高相性 TOP 20】")
            top = [p for p in sorted_pairs if p["score"] >= 2][:20]
            for p in top:
                print(f"\n    {p['name_a']:15s} × {p['name_b']:15s} | スコア: {p['score']}")
                for c in p["comments"][:3]:
                    print(f"      └ {c}")
            print(f"\n  【低相性 (アンチ) BOTTOM 10】")
            bottom = [p for p in sorted_pairs if p["score"] < 0][:10]
            if not bottom:
                bottom = [p for p in sorted_pairs if p["score"] <= 0][:10]
            for p in bottom:
                print(f"\n    {p['name_a']:15s} × {p['name_b']:15s} | スコア: {p['score']}")
                for c in p["comments"][:3]:
                    print(f"      └ {c}")
            print()
            return
        else:
            player_skills = sys.argv[1:]

    results = analyze_all(skills, player_skills)
    print(f"\n  【選択スキル】")
    for sid in player_skills:
        s = find_skill(skills, sid)
        if s:
            roles_str = ", ".join(s.roles)
            scalings_str = ", ".join(s.scalings) if s.scalings else "-"
            print(f"    {s.display_name} ({s.id}) | ロール: {roles_str} | スケール: {scalings_str} | {s.mana_cost:.0f}m/{s.cooldown_seconds:.0f}s")
    print(format_results(results))


if __name__ == "__main__":
    main()
