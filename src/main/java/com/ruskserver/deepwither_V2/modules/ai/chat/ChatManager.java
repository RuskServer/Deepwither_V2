package com.ruskserver.deepwither_V2.modules.ai.chat;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.ai.api.AiApiClient;
import com.ruskserver.deepwither_V2.modules.ai.api.ApiResponseParser;
import com.ruskserver.deepwither_V2.modules.ai.api.RateLimiter;
import com.ruskserver.deepwither_V2.modules.ai.build.BuildCalculator;
import com.ruskserver.deepwither_V2.modules.ai.build.BuildCandidate;
import com.ruskserver.deepwither_V2.modules.ai.build.BuildGoal;
import com.ruskserver.deepwither_V2.modules.ai.kd.KdRetriever;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderDefinition;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderProduct;
import com.ruskserver.deepwither_V2.modules.trader.service.TraderReputationService;
import com.ruskserver.deepwither_V2.modules.trader.service.TraderService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Service
public class ChatManager implements Stoppable {

    private static final int RAG_TOP_K = 5;
    private static final int HISTORY_MAX_LINES = 20;

    private final KdRetriever retriever;
    private final BuildCalculator buildCalculator;
    private final PromptBuilder promptBuilder;
    private final ApiResponseParser responseParser;
    private final RateLimiter rateLimiter;
    private final TraderReputationService reputationService;
    private final TraderService traderService;
    private final Logger log;

    private AiApiClient apiClient;
    private final Map<UUID, ChatSession> sessions = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Inject
    public ChatManager(KdRetriever retriever, BuildCalculator buildCalculator,
                       PromptBuilder promptBuilder, ApiResponseParser responseParser,
                       RateLimiter rateLimiter, TraderReputationService reputationService,
                       TraderService traderService, Logger log) {
        this.retriever = retriever;
        this.buildCalculator = buildCalculator;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.rateLimiter = rateLimiter;
        this.reputationService = reputationService;
        this.traderService = traderService;
        this.log = log;
    }

    public void initializeApi(String apiKey, int timeoutSeconds) {
        this.apiClient = new AiApiClient(apiKey, timeoutSeconds);
        log.info("[ChatManager] AI API client initialized.");
    }

    public boolean isApiReady() {
        return apiClient != null;
    }

    public record ChatResult(String response, boolean success, String error, boolean thinking) {}

    public ChatResult ask(UUID userId, String question) {
        RateLimiter.Result rateResult = rateLimiter.tryAcquire(userId);
        if (rateResult != RateLimiter.Result.ALLOWED) {
            String msg = switch (rateResult) {
                case RATE_LIMITED_GLOBAL -> "グローバルのレート制限中です。しばらくお待ちください。";
                case RATE_LIMITED_USER -> "あなたのレート制限中です。1分ほどお待ちください。";
                case DAILY_LIMIT_EXCEEDED -> "本日のAPI利用上限に達しました。明日以降にお試しください。";
                default -> "レート制限中です。";
            };
            return new ChatResult(msg, false, msg, false);
        }

        ChatSession session = sessions.computeIfAbsent(userId, ChatSession::new);

        try {
            String ragContext = retriever.retrieveAsContext(question, RAG_TOP_K);
            String repContext = buildReputationContext(userId);
            String prompt = promptBuilder.build(question, ragContext, repContext, false);

            if (apiClient == null) {
                return new ChatResult("APIが初期化されていません", false, "API not initialized", false);
            }

            session.appendUserMessage(question);
            var apiResponse = apiClient.call(prompt, false);

            if (!apiResponse.success()) {
                return new ChatResult("", false, apiResponse.error(), false);
            }

            String answer = responseParser.parse(apiResponse.text());
            session.appendAssistantMessage(answer);
            return new ChatResult(answer, true, null, false);

        } catch (Exception e) {
            log.warning("[ChatManager] ask failed: " + e.getMessage());
            return new ChatResult("", false, "Internal error: " + e.getMessage(), false);
        }
    }

    public CompletableFuture<ChatResult> buildAsync(UUID userId, String question) {
        // 重い計算プロセスを廃止し、通常のチャットとして処理
        // ただし、非同期実行のインターフェースは維持する
        return CompletableFuture.supplyAsync(() -> {
            return ask(userId, question);
        }, executor);
    }

    private BuildGoal estimateGoal(String question) {
        String q = question.toLowerCase();

        if (q.contains("魔法防御") || q.contains("mdef") || q.contains("magic")) {
            if (q.contains("物理") || q.contains("def") || q.contains("バランス")) {
                return BuildGoal.balanced(StatType.MAGIC_DEFENSE, StatType.DEFENSE, 0.6);
            }
            return BuildGoal.prioritize(StatType.MAGIC_DEFENSE);
        }
        if (q.contains("防御") || q.contains("defense") || q.contains("物理")) {
            return BuildGoal.prioritize(StatType.DEFENSE);
        }
        if (q.contains("hp") || q.contains("体力") || q.contains("health")) {
            return BuildGoal.prioritize(StatType.HEALTH);
        }
        if (q.contains("マナ") || q.contains("mana") || q.contains("max_mana")) {
            return BuildGoal.prioritize(StatType.MAX_MANA);
        }
        if (q.contains("火力") || q.contains("dps") || q.contains("攻撃")) {
            return BuildGoal.prioritize(StatType.ATTACK_DAMAGE);
        }
        if (q.contains("魔法火力") || q.contains("魔法攻撃") || q.contains("mdmg")) {
            return BuildGoal.prioritize(StatType.MAGIC_DAMAGE);
        }

        return BuildGoal.balanced(StatType.MAGIC_DEFENSE, StatType.DEFENSE, 0.5);
    }

    public int getRemainingDaily() {
        return rateLimiter.getRemainingDaily();
    }

    private String buildReputationContext(UUID userId) {
        Player player = Bukkit.getPlayer(userId);
        if (player == null) return "";

        StringBuilder sb = new StringBuilder("【プレイヤーのトレーダー信用度】\n");
        for (TraderDefinition trader : traderService.getAllTraders().values()) {
            int rep = reputationService.getReputation(player, trader.getNpcName());
            int maxReq = trader.getProducts().stream()
                    .mapToInt(TraderProduct::getRequiredReputation)
                    .max().orElse(0);
            sb.append(trader.getDisplayName()).append(": 信用度 ").append(rep);
            if (maxReq > 0) {
                sb.append(" (最高要求: ").append(maxReq).append(")");
            }
            sb.append("\n");
        }
        sb.append("【注意】信用度が足りないトレーダーの要求アイテムは購入不可。装備提案の際は信用度を考慮すること。\n");
        return sb.toString();
    }

    @Override
    public void stop() {
        executor.shutdownNow();
    }
}
