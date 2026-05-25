package com.planboard.core.model;

import java.util.List;
import java.util.Objects;

/**
 * 配方模型 — 核心纯 Java，无任何 Minecraft/mod 依赖
 * 存储一个合成配方：产物 + 材料列表（按合成顺序排列）
 */
public final class Recipe {

    public enum Source {
        JEI,    // 通过 JEI 书签同步添加
        MANUAL  // 玩家手动添加
    }

    private final ItemStack result;
    private final List<ItemStack> ingredients;
    private final Source source;
    private final long addedAt;
    private boolean enabled;

    public Recipe(ItemStack result, List<ItemStack> ingredients, Source source, long addedAt) {
        this.result = result;
        this.ingredients = List.copyOf(ingredients);
        this.source = source;
        this.addedAt = addedAt;
        this.enabled = true;
    }

    public static Recipe of(ItemStack result, List<ItemStack> ingredients, Source source) {
        return new Recipe(result, ingredients, source, System.currentTimeMillis());
    }

    public ItemStack getResult() {
        return result;
    }

    public List<ItemStack> getIngredients() {
        return ingredients;
    }

    public Source getSource() {
        return source;
    }

    public long getAddedAt() {
        return addedAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recipe recipe = (Recipe) o;
        return Objects.equals(result, recipe.result) && Objects.equals(addedAt, recipe.addedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, addedAt);
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "result=" + result +
                ", ingredients=" + ingredients +
                ", source=" + source +
                ", enabled=" + enabled +
                '}';
    }
}
