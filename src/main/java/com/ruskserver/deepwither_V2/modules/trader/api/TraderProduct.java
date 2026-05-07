package com.ruskserver.deepwither_V2.modules.trader.api;

/**
 * トレーダーが販売する商品の定義。
 * 固定価格での買・売を管理します。
 */
public class TraderProduct {

    private final String itemId;
    private final double buyPrice;      // プレイヤーがこの価格でトレーダーから購入
    private final double sellPrice;     // プレイヤーがこの価格でトレーダーに売却

    public TraderProduct(String itemId, double buyPrice, double sellPrice) {
        this.itemId = itemId;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

    public String getItemId() {
        return itemId;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }
}

