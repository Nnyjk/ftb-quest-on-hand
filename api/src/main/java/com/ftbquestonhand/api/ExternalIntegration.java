package com.ftbquestonhand.api;

import com.ftbquestonhand.model.ItemStack;
import java.util.List;

/**
 * 外部 mod 集成 API
 * 其他 mod（如 AE2、精致存储）可实现此接口接入收藏体系
 */
public interface ExternalIntegration {

    /**
     * 返回支持的外部 mod ID（如 "ae2", "refinedstorage"）
     */
    String getModId();

    /**
     * 从外部存储系统拉取物品列表
     */
    List<ItemStack> fetchStoredItems();

    /**
     * 查询某物品是否存在于外部存储中
     */
    boolean containsItem(ItemStack item);

    /**
     * 获取存储系统的显示名称
     */
    default String getDisplayName() {
        return getModId();
    }
}
