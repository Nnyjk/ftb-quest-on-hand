# FTB Quest On Hand → Plan Board

> 项目重构：JEI 书签同步 + 计划板自动提取工具
> 目标读者：开发者（你我）
> 版本：v2.0.0（重构方向）
> 最后更新：2025-05-25

---

## 1. 项目定位

### 1.1 核心价值

不依赖 FTB Quests，只依赖 JEI。

**标准玩家工作流：**
```
打开 JEI / REI
    → 浏览物品合成表
    → 将想要的物品加入书签（JEI 原生功能）
    → 关闭 JEI，打开容器，手动对照书签找物品

使用计划板后：
```
打开 JEI / REI
    → 将物品加入书签
    → 【计划板自动同步收到配方】
    → 关闭 JEI
    → 手持计划板，右键容器
    → 计划板自动提取缺失物品到背包
```

**核心价值**：消除"对照书签手动找物品"的重复劳动。

### 1.2 核心约束

- **仅支持 JEI**（NotEnoughIngredients，1.20.x）
- **不适配 NBT 物品**（含耐久度、组件、Forge 组件标签等），正式迭代前仅支持纯净物品
- **NeoForge / Forge / Fabric** 三路兼容
- **核心层纯 Java**，无任何 mod 依赖

### 1.3 术语表

| 术语 | 定义 |
|------|------|
| 计划板（Plan Board） | 本 mod 的核心物品，一个可存储配方的"物流清单" |
| 配方列表（Recipe List） | 计划板存储的多个配方，每个配方含若干材料需求 |
| 提取（Extract） | 从目标容器中将缺失材料搬运到玩家背包的动作 |
| JEI 同步钩子 | 监听 JEI 书签变更事件，将新书签自动加入计划板的机制 |

---

## 2. 功能范围

### 2.1 功能列表

| 功能 | 描述 | 优先级 |
|------|------|--------|
| **F1. JEI 书签监听** | 监听 JEI 书签变更，自动将新增物品的配方加入计划板 | P0 |
| **F2. 计划板物品** | 计划板物品实体，可查看配方列表 | P0 |
| **F3. 容器提取** | 手持计划板右键容器，自动提取缺失物品到背包 | P0 |
| **F4. GUI 配方管理** | 手动添加/删除/重排配方，支持拖拽 | P1 |
| **F5. 配方容量配置** | 通过 mod config 配置计划板最大配方数量 | P1 |

### 2.2 不包含的功能

- FTB Quests 集成
- JEI 书签上下文标注（不需要了，JEI 自己管书签）
- 工作台侧边栏
- 任务进度追踪
- NBT 物品支持

---

## 3. F1 — JEI 书签监听

### 3.1 触发机制

JEI 提供 `BookmarkEvent.Add` 和 `BookmarkEvent.Remove` 事件：

```java
// 伪代码
@SubscribeEvent
public void onBookmarkAdd(BookmarkEvent.Add event) {
    ItemStack item = event.getItemStack();
    if (!hasNbt(item)) {  // 暂不支持 NBT 物品
        planBoard.addRecipe(item);
    }
}
```

### 3.2 同步行为

当玩家在 JEI 中添加一个书签时：

1. 获取该物品的**主合成配方**（JEI `IRecipeManager.getCraftingRecipe`）
2. 从配方中提取所有材料（`ItemStack[] ingredients`）
3. 将 `{result: item, ingredients: [...]}` 存入计划板配方列表
4. 如果该配方已存在（相同 result），**不重复添加**
5. 如果配方列表已达容量上限，**静默拒绝**，不报错（避免干扰游戏）

### 3.3 边界情况

| 场景 | 行为 |
|------|------|
| 物品无合成配方（如原石、树叶） | 静默跳过，不添加，JEI 书签保留 |
| 物品有多个合成配方 | 只取第一个（craftingGrid 优先级最高） |
| 物品有 NBT 标签 | **暂不适配**，静默跳过 |
| 计划板容量已达上限 | 静默拒绝，不添加，不提示 |
| JEI 未安装 | 计划板功能降级为纯 GUI 管理，JEI 同步不生效 |

---

## 4. F2 — 计划板物品

### 4.1 物品定义

```java
// 计划板物品
public class PlanBoardItem {
    String displayName = "Plan Board";
    Item icon = Items.PAPER;  // 材质可自定义
    // 物品能力通过 Minecraft Capability 实现
}
```

### 4.2 配方列表存储格式（NBT）

```nbt
PlanBoard: compound {
    recipes: list [
        compound {
            result: compound {
                id: string,    // registryName
                Count: short
            },
            ingredients: list [
                compound { id: string, Count: short },
                ...
            ],
            source: string,   // "jei" | "manual"
            addedAt: long     // 时间戳
        }
    ]
}
```

### 4.3 物品 Tooltip（手持时显示）

```
Plan Board
---
配方数量：3 / 9          ← 当前/最大
JEI 同步：开启
---
Shift + 右键容器：提取缺失物品
Shift + 右键空气：打开配方管理
```

---

## 5. F3 — 容器提取

### 5.1 触发条件

- 玩家手持计划板
- Shift + 右键（具体按键可配置）
- 目标方块是一个容器（`IInventory` / `Menu` 实现）

### 5.2 提取流程

```
1. 读取计划板配方列表
2. 扫描玩家背包，生成 {registryName → count} 背包物品映射
3. 扫描目标容器，生成 {registryName → count} 容器物品映射
4. 对每个配方，按顺序（ingredients 数组顺序）：
   a. 计算该材料在配方中的总需求数量
   b. 计算当前持有数量（背包 + 容器）
   c. 如果持有数量 < 需求数量：
       i.   从容器提取（缺失数量 - 已持有数量），放入背包
       ii.  更新容器物品映射
       iii. 更新背包物品映射
   d. 如果持有数量 >= 需求数量：跳过
5. 如果背包满：停止提取，Toast："背包已满"
6. 提取完成：Toast："已补充 X 个物品"
```

### 5.3 提取顺序规则

**按配方添加顺序处理**，而非按依赖链拓扑排序。

例如：配方 A 需要 `[钻石锭×3, 木棍×2]`
- 先处理钻石锭，再处理木棍
- 这符合 Minecraft 合成顺序直觉（钻石→钻石锭→剑）

### 5.4 提取数量计算

```
需求数量 = 配方中该材料数量
已持有 = 背包中该物品数量 + 容器中该物品数量
提取数量 = max(0, 需求数量 - 已持有)
```

**不允许部分提取失败**：如果容器中物品数量不足所需提取量，提取**整个所需量**（从容器提取实际可提取的数量），不拆单。

### 5.5 边界情况

| 场景 | 行为 |
|------|------|
| 容器不是 IInventory（末影箱、潜影盒等） | 尝试支持；不支持则 Toast 报错 |
| 背包满 | 停止提取，Toast："背包已满，无法提取更多物品" |
| 配方中有 NBT 物品 | **暂不处理**，跳过该材料行 |
| 计划板为空 | Toast："计划板为空，请先添加配方" |
| 容器为空 | Toast："容器为空" |
| 所有物品都已满足 | Toast："所有物品已齐全" |
| 提取过程中容器物品被其他玩家拿走（多人） | 以提取时刻的快照为准，不做原子性保证 |

---

## 6. F4 — GUI 配方管理

### 6.1 打开方式

- 手持计划板 + **Shift + 右键空气**
- 手持计划板 + **Shift + 右键容器**（同时触发提取和管理，但先显示 GUI）

### 6.2 GUI 布局

```
┌────────────────────────────────────────────────────────────┐
│  📋 Plan Board — 配方管理                              [✕] │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  配方列表（3/9）                                           │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ [ ] 钻石剑        需求：钻石锭×3, 木棍×2        [🗑] │ │
│  │ [ ] 铁镐          需求：铁锭×3, 木棍×2          [🗑] │ │
│  │ [x] 剪刀          需求：铁锭×2                   [🗑] │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
│  [+ 从 JEI 书签添加]                                      │
│                                                            │
│  ── 选中配方详情 ──────────────────────────────────────  │
│  产物：钻石剑                                             │
│  材料：                                                    │
│    ◇ 钻石锭 ×3    [在背包: 5]  [在容器: 0]               │
│    ◇ 木棍   ×2    [在背包: 12] [在容器: 0]               │
│                                                            │
│                    [上移]  [下移]  [删除]                  │
└────────────────────────────────────────────────────────────┘
```

### 6.3 配方行交互

| 操作 | 行为 |
|------|------|
| 点击复选框 | 启用/禁用该配方（禁用的配方不参与提取） |
| 点击 🗑 删除 | 删除该配方，显示确认 |
| 拖拽行 | 上下拖动重排顺序 |
| 点击行 | 在下方显示详情 |

### 6.4 添加配方

点击 **"+ 从 JEI 书签添加"**：
- 打开 JEI 书签面板，勾选要添加的物品
- 确认后，批量将选中物品的配方添加到计划板
- 与 F1 的自动同步行为一致（只取主合成配方，去重）

### 6.5 手动添加

点击 **"+ 手动添加"**：
- 弹出搜索框，输入物品名
- 选择物品后，自动获取其合成配方并添加

---

## 7. F5 — 配方容量配置

### 7.1 配置文件

```toml
[general]
# 计划板最大配方数量
# 默认：9
maxRecipes = 9

# 是否启用 JEI 书签自动同步
# 默认：true
jeiSyncEnabled = true

# 提取快捷键
# 默认：SHIFT + RIGHT_CLICK
extractKey = "SHIFT + RIGHT_CLICK"
```

### 7.2 容量上限行为

- 新增配方时检查：`if (recipes.size() >= maxRecipes) return;`
- 达到上限时 Toast："配方已满（9/9），请删除不需要的配方"
- 上限可在配置文件中修改，无需重启

---

## 8. 架构设计

### 8.1 模块结构（保持不变）

```
ftb-quest-on-hand/
├── core/          # 纯 Java，无依赖
│   └── com.ftbquestonhand.core
│       └── PlanBoardManager.java    ← 配方存储、提取逻辑
│   └── com.ftbquestonhand.model
│       ├── ItemStack.java           ← 同前
│       ├── Recipe.java             ← 配方模型（result + ingredients）
│       └── ExtractionResult.java    ← 提取结果
│
├── api/           # 对外 API
│   └── com.ftbquestonhand.api
│       ├── JeiBookmarkListener.java ← JEI 书签监听接口
│       └── ContainerExtractor.java  ← 容器提取接口
│
├── neoforge/      # NeoForge 1.21+ 实现
├── forge/         # Forge 1.20.x 实现
└── fabric/        # Fabric 1.20.x 实现
```

### 8.2 核心类设计

```java
// Recipe.java
public final class Recipe {
    private final ItemStack result;
    private final List<ItemStack> ingredients;
    private final String source;  // "jei" | "manual"
    private final long addedAt;

    public ItemStack getResult();
    public List<ItemStack> getIngredients();
    public boolean isEnabled();   // GUI 中可禁用
}

// ExtractionResult.java
public final class ExtractionResult {
    private final int extractedCount;   // 成功提取的物品种类数
    private final int totalExtracted;   // 成功提取的物品总数量
    private final String message;       // 反馈给玩家的消息

    public boolean isSuccess();
    public boolean isInventoryFull();
}

// PlanBoardManager.java
public class PlanBoardManager {
    public static void addRecipe(String playerId, Recipe recipe);
    public static void removeRecipe(String playerId, Recipe recipe);
    public static List<Recipe> getRecipes(String playerId);
    public static void reorderRecipes(String playerId, int from, int to);
    public static void setRecipeEnabled(String playerId, Recipe recipe, boolean enabled);

    // 提取逻辑
    public static ExtractionResult extractFromContainer(
        String playerId,
        ItemStackHandler containerInventory,
        ItemStackHandler playerInventory
    );
}
```

### 8.3 JEI 集成接口

```java
// JeiBookmarkListener.java
public interface JeiBookmarkListener {
    /**
     * 当玩家在 JEI 中添加书签时调用
     * @param player  玩家
     * @param item    被添加书签的物品
     * @param recipe  该物品的主合成配方（可能为 null）
     */
    void onBookmarkAdded(ServerPlayer player, ItemStack item, @Nullable Recipe recipe);
}
```

### 8.4 容器提取接口

```java
// ContainerExtractor.java
public interface ContainerExtractor {
    /**
     * 从容器提取计划板所需的缺失物品
     * @param holder   容器方块的 IItemHandler
     * @param board    计划板物品（存储配方列表）
     * @param player   执行操作的玩家
     * @return 提取结果
     */
    ExtractionResult extract( IItemHandler holder,
                               ItemStack board,
                               ServerPlayer player);
}
```

---

## 9. 交互设计总结

### 9.1 完整用户流程

```
第一天使用：
1. 打开 JEI，找到想要的物品，加入书签
2. 计划板自动收到配方（JEI 同步）
3. 打开容器，手持计划板 Shift+右键
4. 缺失物品自动进背包

日常使用：
1. JEI 中添加书签 → 自动同步
2. Shift+右键容器 → 自动提取
3. Shift+右键空气 → 管理配方列表
```

### 9.2 快捷键

| 快捷键 | 场景 | 行为 |
|--------|------|------|
| Shift + 右键容器 | 手持计划板 | 提取缺失物品到背包 |
| Shift + 右键空气 | 手持计划板 | 打开配方管理 GUI |
| 复选框点击 | GUI 内 | 启用/禁用配方 |
| 拖拽行 | GUI 内 | 重排配方顺序 |

---

## 10. 待确认问题

| # | 问题 | 状态 |
|---|------|------|
| 1 | 计划板物品材质/外观？ | 🔵 待讨论 |
| 2 | GUI 配方管理是否需要图标预览（物品贴图）？ | 🔵 待讨论 |
| 3 | 多人游戏中，容器提取是否需要考虑其他玩家同时拿物品？ | 🔵 待讨论（暂不处理） |
| 4 | 是否支持漏斗/管道等自动化的提取输出（而非只进背包）？ | 🔵 未来扩展，暂不支持 |
| 5 | REI 在 Fabric 上是否需要同等支持？ | 🔵 待讨论 |

---

## 11. 里程碑

- [ ] **M1**: core + api 层完成，PlanBoardManager 提取逻辑可单元测试
- [ ] **M2**: NeoForge 兼容层，计划板物品 + GUI 可用
- [ ] **M3**: JEI 书签监听集成
- [ ] **M4**: 容器提取功能
- [ ] **M5**: GUI 配方管理（手动添加/删除/重排）
- [ ] **M6**: Forge / Fabric 兼容层
- [ ] **M7**: 配置文件支持
- [ ] **M8**: CI/CD 自动构建
