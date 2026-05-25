package com.ftbquestonhand.core;

import com.ftbquestonhand.model.ItemStack;
import com.ftbquestonhand.model.BookmarkCategory;

import java.util.*;

/**
 * 收藏栏管理器 - 核心纯 Java 实现，无任何 mod 依赖
 */
public class BookmarkManager {

    private static final Map<String, List<ItemStack>> SIDEBAR_BOOKMARKS = new HashMap<>();
    private static final Map<String, List<ItemStack>> JEI_BOOKMARKS = new HashMap<>();

    /**
     * 添加物品到工作台侧边栏收藏
     */
    public static void addToSidebar(String playerId, ItemStack item) {
        SIDEBAR_BOOKMARKS
            .computeIfAbsent(playerId, k -> new ArrayList<>())
            .add(item);
    }

    /**
     * 从工作台侧边栏收藏移除物品
     */
    public static void removeFromSidebar(String playerId, ItemStack item) {
        List<ItemStack> list = SIDEBAR_BOOKMARKS.get(playerId);
        if (list != null) {
            list.remove(item);
        }
    }

    /**
     * 获取玩家侧边栏收藏列表（不可变视图）
     */
    public static List<ItemStack> getSidebarBookmarks(String playerId) {
        return Collections.unmodifiableList(
            SIDEBAR_BOOKMARKS.getOrDefault(playerId, Collections.emptyList())
        );
    }

    /**
     * 添加物品到 JEI 收藏栏
     */
    public static void addToJei(String playerId, ItemStack item) {
        JEI_BOOKMARKS
            .computeIfAbsent(playerId, k -> new ArrayList<>())
            .add(item);
    }

    /**
     * 从 JEI 收藏栏移除物品
     */
    public static void removeFromJei(String playerId, ItemStack item) {
        List<ItemStack> list = JEI_BOOKMARKS.get(playerId);
        if (list != null) {
            list.remove(item);
        }
    }

    /**
     * 获取玩家 JEI 收藏列表（不可变视图）
     */
    public static List<ItemStack> getJeiBookmarks(String playerId) {
        return Collections.unmodifiableList(
            JEI_BOOKMARKS.getOrDefault(playerId, Collections.emptyList())
        );
    }

    /**
     * 同步 FTB Quest 任务奖励物品到侧边栏
     * @param questId   任务 ID
     * @param rewards   任务奖励物品列表
     * @param playerId  玩家 ID
     */
    public static void syncQuestRewardsToSidebar(String questId, List<ItemStack> rewards, String playerId) {
        List<ItemStack> list = SIDEBAR_BOOKMARKS.computeIfAbsent(playerId, k -> new ArrayList<>());
        for (ItemStack reward : rewards) {
            if (!list.contains(reward)) {
                list.add(reward);
            }
        }
    }

    /**
     * 同步 FTB Quest 任务奖励物品到 JEI 收藏
     */
    public static void syncQuestRewardsToJei(String questId, List<ItemStack> rewards, String playerId) {
        List<ItemStack> list = JEI_BOOKMARKS.computeIfAbsent(playerId, k -> new ArrayList<>());
        for (ItemStack reward : rewards) {
            if (!list.contains(reward)) {
                list.add(reward);
            }
        }
    }

    /**
     * 清空玩家所有收藏数据（登出/重置时调用）
     */
    public static void clearPlayer(String playerId) {
        SIDEBAR_BOOKMARKS.remove(playerId);
        JEI_BOOKMARKS.remove(playerId);
    }

    /**
     * 按分类获取收藏（分类可能来自任务书章节或标签）
     */
    public static List<ItemStack> getByCategory(String playerId, BookmarkCategory category) {
        // 分类筛选逻辑，核心层只做简单过滤
        return getSidebarBookmarks(playerId);
    }
}
