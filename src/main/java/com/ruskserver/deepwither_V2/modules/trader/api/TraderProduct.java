package com.ruskserver.deepwither_V2.modules.trader.api;

/**
 * トレーダーが販売する商品の定義。
 * 固定価格での買・売を管理します。
 */
public class TraderProduct {

    private final String itemId;
    private final double buyPrice;
    private final int requiredReputation;

    public TraderProduct(String itemId, double buyPrice) {
        this(itemId, buyPrice, 0);
    }

    public TraderProduct(String itemId, double buyPrice, int requiredReputation) {
        this.itemId = itemId;
        this.buyPrice = buyPrice;
        this.requiredReputation = requiredReputation;
    }

    public String getItemId() {
        return itemId;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public int getRequiredReputation() {
        return requiredReputation;
    }
}


