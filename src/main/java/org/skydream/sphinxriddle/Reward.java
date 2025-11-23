package org.skydream.sphinxriddle;

import net.minecraft.resources.ResourceLocation;

public class Reward {
    private final String itemId;
    private final int maxAmount;

    public Reward(String itemId, int maxAmount) {
        this.itemId = itemId;
        this.maxAmount = maxAmount;
    }

    public String getItemId() { return itemId; }
    public int getMaxAmount() { return maxAmount; }

    // 使用 ResourceLocation 的静态方法
    public ResourceLocation getItemResourceLocation() {
        return ResourceLocation.parse(itemId);
    }
}