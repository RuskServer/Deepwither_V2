package com.ruskserver.deepwither_V2.modules.trader.api;

/**
 * トレーダーが販売する商品の定義。
 * 固定価格での買・売を管理します。
 */
public class TraderProduct {

    private final String itemId;
    private final double buyPrice;      // プレイヤーがこの価格でトレーダーから購入

    public TraderProduct(String itemId, double buyPrice) {
        this.itemId = itemId;
        this.buyPrice = buyPrice;
    }

    public String getItemId() {
        return itemId;
    }

    public double getBuyPrice() {
        return buyPrice;
    }
}

