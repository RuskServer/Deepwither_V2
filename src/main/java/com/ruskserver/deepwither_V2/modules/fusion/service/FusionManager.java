package com.ruskserver.deepwither_V2.modules.fusion.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.di.container.DIContainer;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.fusion.api.FusionRecipe;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FusionManager implements Startable {

    private final Map<String, FusionRecipe> registry = new HashMap<>();
    private final DIContainer container;

    @Inject
    public FusionManager(DIContainer container) {
        this.container = container;
    }

    @Override
    public void start() {
        // DIコンテナによって収集されたFusionRecipeの実装クラスを自動登録
        for (Object instance : container.getAllInstances()) {
            if (instance instanceof FusionRecipe) {
                FusionRecipe recipe = (FusionRecipe) instance;
                registry.put(recipe.getId(), recipe);
            }
        }
        Bukkit.getLogger().info("[FusionManager] " + registry.size() + " 個の合成レシピをロードしました。");
    }

    /**
     * 指定されたIDの合成レシピを取得します。
     * @param id レシピID
     * @return FusionRecipeのインスタンス、存在しない場合はnull
     */
    public FusionRecipe getRecipe(String id) {
        return registry.get(id);
    }

    /**
     * 指定されたNPCが提供するすべての合成レシピを取得します。
     * @param npcId 合成屋NPCのID
     * @return そのNPCが提供するFusionRecipeのリスト
     */
    public List<FusionRecipe> getRecipesForNpc(String npcId) {
        return registry.values().stream()
                .filter(recipe -> recipe.getFusionNpcId().equalsIgnoreCase(npcId))
                .collect(Collectors.toList());
    }

    /**
     * すべての登録済みレシピIDを取得します。
     * @return レシピIDのリスト
     */
    public List<String> getRegisteredRecipeIds() {
        return new ArrayList<>(registry.keySet());
    }

    /**
     * 指定されたNPCが合成屋として登録されているかを確認します。
     * @param npcId 確認するNPCのID
     * @return 指定されたNPCが合成屋として登録されていればtrue、そうでなければfalse
     */
    public boolean isFusionNpc(String npcId) {
        return registry.values().stream()
                .anyMatch(recipe -> recipe.getFusionNpcId().equalsIgnoreCase(npcId));
    }
}
