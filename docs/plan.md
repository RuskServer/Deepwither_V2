# AI Chatbot + RAG + BuildCalculator 実装計画

## 全体アーキテクチャ

```
Discord (JDA via DiscordSRV) → ChatManager → PromptBuilder → Google AI Studio API (gemma-4-31b-it)
                                    ↑               ↑
                               BuildCalculator  KdRetriever
                                    ↑               ↑
                                ItemManager    EmbeddingService
                                SkillRegistry  (gemma-300m-ONNX via DJL)
                                CustomMobManager
                                SkillTreeRegistry
```

## パッケージ構成

```
modules/ai/
├── kd/
│   ├── KdDocument.java          # ナレッジドキュメントモデル
│   ├── KdIndexer.java           # レジストリ走査 → テキスト文書化 → embedding保存
│   └── KdRetriever.java         # クエリembedding → コサイン類似度 → 上位k件
├── embedding/
│   ├── EmbeddingService.java    # DJL + ONNX Runtime ラッパー（Startable）
│   └── VectorStore.java         # インメモリ + JSON永続化、線形探索
├── build/
│   ├── BuildCalculator.java     # 全装備組み合わせpreroll + 精密calculate
│   ├── BuildGoal.java           # 自然言語→マッピング方針
│   └── BuildCandidate.java      # 1構成の最終値
├── api/
│   ├── AiApiClient.java         # Google AI Studio REST API (OkHttp)
│   ├── RateLimiter.java         # 15RPM/3RPM per user/1.5k daily
│   └── ApiResponseParser.java   # Gemma4 thought分離
└── chat/
    ├── ChatManager.java         # オーケストレーター
    ├── ChatSession.java         # ユーザーセッション
    └── PromptBuilder.java       # プロンプト構築

modules/discord/
├── DiscordBotService.java       # JDAベース (DiscordSRV流用)
└── listener/
    └── ChatCommandListener.java # /ask /build スラッシュコマンド
```

## 実装順序

| Phase | コンポーネント | 依存 |
|-------|-------------|------|
| 1 | VectorStore, EmbeddingService | 基盤、他に依存しない |
| 2 | KdIndexer, KdRetriever | ItemManager, SkillRegistry 等 |
| 3 | BuildCalculator | ItemManager, StatManager |
| 4 | PromptBuilder, ChatManager | 上記全て |
| 5 | AiApiClient, RateLimiter, ApiResponseParser | 外部API |
| 6 | DiscordBotService | JDA (DiscordSRV) |
| 7 | config.yml + DI統合 | 全コンポーネント |

## 自然言語→ビルドフロー

```
ユーザー: "魔法防御を最大にしたい"
    ↓
1. RAG検索 → 関連KD（装備一覧, 計算式, パッシブ）
2. BuildGoal 推定 → primary=MAGIC_DEFENSE, weight=1.0
3. BuildCalculator.preroll(goal) → 625通り全探索 → 上位5候補
4. PromptBuilder.build(query, docs, candidates, thinking=true)
5. event.deferReply() → 別スレッドでAPI呼び出し（最大5分）
6. ApiResponseParser.parse() → thought除去 → Discord返却
```

## 設定（config.yml 追記）

```yaml
ai:
  google-api-key: ""
  daily-limit: 1500
  global-rpm: 15
  user-rpm: 3
  thinking-timeout-seconds: 300
  model-path: "plugins/Deepwither_V2/models/gemma-300m-onnx"

discord:
  token: ""
  guild-id: ""
```

## 注意事項

- JDA は DiscordSRV のものを流用するため直接依存を追加しない
- Gemma4 のレスポンスは `parts[].thought` で思考と回答を分離して返すため、必ずフィルタする
- API制限は厳守（15RPM global / 3RPM per user / 1.5k daily）
