package com.ruskserver.deepwither_V2.modules.ai.chat;

import com.ruskserver.deepwither_V2.modules.ai.build.BuildCandidate;
import com.ruskserver.deepwither_V2.modules.ai.build.BuildGoal;
import com.ruskserver.deepwither_V2.modules.ai.kd.KdDocument;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;

import java.util.List;

public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
あなたは Echoes of Aether の世界を熟知した老練な冒険者、あるいは知識豊富なガイドです。
ユーザーの質問に対し、冒険の助けとなるヒントやこの世界の噂話を提供してください。

【回答の指針】
- 特定の装備構成を「最強」や「最適解」として断定的に教えることは避けてください。
- 装備の組み合わせについては「〇〇と△△を合わせると面白い効果があるという噂だ」といった、可能性の提示に留めてください。
- 数値計算を詳しく解説するのではなく、「〇〇を上げると魔法に強くなるだろう」といった感覚的なアドバイスを優先してください。
- Wikiのような無機質な情報の羅列ではなく、世界観に没入できるような親しみやすい口調で話してください。
- MarkdownやLaTeX（$\\text{...}$、$$...$$など）は使用せず、プレーンテキストのみで記述すること。
""";

    public String build(String userQuery, String ragContext, String repContext, boolean thinking) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT).append("\n");

        if (!ragContext.isEmpty()) {
            sb.append("【世界の知識・記録】\n").append(ragContext).append("\n");
        }
        if (!repContext.isEmpty()) {
            sb.append(repContext).append("\n");
        }

        sb.append("【冒険者の問い】\n").append(userQuery).append("\n");

        if (!thinking) {
            sb.append("\n回答は簡潔かつ情緒的に、冒険心をくすぐる内容にしてください。");
        } else {
            sb.append("\nじっくりと考え、この世界の理に則った深い洞察を提供してください。");
        }

        return sb.toString();
    }

    public String buildWithCandidates(String userQuery, String ragContext, String repContext,
                                       BuildGoal goal, List<BuildCandidate> candidates) {
        // buildWithCandidates は機能を縮小し、build メソッドと統合するか、
        // 候補データを「噂されている装備」として抽象的に扱うように変更
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT).append("\n");

        if (!ragContext.isEmpty()) {
            sb.append("【世界の知識・記録】\n").append(ragContext).append("\n");
        }

        sb.append("【注目されている能力】: ").append(goal.getPrimaryStat().getDisplayName()).append("\n");
        sb.append("【噂に上る装備の断片】\n");
        // 具体的なセットデータではなく、IDや名前からヒントを抽出
        candidates.stream().limit(3).forEach(c -> {
            sb.append("- ").append(c.toString().split("\n")[0]).append(" などの組み合わせが語られているようだ。\n");
        });

        sb.append("\n【冒険者の問い】\n").append(userQuery).append("\n");
        sb.append("\nこれらの情報を踏まえ、具体的な答えではなく、冒険者が自ら答えに辿り着けるような助言をしてください。");

        return sb.toString();
    }
}
