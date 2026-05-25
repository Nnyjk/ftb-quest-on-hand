package com.planboard.core.manager;

import com.planboard.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PlanBoardManager 核心逻辑单元测试
 * 无 Minecraft 依赖，纯 Java
 */
class PlanBoardManagerTest {

    private PlanBoardManager manager;
    private String playerId;

    @BeforeEach
    void setUp() {
        manager = new PlanBoardManager();
        playerId = UUID.randomUUID().toString();
    }

    // ===== 配方管理测试 =====

    @Test
    void addRecipe_singleRecipe() {
        Recipe r = Recipe.of(
                ItemStack.of("minecraft:diamond_sword", 1),
                List.of(ItemStack.of("minecraft:diamond", 3)),
                Recipe.Source.JEI
        );
        manager.addRecipe(playerId, r);
        assertEquals(1, manager.recipeCount(playerId));
    }

    @Test
    void addRecipe_multipleRecipes() {
        manager.addRecipe(playerId, makeRecipe("minecraft:diamond_sword", 1, "minecraft:diamond", 3));
        manager.addRecipe(playerId, makeRecipe("minecraft:iron_pickaxe", 1, "minecraft:iron_ingot", 3));
        assertEquals(2, manager.recipeCount(playerId));
    }

    @Test
    void removeRecipe_existing() {
        Recipe r = makeRecipe("minecraft:diamond_sword", 1, "minecraft:diamond", 3);
        manager.addRecipe(playerId, r);
        assertTrue(manager.removeRecipe(playerId, r));
        assertEquals(0, manager.recipeCount(playerId));
    }

    @Test
    void removeRecipe_nonExisting() {
        Recipe r1 = makeRecipe("minecraft:diamond_sword", 1, "minecraft:diamond", 3);
        Recipe r2 = makeRecipe("minecraft:iron_pickaxe", 1, "minecraft:iron_ingot", 3);
        manager.addRecipe(playerId, r1);
        assertFalse(manager.removeRecipe(playerId, r2));
    }

    @Test
    void getRecipes_returnsCopy() {
        Recipe r = makeRecipe("minecraft:diamond_sword", 1, "minecraft:diamond", 3);
        manager.addRecipe(playerId, r);
        List<Recipe> list = manager.getRecipes(playerId);
        assertEquals(1, list.size());
        assertThrows(UnsupportedOperationException.class, () -> list.add(r));
    }

    @Test
    void getEnabledRecipes_filtersDisabled() {
        Recipe r1 = makeRecipe("minecraft:diamond_sword", 1, "minecraft:diamond", 3);
        Recipe r2 = makeRecipe("minecraft:iron_pickaxe", 1, "minecraft:iron_ingot", 3);
        manager.addRecipe(playerId, r1);
        manager.addRecipe(playerId, r2);
        manager.setEnabled(playerId, r1, false);
        List<Recipe> enabled = manager.getEnabledRecipes(playerId);
        assertEquals(1, enabled.size());
        assertEquals("minecraft:iron_pickaxe", enabled.get(0).getResult().getRegistryName());
    }

    @Test
    void reorder_swapsElements() {
        Recipe r1 = makeRecipe("minecraft:diamond_sword", 1, "minecraft:diamond", 3);
        Recipe r2 = makeRecipe("minecraft:iron_pickaxe", 1, "minecraft:iron_ingot", 3);
        manager.addRecipe(playerId, r1);
        manager.addRecipe(playerId, r2);
        manager.reorder(playerId, 0, 1);
        List<Recipe> list = manager.getRecipes(playerId);
        assertEquals("minecraft:iron_pickaxe", list.get(0).getResult().getRegistryName());
        assertEquals("minecraft:diamond_sword", list.get(1).getResult().getRegistryName());
    }

    @Test
    void clear_removesAll() {
        manager.addRecipe(playerId, makeRecipe("minecraft:diamond_sword", 1, "minecraft:diamond", 3));
        manager.addRecipe(playerId, makeRecipe("minecraft:iron_pickaxe", 1, "minecraft:iron_ingot", 3));
        manager.clear(playerId);
        assertEquals(0, manager.recipeCount(playerId));
    }

    // ===== 提取逻辑测试 =====

    @Test
    void extract_emptyPlan_returnsEmptyPlan() {
        ExtractionResult result = manager.extract(playerId, List.of(), List.of(), 9);
        assertEquals(ExtractionResult.Status.EMPTY_PLAN, result.getStatus());
    }

    @Test
    void extract_allItemsSatisfied_returnsNothing() {
        manager.addRecipe(playerId, makeRecipe("minecraft:diamond_sword", 1, "minecraft:diamond", 3));
        List<ItemStack> container = List.of(ItemStack.of("minecraft:diamond", 10));
        List<ItemStack> backpack = List.of();
        ExtractionResult result = manager.extract(playerId, container, backpack, 9);
        assertEquals(ExtractionResult.Status.NOTHING_TO_DO, result.getStatus());
    }

    @Test
    void extract_partialFromContainer_extractsMissing() {
        manager.addRecipe(playerId, makeRecipe("minecraft:diamond_sword", 1, "minecraft:diamond", 3));
        // 背包有 1 个钻石，容器有 2 个，需要 3 个
        List<ItemStack> container = List.of(ItemStack.of("minecraft:diamond", 2));
        List<ItemStack> backpack = List.of(ItemStack.of("minecraft:diamond", 1));
        ExtractionResult result = manager.extract(playerId, container, backpack, 9);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getTotalItems()); // 容器提取 2 个
    }

    @Test
    void extract_multipleIngredients_extractsInOrder() {
        manager.addRecipe(playerId, makeRecipe(
                "minecraft:diamond_sword", 1,
                "minecraft:diamond", 3,
                "minecraft:stick", 2
        ));
        // 背包有 0 钻石 0 木棍，容器有 3 钻石 2 木棍
        List<ItemStack> container = List.of(
                ItemStack.of("minecraft:diamond", 3),
                ItemStack.of("minecraft:stick", 2)
        );
        List<ItemStack> backpack = List.of();
        ExtractionResult result = manager.extract(playerId, container, backpack, 9);
        assertTrue(result.isSuccess());
        assertEquals(5, result.getTotalItems()); // 3 钻石 + 2 木棍
        assertEquals(2, result.getExtractedTypes());
    }

    @Test
    void extract_containerEmpty_skipsIngredient() {
        manager.addRecipe(playerId, makeRecipe("minecraft:diamond_sword", 1, "minecraft:diamond", 3));
        List<ItemStack> container = List.of(ItemStack.of("minecraft:iron_ingot", 10)); // 错误的物品
        List<ItemStack> backpack = List.of();
        ExtractionResult result = manager.extract(playerId, container, backpack, 9);
        assertEquals(ExtractionResult.Status.NOTHING_TO_DO, result.getStatus());
    }

    @Test
    void extract_multipleRecipes_processesAll() {
        Recipe r1 = makeRecipe("minecraft:diamond_sword", 1, "minecraft:diamond", 1);
        Recipe r2 = makeRecipe("minecraft:iron_pickaxe", 1, "minecraft:iron_ingot", 1);
        manager.addRecipe(playerId, r1);
        manager.addRecipe(playerId, r2);
        List<ItemStack> container = List.of(
                ItemStack.of("minecraft:diamond", 1),
                ItemStack.of("minecraft:iron_ingot", 1)
        );
        ExtractionResult result = manager.extract(playerId, container, List.of(), 9);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getExtractedTypes());
        assertEquals(2, result.getTotalItems());
    }

    @Test
    void extract_disabledRecipe_skipped() {
        Recipe r = makeRecipe("minecraft:diamond_sword", 1, "minecraft:diamond", 3);
        manager.addRecipe(playerId, r);
        manager.setEnabled(playerId, r, false);
        List<ItemStack> container = List.of(ItemStack.of("minecraft:diamond", 10));
        ExtractionResult result = manager.extract(playerId, container, List.of(), 9);
        assertEquals(ExtractionResult.Status.EMPTY_PLAN, result.getStatus());
    }

    @Test
    void extract_partialContainerAvailable_extractsWhatCan() {
        manager.addRecipe(playerId, makeRecipe("minecraft:diamond_sword", 1, "minecraft:diamond", 5));
        // 背包有 0，容器只有 2 个（需要 5 个）
        List<ItemStack> container = List.of(ItemStack.of("minecraft:diamond", 2));
        List<ItemStack> backpack = List.of();
        ExtractionResult result = manager.extract(playerId, container, backpack, 9);
        // 能提取多少提取多少
        assertTrue(result.isSuccess());
        assertEquals(2, result.getTotalItems());
    }

    // ===== 辅助方法测试 =====

    @Test
    void aggregateItems_mergesSameRegistry() {
        List<ItemStack> items = List.of(
                ItemStack.of("minecraft:diamond", 2),
                ItemStack.of("minecraft:diamond", 3),
                ItemStack.of("minecraft:iron_ingot", 1)
        );
        var aggregated = manager.aggregateItems(items);
        assertEquals(5, aggregated.get("minecraft:diamond").intValue());
        assertEquals(1, aggregated.get("minecraft:iron_ingot").intValue());
    }

    @Test
    void countItem_returnsCorrectCount() {
        List<ItemStack> items = List.of(
                ItemStack.of("minecraft:diamond", 2),
                ItemStack.of("minecraft:diamond", 3)
        );
        assertEquals(5, manager.countItem(items, "minecraft:diamond"));
        assertEquals(0, manager.countItem(items, "minecraft:iron_ingot"));
    }

    // ===== 辅助方法 =====

    private Recipe makeRecipe(String result, int resultCount, String ingredient, int ingredientCount) {
        return Recipe.of(
                ItemStack.of(result, resultCount),
                List.of(ItemStack.of(ingredient, ingredientCount)),
                Recipe.Source.JEI
        );
    }
}
