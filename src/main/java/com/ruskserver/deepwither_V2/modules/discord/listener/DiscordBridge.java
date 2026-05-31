package com.ruskserver.deepwither_V2.modules.discord.listener;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.ai.chat.ChatManager;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.events.message.MessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@Component
public class DiscordBridge extends ListenerAdapter {

    private static final Color EMBED_COLOR = new Color(0xA5, 0x6F, 0xFF);
    private static final Color EMBED_COLOR_LIMIT = new Color(0xE0, 0x7B, 0x39);
    private static final Color EMBED_COLOR_DOWN = new Color(0x99, 0x99, 0x99);
    private static final Color EMBED_COLOR_PENDING = new Color(0x7C, 0x7C, 0xFF);
    private static final String FOOTER_TEXT = "Echoes of Aether AI";
    private static final int CONTENT_LIMIT = 3800;

    private final Deepwither_V2 plugin;
    private final ChatManager chatManager;
    private final Logger log;

    @Inject
    public DiscordBridge(Deepwither_V2 plugin, ChatManager chatManager, Logger log) {
        this.plugin = plugin;
        this.chatManager = chatManager;
        this.log = log;
        log.info("[DiscordBridge] Initialized.");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try {
            if (event.getAuthor().isBot()) return;

            String raw = event.getMessage().getContentRaw();
            String botId = event.getJDA().getSelfUser().getId();
            String mentionA = "<@" + botId + ">";
            String mentionB = "<@!" + botId + ">";

            if (!raw.contains(mentionA) && !raw.contains(mentionB)) return;

            String query = raw.replace(mentionA, "").replace(mentionB, "").trim();
            if (query.isEmpty()) return;

            String userId = event.getAuthor().getId();
            String userName = event.getAuthor().getName();
            log.info("[DiscordBridge] Query from " + userName + " (" + userId + "): " + query);

            boolean isBuild = query.contains("装備") || query.contains("ビルド") || query.contains("構成")
                    || query.contains("build") || query.contains("armor") || query.contains("防御");

            long startMs = System.currentTimeMillis();
            MessageEmbed pending = buildPendingEmbed();
            event.getChannel().sendMessageEmbeds(pending)
                    .referenceById(event.getMessageIdLong())
                    .queue(sent -> {
                        CompletableFuture<ChatManager.ChatResult> future;
                        if (isBuild) {
                            future = chatManager.buildAsync(java.util.UUID.nameUUIDFromBytes(userId.getBytes()), query);
                        } else {
                            future = CompletableFuture.completedFuture(
                                    chatManager.ask(java.util.UUID.nameUUIDFromBytes(userId.getBytes()), query));
                        }

                        future.thenAccept(result -> {
                            long ms = System.currentTimeMillis() - startMs;
                            MessageEmbed embed = buildEmbed(result, ms);
                            sent.editMessageEmbeds(embed).queue();
                            log.info("[DiscordBridge] Replied (success=" + result.success() + ", " + ms + "ms)");
                        });
                    });
        } catch (Throwable t) {
            log.warning("[DiscordBridge] onMessageReceived failed: " + t.getMessage());
        }
    }

    private MessageEmbed buildPendingEmbed() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(EMBED_COLOR_PENDING);
        eb.setTitle("🤔 考え中…");
        eb.setDescription("質問を読み込んで生成しているよ。少しだけ待っててね。");
        eb.setFooter(FOOTER_TEXT);
        return eb.build();
    }

    private MessageEmbed buildEmbed(ChatManager.ChatResult result, long elapsedMs) {
        EmbedBuilder eb = new EmbedBuilder();
        String footer = FOOTER_TEXT + "  ·  " + String.format("%.1f", elapsedMs / 1000.0) + "s";
        eb.setFooter(footer);

        if (result.success()) {
            eb.setColor(EMBED_COLOR);
            String text = result.response();
            eb.setDescription(truncate(text, CONTENT_LIMIT));

            int daily = chatManager.getRemainingDaily();
            eb.addField("⚙ 今日の残り", daily + "回", false);
        } else {
            eb.setColor(EMBED_COLOR_LIMIT);
            eb.setTitle("⏳ 利用制限 / エラー");
            eb.setDescription(result.error());
        }

        return eb.build();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
