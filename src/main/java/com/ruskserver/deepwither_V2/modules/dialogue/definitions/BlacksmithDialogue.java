package com.ruskserver.deepwither_V2.modules.dialogue.definitions;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.crafting.gui.CraftingRecipeListGui;
import com.ruskserver.deepwither_V2.modules.crafting.gui.CraftingRepairListGui;
import com.ruskserver.deepwither_V2.modules.dialogue.api.Dialogue;
import com.ruskserver.deepwither_V2.modules.dialogue.api.DialogueGraph;
import com.ruskserver.deepwither_V2.modules.dialogue.api.SpeakerType;
import com.ruskserver.deepwither_V2.modules.gui.GuiContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiService;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.quest.definitions.MiningSmithingTutorialQuest;
import com.ruskserver.deepwither_V2.modules.quest.service.QuestService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

@Component
public class BlacksmithDialogue implements Dialogue {

    private final QuestService questService;
    private final ItemManager itemManager;
    private final GuiService guiService;
    private final Deepwither_V2 plugin;

    @Inject
    public BlacksmithDialogue(QuestService questService, ItemManager itemManager, GuiService guiService, Deepwither_V2 plugin) {
        this.questService = questService;
        this.itemManager = itemManager;
        this.guiService = guiService;
        this.plugin = plugin;
    }

    @Override
    public String getNpcName() {
        return "鍛冶屋";
    }

    @Override
    public List<String> getNpcNames() {
        return List.of("鍛冶屋", "合成屋");
    }

    @Override
    public DialogueGraph getGraph() {
        return DialogueGraph.builder("blacksmith_tutorial")
                .node("start", SpeakerType.NPC, "お、いらっしゃーい。何にする？")
                    // 報告可能
                    .choice("【報告】中古の鉄ピッケルが完成した！", "turn_in_node",
                            ctx -> questService.isAccepted(ctx.player().getUniqueId(), MiningSmithingTutorialQuest.ID)
                                    && questService.checkCompletion(ctx.player(), MiningSmithingTutorialQuest.ID))
                    // 進行中
                    .choice("【進捗確認】チュートリアルの手順をもう一度", "in_progress_node",
                            ctx -> questService.isAccepted(ctx.player().getUniqueId(), MiningSmithingTutorialQuest.ID)
                                    && !questService.checkCompletion(ctx.player(), MiningSmithingTutorialQuest.ID))
                    // 未受注
                    .choice("【チュートリアル】採掘と装備製作を教えてほしい", "intro_tutorial",
                            ctx -> !questService.isAccepted(ctx.player().getUniqueId(), MiningSmithingTutorialQuest.ID)
                                    && !questService.isCompleted(ctx.player().getUniqueId(), MiningSmithingTutorialQuest.ID))
                    // 常時メニュー
                    .choice("製作レシピ一覧を開く", "open_craft_gui")
                    .choice("装備を修理したい", "open_repair_gui")
                    .choice("用はない", "goodbye")
                .end()

                // 初回チュートリアル受注
                .node("intro_tutorial", SpeakerType.NPC,
                        "外のエンティティども、最近やたら元気で困っちゃうよね〜。丸腰で行ったらあっという間にミンチだから、自分でピッケル振って装備くらい作れるようになっときなよ。")
                    .choice("どうすればいい？", "intro_tutorial_2")
                .end()

                .node("intro_tutorial_2", SpeakerType.NPC,
                        "ほら、この探鉱ピッケルあげるからさ。そこらの金鉱石を掘って粗金塊を集めて、合成台でインゴットにして、鉄ピッケルに仕立ててみな？道具が良くなればそれだけ生き残りやすくなるし、まあ気楽にやってみてよ。")
                    .choice("やってみる！（ピッケル受領＆受注）", "accept_tutorial")
                    .choice("また今度にする", "goodbye")
                .end()

                .node("accept_tutorial", SpeakerType.NPC,
                        "よしよし、まずは外の金鉱石を掘って『粗金塊』を10個ほど集めてきな！手に入ったらここの合成台でインゴットに精錬するんだよ。")
                    .action(ctx -> {
                        Player player = ctx.player();
                        ItemStack pickaxe = itemManager.generate("lunaris_survey_pickaxe");
                        if (pickaxe != null) {
                            questService.giveOrDrop(player, pickaxe);
                        }
                        questService.acceptQuest(player, MiningSmithingTutorialQuest.ID);
                    })
                    .choice("わかった！", "goodbye")
                .end()

                // 進行中
                .node("in_progress_node", SpeakerType.NPC,
                        "粗金塊を掘って、インゴットにして、鉄ピッケルに仕立てるんだよ。合成台はここのやつを自由に使っていいからね〜。")
                    .choice("製作レシピを開く", "open_craft_gui")
                    .choice("手順をもう一度", "mining_hint")
                    .choice("わかった、行ってくる", "goodbye")
                .end()

                .node("mining_hint", SpeakerType.NPC,
                        "1. 探鉱ピッケルで金鉱石を掘り、粗金塊を10個集める\n2. 合成台で金精インゴットを2個精錬する\n3. 探鉱ピッケルとインゴットで中古の鉄ピッケルを合成する\nこれでバッチリさ！")
                    .choice("なるほど", "goodbye")
                .end()

                // 完了報告
                .node("turn_in_node", SpeakerType.NPC,
                        "お、鉄ピッケル完成させたんだ！やるじゃん〜。これでより硬い鉱石もサクサク掘れるようになるよ。報酬あげとくね、大切に使いなよ！")
                    .action(ctx -> questService.turnInQuest(ctx.player(), MiningSmithingTutorialQuest.ID))
                    .choice("ありがとう！", "post_tutorial_talk")
                .end()

                .node("post_tutorial_talk", SpeakerType.NPC,
                        "これで採掘と鍛冶の基礎はおしまい！あとは好きな装備を作ったり修理したりして、ガンガン生き残ってね〜。")
                    .choice("了解！", "goodbye")
                .end()

                // GUIオープン
                .node("open_craft_gui", SpeakerType.NPC, "製作レシピを開くね。")
                    .action(ctx -> {
                        Player player = ctx.player();
                        ctx.endDialogue();
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            GuiContext context = new GuiContext(Map.of(CraftingRecipeListGui.NPC_KEY, "合成屋"));
                            guiService.open(player, CraftingRecipeListGui.ID, context);
                        });
                    })
                .end()

                .node("open_repair_gui", SpeakerType.NPC, "修理可能な装備を確認するね。")
                    .action(ctx -> {
                        Player player = ctx.player();
                        ctx.endDialogue();
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            guiService.open(player, CraftingRepairListGui.ID, GuiContext.EMPTY);
                        });
                    })
                .end()

                .node("goodbye", SpeakerType.NPC, "へーい、死なない程度に気をつけてね〜。")
                .end()

                .build();
    }
}
