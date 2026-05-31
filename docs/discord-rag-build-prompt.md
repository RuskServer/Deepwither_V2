# Discord RAG + LLM ビルド構築プロンプトテンプレート

以下は Discord のチャットボット（RAG + LLM）に投げる「魔法防御重視ビルド構築」タスクのプロンプト例です。
実際のゲーム内数値を含めているので、LLMが具体的な計算と根拠を持って回答できます。

---

## プロンプト

```
# タスク: Echoes of Aether 魔法防御特化ビルドの構築

あなたは Echoes of Aether のビルド最適化アドバイザーです。
以下のゲームシステム情報を元に、「物理・魔法両面で高い耐久力を誇る魔法防御特化ビルド」を提案してください。

## ステータスシステム

### 最終値の計算式
```
finalValue = sumAdditive × (1.0 + sumMultiplicative)
```
- ADDITIVE: 装備の基礎値など、単純加算
- MULTIPLICATIVE: スキルツリーや属性ボーナスなど、乗算（値は 0.05 なら +5%）

### 防御によるダメージ軽減式
```
軽減後ダメージ = 元ダメージ × (250 / (250 + 防御値))
```
- 物理ダメージ → 対象の DEFENSE で軽減
- 魔法ダメージ → 対象の MAGIC_DEFENSE で軽減
- TRUE_DAMAGE / ENVIRONMENTAL は防御無視

### 利用可能なステータス(StatType)
| 識別子 | 日本語名 | 効果 |
|--------|---------|------|
| ATTACK_DAMAGE | 攻撃力 | 物理ダメージの基礎値 |
| DEFENSE | 防御力 | 物理軽減に使用 |
| MAGIC_DAMAGE | 魔法攻撃力 | 魔法ダメージの基礎値 |
| MAGIC_DEFENSE | 魔法防御力 | 魔法軽減に使用 |
| CRITICAL_CHANCE | クリティカル率 | 0~100% |
| CRITICAL_DAMAGE | クリティカルダメージ | 150%基準+加算値÷100 |
| HEALTH | 最大HP | 体力 |
| MAX_MANA | 最大マナ | マナ最大値 |
| ATTACK_SPEED | 攻撃速度 | 秒間攻撃回数 |
| SPEED | 移動速度 | 移動倍率 |
| COOLDOWN_REDUCTION | クールタイム短縮 | 短縮率(%) |
| FIRE_DAMAGE | 火属性攻撃力 | 魔法攻撃に固定加算 |
| ICE_DAMAGE | 氷属性攻撃力 | 同上 |
| LIGHTNING_DAMAGE | 雷属性攻撃力 | 同上 |

## 装備一覧（実際の数値）

### Aetherium Bulwark セット (EPIC) — 魔法防御+物理防御の高バランス
| 部位 | DEFENSE | MAGIC_DEFENSE | 追加効果 |
|------|---------|---------------|---------|
| 胴(AetheriumBulwarkChestplate) | 68.0 | 62.0 | +40 HP, -2% 移動速度 |
| 脚(AetheriumBulwarkLeggings) | 54.0 | 51.0 | +35 HP, -2% 移動速度 |
| 頭(AetheriumBulwarkHelmet) | 32.0 | 28.0 | — |
| 足(AetheriumBulwarkBoots) | 30.0 | 28.0 | — |
| **合計** | **184.0** | **169.0** | **+75 HP, -4% 速度** |

### RC Ruby Aegis セット (LEGENDARY) — 魔法防御特化
| 部位 | DEFENSE | MAGIC_DEFENSE | 追加効果 |
|------|---------|---------------|---------|
| 胴(RCRubyAegisChestplate) | 22.0 | 42.0 | — |
| 脚(RCRubyAegisLeggings) | 18.0 | 34.0 | — |
| 頭(RCRubyAegisHood) | 12.0 | 28.0 | — |
| 足(RCRubyAegisBoots) | 14.0 | 26.0 | — |
| **合計** | **66.0** | **130.0** | **—** |

### Glacial Fortress セット (EPIC) — 物理防御極振り
| 部位 | DEFENSE | MAGIC_DEFENSE | 追加効果 |
|------|---------|---------------|---------|
| 胴(GlacialFortressChestplate) | 92.0 | — | +60 HP, +14 攻撃力 |
| 脚(GlacialFortressLeggings) | 72.0 | — | — |
| 頭(GlacialFortressHelmet) | 38.0 | — | — |
| 足(GlacialFortressBoots) | 36.0 | — | — |
| **合計** | **238.0** | **0** | **+60 HP, +14 攻撃力** |

### Moon Shadow セット (UNCOMMON) — 魔法防御寄り軽装
| 部位 | DEFENSE | MAGIC_DEFENSE | 追加効果 |
|------|---------|---------------|---------|
| 胴(MoonShadowUpper) | 8.0 | 30.0 | — |
| 脚(MoonShadowLower) | 6.0 | 24.0 | — |
| 頭(MoonShadowHood) | 5.0 | 20.0 | — |
| 足(MoonShadowBoots) | 4.0 | 18.0 | — |
| **合計** | **23.0** | **92.0** | **—** |

### Moonveil セット (UNCOMMON) — 軽量魔法防御+マナ
| 部位 | DEFENSE | MAGIC_DEFENSE | 追加効果 |
|------|---------|---------------|---------|
| 胴(MoonveilTunic) | 5.0 | 28.0 | +30 MAX_MANA |
| 脚(MoonveilLeggings) | 4.0 | 22.0 | — |
| 頭(MoonveilHood) | 3.0 | 18.0 | — |
| 足(MoonveilBoots) | 2.0 | 16.0 | — |
| **合計** | **14.0** | **84.0** | **+30 MAX_MANA** |

### 武器例
| 武器 | 主ステータス |
|------|------------|
| RequiemBurstStaff (EPIC Wand) | MAGIC_DAMAGE 35.0, FIRE_DAMAGE 8.0, CRITICAL_CHANCE 7.0, CRITICAL_DAMAGE 180.0, COOLDOWN_REDUCTION 3.0 |
| StarterWand (COMMON) | MAGIC_DAMAGE 25.0, MAX_MANA 100.0, ATTACK_SPEED 1.2 |

### Modifier ランダムボーナス
各装備生成時、各基礎値に20%の確率で +1%〜+10% のランダムボーナスが付与されます。

## スキルツリーの防御パッシブ

### ウォリアー系
| ノード名 | 効果(Lv1時) | 最大Lv |
|---------|------------|--------|
| 堅牢 I (toughness_1) | DEFENSE +5% MULTIPLICATIVE | 3 |
| 堅牢 II (toughness_2) | DEFENSE +8% MULTIPLICATIVE | 3 |
| 頑強 (sturdy) | DEFENSE +5% MULTIPLICATIVE | 3 |

→ ウォリアーツリー最大で DEFENSE × (1 + 0.05×3 + 0.08×3 + 0.05×3) = DEFENSE × 1.54

### メイジ系
| ノード名 | 効果(Lv1時) | 最大Lv |
|---------|------------|--------|
| 氷の鎧 (frost_armor) | DEFENSE +5% MULTIPLICATIVE | 3 |

→ メイジツリー最大で DEFENSE × 1.15

### VIT(バイタリティ) 属性
VIT 1pt につき DEFENSE +0.5% MULTIPLICATIVE

## 依頼内容

以下の条件で、**最も高い魔法防御力(MAGIC_DEFENSE)を達成する装備構成とステータス配分**を提案してください。

**条件:**
1. 防具4部位（頭・胴・脚・足）の装備セットを選択
2. 武器1枠（任意）
3. スキルツリーのパッシブノードを割り振る（ウォリアー系 / メイジ系 / 両方）
4. ステータス割り振り（全プレイヤーはレベルアップで自由に振れる想定、VIT は防御乗算）

**出力してほしいこと:**
- 最終的な DEFENSE / MAGIC_DEFENSE / HEALTH / MAX_MANA の数値
- ダメージ軽減率（物理・魔法それぞれ）
  - 例: MAGIC_DEFENSE 200 の場合 → 250/(250+200) ≈ 0.556 → 44.4%軽減
- 推奨武器とその理由
- 代替案（コストや入手難易度を考慮した現実的な構成）
- トレードオフ（速度低下や火力低下など）

**計算過程も明示してください。**
```

---

## 使い方

1. Discord bot の RAG にこのプロンプトとコードベースの該当ファイルをナレッジとして登録
2. ユーザーが `!build 魔法防御` や `!optimize tank` などと打つと、このプロンプトがコンテキストとして注入される
3. LLM が実際の数値を参照して計算・提案を返す

## RAG用ナレッジとして登録すべきファイル

| 役割 | ファイルパス |
|------|------------|
| ステータス定義 | `src/main/java/.../core/stat/StatType.java` |
| 計算式 | `src/main/java/.../modules/stat/StatProfile.java` |
| 軽減式 | `src/main/java/.../combat/damage/phases/DamagePhase.java` (Defense相) |
| 防具一覧 | `src/main/java/.../modules/item/definitions/armor/*.java` |
| 武器一覧 | `src/main/java/.../modules/item/definitions/*.java` |
| スキルツリー | `src/main/java/.../skilltree/definitions/WarriorSkillTree.java`, `MageSkillTree.java` |
| Modifier管理 | `src/main/java/.../modules/item/modifier/ModifierManager.java` |
