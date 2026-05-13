package com.ruskserver.deepwither_V2.modules.fusion.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.fusion.api.FusionRecipe;

import java.util.Map;

@Component
public class ExampleFusionRecipe implements FusionRecipe {

    @Override
    public String getId() {
        return "example_fusion_recipe";
    }

    @Override
    public Map<String, Integer> getIngredients() {
        return Map.of(
                "item_id_a", 2, // 例: アイテムAが2個
                "item_id_b", 1  // 例: アイテムBが1個
        );
    }

    @Override
    public String getResultItemId() {
        return "result_item_id"; // 例: 結果アイテムC
    }

    @Override
    public int getResultAmount() {
        return 1;
    }

    @Override
    public String getFusionNpcId() {
        return "合成屋"; // このレシピを提供するNPCの名前
    }

    @Override
    public int getRequiredLevel() {
        return 10; // 合成に必要なレベル
    }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return Map.of(
                "fusion_skill", 5 // 例: 合成スキルレベル5が必要
        );
    }
}
