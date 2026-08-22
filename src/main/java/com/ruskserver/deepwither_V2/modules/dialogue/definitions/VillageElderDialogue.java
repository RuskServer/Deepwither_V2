package com.ruskserver.deepwither_V2.modules.dialogue.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.dialogue.api.Dialogue;
import com.ruskserver.deepwither_V2.modules.dialogue.api.DialogueGraph;
import com.ruskserver.deepwither_V2.modules.dialogue.api.SpeakerType;
import com.ruskserver.deepwither_V2.modules.quest.definitions.BeginningOfJourneyQuest;
import com.ruskserver.deepwither_V2.modules.quest.gui.QuestGUI;
import com.ruskserver.deepwither_V2.modules.quest.service.QuestService;
import org.bukkit.entity.Player;

import java.util.UUID;

@Component
public class VillageElderDialogue implements Dialogue {

    private final QuestService questService;
    private final QuestGUI questGui;

    @Inject
    public VillageElderDialogue(QuestService questService, QuestGUI questGui) {
        this.questService = questService;
        this.questGui = questGui;
    }

    @Override
    public String getNpcName() {
        return "村長";
    }

    @Override
    public DialogueGraph getGraph() {
        return DialogueGraph.builder("village_elder_main")
                // 開始・ルーターノード
                .node("start", SpeakerType.NPC, "やあ、よく来たね。まあのんびりしていきなよ。")
                    .choice("話しかける", "route_check")
                .end()

                .node("route_check", SpeakerType.NPC, "/skip/")
                    // 完了報告可能
                    .choice("【報告】砦のグールを討伐してきた", "turn_in_node",
                            ctx -> questService.isAccepted(ctx.player().getUniqueId(), BeginningOfJourneyQuest.ID)
                                    && questService.checkCompletion(ctx.player(), BeginningOfJourneyQuest.ID))
                    // 討伐中
                    .choice("砦のグール討伐の進捗について", "in_progress_node",
                            ctx -> questService.isAccepted(ctx.player().getUniqueId(), BeginningOfJourneyQuest.ID)
                                    && !questService.checkCompletion(ctx.player(), BeginningOfJourneyQuest.ID))
                    // メイン未受注
                    .choice("何か手伝えることはある？", "intro_story",
                            ctx -> !questService.isAccepted(ctx.player().getUniqueId(), BeginningOfJourneyQuest.ID)
                                    && !questService.isCompleted(ctx.player().getUniqueId(), BeginningOfJourneyQuest.ID))
                    // 完了済み通常メニュー
                    .choice("世界の様子について聞く", "world_lore",
                            ctx -> questService.isCompleted(ctx.player().getUniqueId(), BeginningOfJourneyQuest.ID))
                    .choice("素材収集の依頼（デイリー）を受けたい", "open_daily",
                            ctx -> questService.isCompleted(ctx.player().getUniqueId(), BeginningOfJourneyQuest.ID))
                    .choice("用はない", "goodbye")
                .end()

                // 初回世界観説明 & メイン受注
                .node("intro_story", SpeakerType.NPC,
                        "知っての通り、100年前に旧文明とやらが滅んじゃってさ。残された人類はこのデカい島に身を寄せて、今日までなんとなーく頑張って生き延びてきたわけ。")
                    .choice("ふむふむ", "intro_story_2")
                .end()

                .node("intro_story_2", SpeakerType.NPC,
                        "ところが最近、どういう風の吹き回しかエンティティどもがやたら活発になってきててさぁ。いやーほんとヤバいよね。")
                    .choice("それで、頼みって？", "intro_story_3")
                .end()

                .node("intro_story_3", SpeakerType.NPC,
                        "ってことで！君たち冒険者にはぜひ頑張って生き残ってほしいんだよね。手始めに、そこの古い砦にうろついてるグールでも適当に片付けてきてくれない？腕試しってやつさ。終わったらそこそこの装備あげるからさ。")
                    .choice("わかった、片付けてくる（受注）", "accept_main")
                    .choice("旧文明やエンティティについてもっと詳しく", "world_lore")
                    .choice("今はパスで", "decline_main")
                .end()

                .node("accept_main", SpeakerType.NPC, "いいねえ！じゃあ砦のグール5体ほどよろしくね〜。死なない程度に頑張って！")
                    .action(ctx -> questService.acceptQuest(ctx.player(), BeginningOfJourneyQuest.ID))
                    .choice("任せてくれ", "goodbye")
                .end()

                .node("decline_main", SpeakerType.NPC, "おやおや、命は大事にしなきゃね。気が向いたらまた来てよ〜。")
                    .choice("またね", "goodbye")
                .end()

                // 討伐中
                .node("in_progress_node", SpeakerType.NPC, "ん？まだ砦のグールが残ってるみたいだよ。油断してやられないようにね〜。")
                    .choice("砦はどこ？", "fortress_hint")
                    .choice("わかった、行ってくる", "goodbye")
                .end()

                .node("fortress_hint", SpeakerType.NPC, "村を出て少し進んだところにある石造りの崩れた砦だよ。グールがうろうろしてるからすぐ分かるはずさ。")
                    .choice("よし、行ってくる", "goodbye")
                .end()

                // 完了報告
                .node("turn_in_node", SpeakerType.NPC, "お、無事に戻ったんだ！やるじゃん。じゃあ約束の装備あげるね。これで次もなんとか生き残ってよ〜。")
                    .action(ctx -> questService.turnInQuest(ctx.player(), BeginningOfJourneyQuest.ID))
                    .choice("ありがとう！", "post_complete_talk")
                .end()

                .node("post_complete_talk", SpeakerType.NPC, "腕も立つみたいだし、これからは日々の素材集め（デイリー依頼）も頼むよ。またいつでも声かけてね。")
                    .choice("わかった", "goodbye")
                .end()

                // 世界観解説
                .node("world_lore", SpeakerType.NPC,
                        "100年前の旧文明は高度な魔導技術を持ってたらしいけど、あっさり崩壊しちゃってね。今は島を取り囲む霧と、急増したエンティティたちのせいで外の世界がどうなってるかもさっぱりさ。")
                    .choice("エンティティって何者？", "lore_entities")
                    .choice("なるほど", "goodbye")
                .end()

                .node("lore_entities", SpeakerType.NPC,
                        "遺跡や砦の奥から湧き出る異形の怪物たちさ。グールなんかもその一種だね。放っておくと村まで来ちゃうから、冒険者のみんなで適当に間引いてほしいわけ。")
                    .choice("よく分かった", "goodbye")
                .end()

                // デイリーGUIオープン
                .node("open_daily", SpeakerType.NPC, "日課の素材集めかい？リストを開くから確認してみてね。")
                    .action(ctx -> {
                        Player player = ctx.player();
                        ctx.endDialogue();
                        questGui.openQuestGui(player);
                    })
                .end()

                .node("goodbye", SpeakerType.NPC, "じゃあね〜、気をつけて。")
                .end()

                .build();
    }
}
