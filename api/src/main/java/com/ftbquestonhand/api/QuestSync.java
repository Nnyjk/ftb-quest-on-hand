package com.ftbquestonhand.api;

import com.ftbquestonhand.model.ItemStack;
import java.util.List;

/**
 * FTB Quest 任务同步 API
 * 兼容所有支持 FTB Quests 的 mod 加载器
 */
public interface QuestSync {

    /**
     * 从 FTB Quests 任务书中拉取指定任务的奖励物品
     * @param questId 任务 ID
     * @return 奖励物品列表
     */
    List<ItemStack> getQuestRewards(String questId);

    /**
     * 将奖励物品一键同步到侧边栏
     */
    default void syncToSidebar(String questId, String playerId) {
        List<ItemStack> rewards = getQuestRewards(questId);
        com.ftbquestonhand.core.BookmarkManager.syncQuestRewardsToSidebar(questId, rewards, playerId);
    }

    /**
     * 将奖励物品一键同步到 JEI 收藏栏
     */
    default void syncToJei(String questId, String playerId) {
        List<ItemStack> rewards = getQuestRewards(questId);
        com.ftbquestonhand.core.BookmarkManager.syncQuestRewardsToJei(questId, rewards, playerId);
    }
}
