package com.ruskserver.deepwither_V2.modules.ai.chat;

import com.ruskserver.deepwither_V2.modules.ai.build.BuildCandidate;
import com.ruskserver.deepwither_V2.modules.ai.build.BuildGoal;
import com.ruskserver.deepwither_V2.modules.ai.kd.KdDocument;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;

import java.util.List;

public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
あなたは Echoes of Aether のビルドアドバイザーです。
以下のデータを元に、ユーザーの要望に合った装備構成を提案してください。
必ず実際の数値を提示し、計算過程も示してください。

【計算式】
- 最終値 = 加算値の合計 × (1 + 乗算値の合計)
- ダメージ軽減率 = 250 / (250 + 防御値)
- 物理ダメージ → DEFENSE で軽減
- 魔法ダメージ → MAGIC_DEFENSE で軽減
- TRUE_DAMAGE / ENVIRONMENTAL は防御無視

【注意事項】
- 数値は実際のゲーム内値に基づいて計算すること
- 複数の装備セットを組み合わせる場合は、各部位のステータスを合計すること
- MarkdownやLaTeX（$\\text{...}$、$$...$$など）は使用せず、プレーンテキストのみで記述すること
""";

    public String build(String userQuery, String ragContext, boolean thinking) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT).append("\n");

        if (!ragContext.isEmpty()) {
            sb.append(ragContext).append("\n");
        }

        sb.append("【ユーザー質問】\n").append(userQuery).append("\n");

        if (!thinking) {
            sb.append("\n回答は簡潔に、具体的な数値を含めてください。");
        } else {
            sb.append("\n思考プロセスを踏まえた上で、最終的な回答を提供してください。");
        }

        return sb.toString();
    }

    public String buildWithCandidates(String userQuery, String ragContext,
                                       BuildGoal goal, List<BuildCandidate> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT).append("\n");

        if (!ragContext.isEmpty()) {
            sb.append(ragContext).append("\n");
        }

        sb.append("【BuildCalculator事前計算 候補】\n");
        sb.append("目標: ").append(goal.getPrimaryStat().getDisplayName());
        if (goal.getSecondaryStat() != null) {
            sb.append(" (優先度").append(String.format("%.0f", goal.getPrimaryWeight() * 100))
                    .append("%) / ").append(goal.getSecondaryStat().getDisplayName());
        }
        sb.append("\n\n");

        for (int i = 0; i < candidates.size(); i++) {
            sb.append("【装備セット ").append(i + 1).append("】\n");
            sb.append(candidates.get(i).toString()).append("\n");
        }

        sb.append("\n【ユーザー質問】\n").append(userQuery).append("\n");
        sb.append("\n思考プロセスを踏まえた上で、ユーザーの要望に最も適した構成を提案してください。");
        sb.append("\n「装備セット」の番号ではなく、装備の具体的なIDや特徴を引用して説明すること。");

        return sb.toString();
    }
}
