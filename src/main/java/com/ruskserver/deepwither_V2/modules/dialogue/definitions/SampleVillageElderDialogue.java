package com.ruskserver.deepwither_V2.modules.dialogue.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.dialogue.api.*;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Component
public class SampleVillageElderDialogue implements Dialogue {

    @Override
    public String getNpcName() {
        return "VillageElder";
    }

    @Override
    public DialogueGraph getGraph() {
        return DialogueGraph.builder("village_elder_intro")
                .node("greeting", SpeakerType.NPC, "よく来たな、旅人よ。私の村にようこそ。")
                    .choice("こんにちは", "ask_help")
                    .choice("私は用がある", "busy_end")
                .end()
                .node("ask_help", SpeakerType.NPC, "実は村の外で変な影を見たのだ。")
                    .choice("何があったんだ？", "explain")
                    .choice("また今度来るよ", "goodbye")
                .end()
                .node("explain", SpeakerType.NPC, "ここから北へ進んだ森の中で、黒い霧が立ち込めている。")
                    .choice("調査を引き受ける", "quest_accept")
                    .choice("他の人に頼んだら？", "decline")
                .end()
                .node("quest_accept", SpeakerType.NPC, "助かる！準備ができたらまた話そう。")
                    .action(ctx -> {
                        Player player = ctx.player();
                        player.sendMessage(net.kyori.adventure.text.Component.text("クエスト『黒い霧の調査』を受注しました。", NamedTextColor.GREEN));
                        ctx.setFlag("quest_blackmist", "accepted");
                    })
                    .choice("わかった、行ってくる", "goodbye")
                .end()
                .node("decline", SpeakerType.NPC, "そうか…仕方ない。もし気が変わったら来てくれ。")
                    .choice("うん、またね", "goodbye")
                .end()
                .node("busy_end", SpeakerType.NPC, "そうか。また話そう。")
                .end()
                .node("goodbye", SpeakerType.NPC, "気をつけてな、旅人。")
                .end()
                .build();
    }
}
