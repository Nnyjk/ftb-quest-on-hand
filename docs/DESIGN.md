# Plan Board — 设计文档

> 架构设计与模块规划
> 目标读者：开发者（你我）
> 版本：v2.0.0

---

## 1. 项目概述

**Plan Board** 是一个 Minecraft mod，通过监听 JEI 书签自动同步配方，并提供手持计划板右键容器自动提取缺失物品的功能。

**核心价值**：消除"对照 JEI 书签手动从容器找物品"的重复劳动。

---

## 2. 核心约束

- **仅支持 JEI**（NotEnoughIngredients，1.20.x），Fabric 侧 REI 暂不包含
- **不适配 NBT 物品**（含耐久度、组件标签等），正式迭代前仅支持纯净物品
- **NeoForge / Forge / Fabric** 三路兼容
- **核心层纯 Java**，无任何 Minecraft / mod 依赖

---

## 3. 模块架构

```
plan-board/
│
├── core/                          # 纯 Java，无外部依赖
│   └── com/planboard/core
│       ├── PlanBoardManager.java   ← 配方存储、提取逻辑核心
│       └── model/
│           ├── ItemStack.java      ← 物品栈
│           ├── Recipe.java         ← 配方（产物 + 材料列表）
│           └── ExtractionResult.java
│
├── api/                           # 对外 API 接口
│   └── com/planboard/api
│       ├── JeiBookmarkListener.java   ← JEI 书签监听
│       └── ContainerExtractor.java     ← 容器提取
│
├── neoforge/                      # NeoForge 1.21+ 实现
│   └── com/planboard/neoforge
│       ├── PlanBoardMod.java      ← mod 入口
│       ├── item/
│       │   └── PlanBoardItem.java ← 物品定义 + Capability
│       ├── gui/
│       │   └── PlanBoardScreen.java
│       ├── integration/
│       │   ├── JeiListener.java   ← JEI 事件监听
│       │   └── ContainerExtract.java
│       └── events/
│           └── PlayerEvents.java
│
├── forge/                         # Forge 1.20.x 实现
├── fabric/                        # Fabric 1.20.x 实现
│
├── settings.gradle
├── build.gradle
└── README.md
```

### 3.1 依赖方向

```
兼容层（neoforge/forge/fabric）
    ↓ implementation
api/（接口）
    ↓ api dependency
core/（纯 Java 核心）
```

---

## 4. 核心 API 设计

### 4.1 Recipe

```java
public final class Recipe {
    private final ItemStack result;              // 产物
    private final List<ItemStack> ingredients;    // 材料列表，按合成顺序排列
    private final String source;                  // "jei" | "manual"
    private final long addedAt;

    public ItemStack getResult();
    public List<ItemStack> getIngredients();
    public String getSource();
    public long getAddedAt();
}
```

### 4.2 ItemStack

```java
public final class ItemStack {
    private final String registryName;  // "minecraft:diamond"
    private final int count;

    // 无 NBT（正式迭代前不支持）
}
```

### 4.3 ExtractionResult

```java
public final class ExtractionResult {
    private final int extractedTypes;    // 成功提取的物品种类数
    private final int totalItems;        // 成功提取的物品总数量
    private final boolean inventoryFull;
    private final String message;       // Toast 消息

    public boolean isSuccess();
    public boolean isInventoryFull();
    public String getMessage();
}
```

### 4.4 PlanBoardManager（提取逻辑）

```java
public class PlanBoardManager {

    // === 配方管理 ===
    public static void addRecipe(String playerId, Recipe recipe);
    public static boolean removeRecipe(String playerId, Recipe recipe);
    public static List<Recipe> getRecipes(String playerId);
    public static void reorder(String playerId, int from, int to);
    public static void setEnabled(String playerId, Recipe recipe, boolean enabled);

    // === 容量 ===
    public static int getMaxCapacity(String playerId);  // 从 config 读取

    // === 提取（核心逻辑）===
    // containerInventory: 容器物品列表
    // playerInventory:   玩家背包物品列表
    public static ExtractionResult extract(
        String playerId,
        List<ItemStack> containerInventory,
        List<ItemStack> playerInventory
    );
}
```

**extract 算法**：

```
输入: recipe.ingredients[], containerInventory, playerInventory
输出: ExtractionResult

背包已满标志 = false
已提取总数 = 0

for each ingredient in recipe.ingredients (按顺序):
    required = ingredient.count
    have_in_backpack = sum(playerInventory[registryName])
    have_in_container = sum(containerInventory[registryName])
    total_have = have_in_backpack + have_in_container

    if total_have >= required:
        continue  // 该材料已满足

    need = required - total_have
    can_take_from_container = sum(containerInventory[registryName])

    if can_take_from_container == 0:
        continue  // 容器里没有，无法提取

    take = min(need, can_take_from_container)

    if backpack_has_space(take):
        move take items from container to backpack
        extracted += take
        total_items += take
    else:
        inventory_full = true
        break

return ExtractionResult(extracted, total_items, inventory_full, message)
```

---

## 5. 兼容层职责

### 5.1 NeoForge 1.21+

- **物品系统**：NeoForge Item / CreativeTab
- **Capability**：通过 `IItemStackHandler` 存储配方数据
- **JEI 集成**：` mezz.jei:jei-4FMLForge` API，监听 `BookmarkEvent.Add`
- **容器检测**：遍历 `BlockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER)`
- **GUI**：NeoForge 原生 GUI 系统
- **配置**：NeoForge 的 `Configuration` / TOML 配置

### 5.2 Forge 1.20.x

- **物品系统**：Forge Item
- **Capability**：通过 `ItemStack.getOrCreateTag()` + 持久化
- **JEI 集成**：`mezz.jei:jei-1FMLForge`
- **容器检测**：同 NeoForge
- **GUI**：Mixin 替换原版 ContainerScreen

### 5.3 Fabric 1.20.x

- **物品系统**：Fabric ItemRegistry
- **持久化**：Fabric 的 `PersistentDataHandler`
- **JEI**：Fabric 上使用 REI（`mezzi.jei` 无 Fabric 版本）
  - REI 书签事件：`com.blamepajamas.jei.events.BookmarkAddEvent`
  - 同一接口 `JeiBookmarkListener`，不同实现
- **容器检测**：Fabric Transfer API（`FabricContainerApi`）
- **GUI**：Fabric 原生 Screen

---

## 6. 数据持久化

### 6.1 NBT 结构

```nbt
PlanBoardData: compound {
    recipes: list [
        {
            result: { id: string, Count: short },
            ingredients: [ { id: string, Count: short }, ... ],
            source: string,   // "jei" | "manual"
            enabled: byte,    // 0/1
            addedAt: long
        }
    ]
}
```

### 6.2 存储位置

- 通过 `ItemStack.getOrCreateTag()` 存储在物品 NBT 中
- 计划板物品丢失 = 数据丢失（与玩家行为一致）

### 6.3 写入时机

- 配方添加/删除/重排：立即写入
- 提取操作：**不修改配方数据**，只读写容器和背包

---

## 7. 配置文件

```toml
[general]
# 计划板最大配方数量
maxRecipes = 9

# 是否启用 JEI 书签自动同步
jeiSyncEnabled = true

[extraction]
# 是否优先从背包已有物品计算
# true = 只从容器提取"背包+容器仍不满足"的部分
# false = 优先从容器提取（可能产生物品堆叠）
preferContainer = false

[compatibility]
# 是否忽略 NBT 物品（不添加也不报错）
ignoreNbtItems = true
```

---

## 8. 待确认问题

| # | 问题 | 状态 |
|---|------|------|
| 1 | 计划板物品材质 | 🔵 待定 |
| 2 | GUI 配方管理是否需要物品图标 | 🔵 待定 |
| 3 | REI 书签在 Fabric 上的等效事件 | 🔵 待研究 |
| 4 | 多人游戏容器并发处理 | 🔵 暂不处理 |
