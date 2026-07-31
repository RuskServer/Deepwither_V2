package com.ruskserver.deepwither_V2.modules.item.set;

import java.util.List;
import java.util.Set;

public enum EquipmentSet {
    GHOUL_ATTACKER(
            "ghoul_attacker",
            "グール・アタッカー",
            Set.of(
                    "ghoul_attacker_hood",
                    "ghoul_attacker_harness",
                    "ghoul_attacker_wrappings",
                    "ghoul_attacker_greaves"
            ),
            List.of(
                    "[2] 腐肉喰らい: モブへ与えた物理ダメージの2%を回復する。",
                    "[4] 飢餓の狂騒: モブ撃破時、6秒間攻撃力+15%・移動速度+10%。"
            )
    ),
    GHOUL_DEFENDER(
            "ghoul_defender",
            "グール・ディフェンダー",
            Set.of(
                    "ghoul_defender_mask",
                    "ghoul_defender_carapace",
                    "ghoul_defender_legguards",
                    "ghoul_defender_sabatons"
            ),
            List.of(
                    "[2] 壊死皮膜: 受ける物理ダメージを6%軽減する。",
                    "[4] 死肉再生: 体力35%以下で回復し、5秒間防御力+15%。"
            )
    );

    private final String id;
    private final String displayName;
    private final Set<String> itemIds;
    private final List<String> bonusDescriptions;

    EquipmentSet(String id, String displayName, Set<String> itemIds, List<String> bonusDescriptions) {
        this.id = id;
        this.displayName = displayName;
        this.itemIds = itemIds;
        this.bonusDescriptions = bonusDescriptions;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<String> getItemIds() {
        return itemIds;
    }

    public List<String> getBonusDescriptions() {
        return bonusDescriptions;
    }

    public static EquipmentSet fromId(String id) {
        if (id == null) return null;
        for (EquipmentSet set : values()) {
            if (set.id.equals(id)) return set;
        }
        return null;
    }
}
