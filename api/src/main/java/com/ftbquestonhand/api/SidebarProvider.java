package com.ftbquestonhand.api;

import com.ftbquestonhand.model.ItemStack;
import java.util.List;

/**
 * 侧边栏 UI 注册 API
 * 各兼容层实现此接口以提供特定平台的 UI
 */
public interface SidebarProvider {

    /**
     * 返回侧边栏显示的收藏列表
     */
    List<ItemStack> getDisplayItems(String playerId);

    /**
     * 处理物品点击事件
     */
    void onItemClicked(String playerId, ItemStack item, int slotIndex);

    /**
     * 获取侧边栏宽度（像素）
     */
    default int getSidebarWidth() { return 30; }

    /**
     * 获取侧边栏高度（像素）
     */
    default int getSidebarHeight() { return 200; }
}
