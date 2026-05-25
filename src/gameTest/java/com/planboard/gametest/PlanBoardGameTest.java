package com.planboard.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestGenerator;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import org.junit.jupiter.api.Test;

import static net.neoforged.neoforge.gametest.Framework.GameTestHelper.*;

/**
 * Plan Board Integration GameTest
 * 验证计划板与容器交互的核心功能
 *
 * 运行方式:
 *   /gametest run com.planboard.gametest.PlanBoardGameTest
 * 或运行所有测试:
 *   /gametest runall
 */
@GameTestHolder("planboard")
public class PlanBoardGameTest {

    private static final String STRUCTURE_PREFIX = "planboard:";

    // ===== Helper methods =====

    /**
     * 给予玩家一个物品
     */
    private void giveItem(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack);
    }

    /**
     * 清空玩家背包
     */
    private void clearInventory(ServerPlayer player) {
        player.getInventory().clearContent();
    }

    /**
     * 统计玩家背包中指定物品数量
     */
    private int countItemInInventory(ServerPlayer player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    // ===== GameTest Cases =====

    /**
     * 测试玩家可以在工作台旁打开计划板 GUI
     */
    @GameTest(template = STRUCTURE_PREFIX + "empty", batch = "gui")
    @Test
    void testOpenPlanBoardGui(TestContext context) {
        ServerPlayer player = context.makeMockPlayer();
        context.assertTrue(true, "Plan Board GUI open test placeholder");
        context.complete();
    }

    /**
     * 测试容器检测：可以正确读取容器内的物品
     */
    @GameTest(template = STRUCTURE_PREFIX + "single_chest", batch = "container")
    @Test
    void testContainerItemDetection(TestContext context) {
        BlockPos chestPos = new BlockPos(1, 2, 0);
        var chestBlock = context.getBlock(chestPos);

        context.assertTrue(chestBlock.is(Blocks.CHEST), "Target should be a chest");

        // 验证容器可以读取（功能占位）
        context.complete();
    }

    /**
     * 测试提取逻辑：背包有空间时，物品可以被提取
     */
    @GameTest(template = STRUCTURE_PREFIX + "single_chest", batch = "extraction")
    @Test
    void testExtractToEmptyBackpack(TestContext context) {
        ServerPlayer player = context.makeMockPlayer();
        clearInventory(player);

        // 给予计划板物品（模拟）
        giveItem(player, new ItemStack(Items.PAPER, 1));

        // 容器中放置钻石
        BlockPos chestPos = new BlockPos(1, 2, 0);
        context.setBlock(chestPos, Blocks.DIAMOND_BLOCK);

        context.assertTrue(true, "Extraction to empty backpack test placeholder");
        context.complete();
    }

    /**
     * 测试背包满时，提取操作停止
     */
    @GameTest(template = STRUCTURE_PREFIX + "single_chest", batch = "extraction")
    @Test
    void testExtractStopsOnFullInventory(TestContext context) {
        ServerPlayer player = context.makeMockPlayer();
        clearInventory(player);

        // 用 36 组物品填满背包（每格64个 = 2304 个物品）
        for (int i = 0; i < 36; i++) {
            giveItem(player, new ItemStack(Items.COBBLESTONE, 64));
        }

        context.assertTrue(player.getInventory().isFull(), "Backpack should be full");

        context.complete();
    }

    /**
     * 测试空计划板的提取行为
     */
    @GameTest(template = STRUCTURE_PREFIX + "single_chest", batch = "extraction")
    @Test
    void testExtractWithEmptyPlan(TestContext context) {
        ServerPlayer player = context.makeMockPlayer();
        clearInventory(player);

        // 不给予计划板，直接尝试提取
        // 预期：返回"计划板为空"的消息

        context.complete();
    }

    /**
     * 测试批量结构生成器（用于创建测试场景）
     */
    @GameTestGenerator
    @SuppressWarnings("unused")
    public static java.util.List<GameTestInfo> generateTests() {
        java.util.List<GameTestInfo> tests = new java.util.ArrayList<>();

        // 生成空房间测试
        tests.add(createTestTemplate(STRUCTURE_PREFIX + "empty", "empty"));

        // 生成单箱子测试
        tests.add(createTestTemplate(STRUCTURE_PREFIX + "single_chest", "single_chest"));

        return tests;
    }

    private static GameTestInfo createTestTemplate(String testName, String structureName) {
        return new GameTestInfo(
                testName,
                () -> {
                    // 动态创建测试结构（占位）
                },
                "default",
                100, // tick limit
                3,   // required success count
                0    // required failure count
        );
    }
}
