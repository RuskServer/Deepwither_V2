package com.ruskserver.deepwither_V2.modules.dungeon;

import com.ruskserver.deepwither_V2.core.di.annotations.Module;

/**
 * プロシージャルダンジョン生成システムのモジュールシェル。
 * <p>
 * WorldEdit スケマティックを用いた部屋とドアの逐次生成方式のダンジョンを提供します。
 * <p>
 * このモジュールが DI コンテナに登録されると、配下の {@code @Service}, {@code @Component} クラスが
 * 自動的にスキャン・登録されます。
 */
@Module
public class DungeonModule {
}
