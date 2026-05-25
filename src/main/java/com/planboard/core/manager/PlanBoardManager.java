package com.planboard.core.manager;

import com.planboard.core.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 计划板管理器 — 核心纯 Java 提取逻辑
 * 可在无 Minecraft 环境下独立单元测试
 */
public class PlanBoardManager {

    private final Map<String, List<Recipe>> playerRecipes = new HashMap<>();
    private final Map<String, String> focusedPlayer = new HashMap<>();

    // --- 配方管理 ---

    public void addRecipe(String playerId, Recipe recipe) {
        playerRecipes.computeIfAbsent(playerId, k -> new ArrayList<>()).add(recipe);
    }

    public boolean removeRecipe(String playerId, Recipe recipe) {
        List<Recipe> list = playerRecipes.get(playerId);
        if (list == null) return false;
        return list.remove(recipe);
    }

    public List<Recipe> getRecipes(String playerId) {
        return List.copyOf(playerRecipes.getOrDefault(playerId, Collections.emptyList()));
    }

    public List<Recipe> getEnabledRecipes(String playerId) {
        return playerRecipes.getOrDefault(playerId, Collections.emptyList())
                .stream()
                .filter(Recipe::isEnabled)
                .collect(Collectors.toList());
    }

    public void reorder(String playerId, int from, int to) {
        List<Recipe> list = playerRecipes.get(playerId);
        if (list == null || from < 0 || to < 0 || from >= list.size() || to >= list.size()) {
            return;
        }
        Collections.swap(list, from, to);
    }

    public void setEnabled(String playerId, Recipe recipe, boolean enabled) {
        List<Recipe> list = playerRecipes.get(playerId);
        if (list == null) return;
        for (Recipe r : list) {
            if (r.equals(recipe)) {
                r.setEnabled(enabled);
                break;
            }
        }
    }

    public void clear(String playerId) {
        playerRecipes.remove(playerId);
    }

    public int recipeCount(String playerId) {
        return playerRecipes.getOrDefault(playerId, Collections.emptyList()).size();
    }

    // --- 提取逻辑（核心算法）---

    /**
     * 从容器中提取缺失物品到背包
     *
     * @param playerId          玩家 ID
     * @param containerItems    容器内所有物品
     * @param backpackItems     玩家背包内所有物品
     * @param maxCapacity       计划板最大配方数量
     * @return 提取结果
     */
    public ExtractionResult extract(
            String playerId,
            List<ItemStack> containerItems,
            List<ItemStack> backpackItems,
            int maxCapacity
    ) {
        List<Recipe> recipes = getEnabledRecipes(playerId);

        if (recipes.isEmpty()) {
            return ExtractionResult.emptyPlan();
        }

        // 聚合容器内和背包内的物品数量
        Map<String, Integer> containerCount = aggregateItems(containerItems);
        Map<String, Integer> backpackCount = aggregateItems(backpackItems);

        int totalExtracted = 0;
        int extractedTypes = 0;
        boolean inventoryFull = false;

        for (Recipe recipe : recipes) {
            for (ItemStack ingredient : recipe.getIngredients()) {
                String reg = ingredient.getRegistryName();
                int need = ingredient.getCount();

                int inBackpack = backpackCount.getOrDefault(reg, 0);
                int inContainer = containerCount.getOrDefault(reg, 0);
                int totalHave = inBackpack + inContainer;

                if (totalHave >= need) {
                    continue; // 该材料已满足
                }

                int missing = need - totalHave;

                // 优先从背包已有的满足（减少搬运）
                // 缺失的部分从容器提取
                int canTakeFromContainer = inContainer;

                if (canTakeFromContainer == 0) {
                    continue; // 容器里没有，无法提取
                }

                int take = Math.min(missing, canTakeFromContainer);

                // 模拟检查背包是否有空间（假设背包无限大，由调用方判断）
                // 这里只计算提取数量，不做实际搬运

                containerCount.put(reg, inContainer - take);
                backpackCount.put(reg, inBackpack + take);

                totalExtracted += take;
                if (take > 0) extractedTypes++;
            }

            if (inventoryFull) break;
        }

        if (totalExtracted == 0) {
            return ExtractionResult.nothingToDo();
        }

        return new ExtractionResult(
                inventoryFull ? ExtractionResult.Status.INVENTORY_FULL : ExtractionResult.Status.SUCCESS,
                extractedTypes,
                totalExtracted,
                inventoryFull
                        ? "Inventory full! Extracted " + totalExtracted + " items"
                        : "Extracted " + totalExtracted + " items (" + extractedTypes + " types)"
        );
    }

    // --- 辅助方法 ---

    /**
     * 聚合相同 registryName 的物品数量
     */
    public Map<String, Integer> aggregateItems(List<ItemStack> items) {
        Map<String, Integer> map = new HashMap<>();
        for (ItemStack item : items) {
            if (item == null) continue;
            map.merge(item.getRegistryName(), item.getCount(), Integer::sum);
        }
        return map;
    }

    /**
     * 检查某个物品是否在列表中（按 registryName 匹配）
     */
    public boolean containsItem(List<ItemStack> items, String registryName) {
        return items.stream().anyMatch(i -> i.getRegistryName().equals(registryName));
    }

    /**
     * 获取某个物品在列表中的总数量
     */
    public int countItem(List<ItemStack> items, String registryName) {
        return items.stream()
                .filter(i -> i.getRegistryName().equals(registryName))
                .mapToInt(ItemStack::getCount)
                .sum();
    }
}
