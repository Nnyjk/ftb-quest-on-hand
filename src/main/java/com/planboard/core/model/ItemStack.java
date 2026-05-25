package com.planboard.core.model;

import java.util.Objects;

/**
 * 物品栈模型 — 核心纯 Java，无任何 Minecraft/mod 依赖
 * 暂不支持 NBT 标签
 */
public final class ItemStack {

    private final String registryName;
    private final int count;

    public ItemStack(String registryName, int count) {
        if (registryName == null || registryName.isBlank()) {
            throw new IllegalArgumentException("registryName cannot be null or blank");
        }
        if (count < 1) {
            throw new IllegalArgumentException("count must be >= 1");
        }
        this.registryName = registryName;
        this.count = count;
    }

    public static ItemStack of(String registryName, int count) {
        return new ItemStack(registryName, count);
    }

    public String getRegistryName() {
        return registryName;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemStack itemStack = (ItemStack) o;
        return count == itemStack.count && Objects.equals(registryName, itemStack.registryName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registryName, count);
    }

    @Override
    public String toString() {
        return "ItemStack{" +
                "registryName='" + registryName + '\'' +
                ", count=" + count +
                '}';
    }
}
