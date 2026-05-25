package com.ftbquestonhand.model;

import java.util.Objects;

/**
 * 物品栈 - 核心纯 Java 模型
 */
public final class ItemStack {

    private final String registryName;
    private final int count;
    private final String nbt;

    public ItemStack(String registryName, int count) {
        this(registryName, count, null);
    }

    public ItemStack(String registryName, int count, String nbt) {
        this.registryName = Objects.requireNonNull(registryName);
        this.count = count;
        this.nbt = nbt;
    }

    public String getRegistryName() { return registryName; }
    public int getCount()           { return count; }
    public String getNbt()           { return nbt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemStack that)) return false;
        return Objects.equals(registryName, that.registryName)
            && count == that.count
            && Objects.equals(nbt, that.nbt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registryName, count, nbt);
    }

    @Override
    public String toString() {
        return "ItemStack{registry=" + registryName + ", count=" + count + ", nbt=" + nbt + "}";
    }
}
