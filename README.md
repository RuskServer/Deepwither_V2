<div align="center">

# ⚔️ Echoes of Aether Core Engine

**The High-Performance Modular MMO Engine (Codename: Deepwither_V2)**

[![Java](https://img.shields.io/badge/Java-25-orange.svg?style=for-the-badge&logo=java)](#)
[![Paper API](https://img.shields.io/badge/Paper-26.1.2-blue.svg?style=for-the-badge)](#)
[![Architecture](https://img.shields.io/badge/Architecture-Modular_Monolith-brightgreen.svg?style=for-the-badge)](#)
[![Database](https://img.shields.io/badge/Database-H2_%2B_HikariCP-lightgrey.svg?style=for-the-badge)](#)

</div>

---

## 📜 Overview

**Echoes of Aether Core Engine**（内部コードネーム: Deepwither_V2）は、Minecraft MMORPGサーバー『Echoes of Aether』を駆動するために独自開発された専用MMOエンジンです。

MMORPG特有の「膨大なプレイヤーデータ」「複雑なランダムステータス生成」「数百に及ぶスキルの管理」といった過酷な要件を、圧倒的なパフォーマンスで処理するためにゼロから設計されました。独自の**カスタム依存性注入（DI）コンテナ**による「モジュラーモノリス」アーキテクチャを採用し、大規模開発においてもスパゲッティコード化を防ぎ、新機能（モジュール）を最速でデプロイできる基盤を提供します。

## ✨ Engine Features

### 🧬 Custom DI Container (The Heart of the Engine)
* Spring Frameworkライクなアノテーション（`@Component`, `@Service`, `@Command`）によって、ゲーム内のすべてのシステム（経済、戦闘、クエスト等）を自動で接続（Auto-Wiring）します。
* コンポーネント間の循環参照を起動時に検出し、依存ツリーをコンソールに出力する高度な安全機構を備えています。

### ⚡ Aggregated MMO Player Profile
* レベル、所持金、スキルツリーなど、MMO特有の膨大に分散しがちなプレイヤーデータを `PlayerData` コンテナに一元集約。
* 変更があったモジュール（例: 経験値が増えた場合のみ）だけをピンポイントでDBへ保存する **DirtyFlag（差分更新）システム** を搭載。数千人規模の同時接続でもデータベースを詰まらせません。

### ⚔️ Data-Driven Loot & Item Engine
* MMOの根幹である「武器・防具」をYAMLではなくJavaクラスとして直接定義。
* PDC（PersistentDataContainer）を利用し、ハクスラのような「ランダムモディファイア（追加ステータス）」をアイテム個別に付与。ステータスに応じたLore（説明文）の自動フォーマット機能を備えています。

### 🧩 Single-File Module Architecture
* ちょっとした新システム（例：特定のブロックを壊すとコインが出る機能など）を、1つの `.java` ファイルにデータ構造からコマンドまで全て詰め込んで実装可能。新コンテンツのプロトタイピングと本番実装の速度を劇的に引き上げます。

---

## 🛠️ Quick Start (Developer API)

エンジンのAPIを利用すれば、MMOの新しい成長システムなども型安全かつ爆速で実装できます。

```java
@Service
public class EchoesLevelingSystem {

    private final PlayerDataRepository repository;

    // エンジンがリポジトリを自動注入
    @Inject
    public EchoesLevelingSystem(PlayerDataRepository repository) {
        this.repository = repository;
    }

    public void grantExperience(Player player, int exp) {
        repository.get(player.getUniqueId()).ifPresent(data -> {
            // キャスト不要の完全な型安全性
            LevelData levelData = data.get(LevelDataProvider.KEY);
            levelData.addExp(exp);
            
            // Dirtyフラグを立てて保存（このプレイヤーのレベルデータテーブルだけが更新される）
            data.markDirty(LevelDataProvider.KEY);
            repository.save(player.getUniqueId(), data);
            
            player.sendMessage("§a+" + exp + " EXP");
        });
    }
}
```

---

## 📚 Engine Documentation

Echoes of Aetherエンジンの内部仕様や、新しいモジュールの開発手順については `docs/` ディレクトリを参照してください。

* 🟢 **[エンジン初期化とDIコンテナ (`getting-started.md`)](docs/getting-started.md)**
* 🔵 **[超高速データベース・キャッシュ層 (`database-guide.md`)](docs/database-guide.md)**
* 🟣 **[1ファイル完結モジュールの開発手法 (`example-single-file-module.md`)](docs/example-single-file-module.md)**
* 🟡 **[カスタムアイテム・モディファイア設計 (`custom-item-guide.md`)](docs/custom-item-guide.md)**

---

<div align="center">
  <i>The Core Engine of <b>Echoes of Aether</b>. Developed by RuskServer</i>
</div>
