# FTB Quest On Hand — 详细设计文档

> 目标读者：ftb-quest-on-hand 开发者（你我）
> 当前版本：v1.0.0
> 最后更新：2025-05-25

---

## 1. 概述

### 1.1 项目目标

在 Minecraft 原版/模组服务端游戏中，为工作台（Crafting Table）UI 添加一条可折叠侧边栏，提供以下核心能力：

1. **侧边栏收藏** — 展示、管理玩家收藏的物品
2. **FTB Quest → 侧边栏同步** — 一键将任务**目标物品**同步至侧边栏
3. **FTB Quest → JEI 书签同步** — 一键将任务**目标物品**同步至 JEI 书签
4. **外部 mod API** — 为 AE2、精致存储等提供标准接入接口

### 1.2 核心约束

- **核心层（core）**：纯 Java 17，无任何 Minecraft / mod 依赖
- **兼容层**：NeoForge 1.21+ / Forge 1.20.x / Fabric 1.20.x，三路独立实现
- **FTB Quest 版本**：支持 FTB Quests 1.20.x（Charm, Exodus, 等）
- **JEI 版本**：支持 JEI 1.20.x（NotEnoughIngredients / REI 兼容层暂不包含）

### 1.3 术语表

| 术语 | 定义 |
|------|------|
| 侧边栏（Sidebar） | 附加在工作台 UI 右侧的可折叠收藏面板 |
| 收藏槽（Slot） | 侧边栏中的物品格，每格放一个 ItemStack |
| 任务目标物品（Quest Objectives） | FTB Quest 任务中"收集 X 物品"类目标的所需物品 |
| 任务奖励物品（Quest Rewards） | 任务完成后发放的奖励物品（本文档**不涉及**） |
| 书签（Bookmark） | JEI 内置的物品收藏功能，通过 JEI API 操作 |
| 外部集成（External Integration） | 其他 mod 通过本 mod 提供的 API 接入收藏体系 |

---

## 2. 系统架构

### 2.1 模块划分

```
ftb-quest-on-hand/
│
├── core/                  # 纯 Java，无任何外部依赖
│   └── com.ftbquestonhand.core
│       └── BookmarkManager.java        ← 收藏数据CRUD、内存存储
│   └── com.ftbquestonhand.model
│       ├── ItemStack.java              ← 物品栈（registryName + count + NBT）
│       └── BookmarkCategory.java       ← 收藏分类枚举
│
├── api/                   # 纯接口 + 默认方法，无实现
│   └── com.ftbquestonhand.api
│       ├── QuestSync.java             ← FTB Quest 同步接口
│       ├── SidebarProvider.java      ← 侧边栏 UI 渲染接口
│       └── ExternalIntegration.java   ← 外部 mod 接入接口
│
├── neoforge/              # NeoForge 1.21+ 完整实现
├── forge/                 # Forge 1.20.x 完整实现
└── fabric/                # Fabric 1.20.x 完整实现
```

### 2.2 依赖关系

```
兼容层（neoforge/forge/fabric）
    ↓ implementation
api/（接口 + 默认实现）
    ↓ api dependency
core/（纯 Java 核心逻辑）
```

- **api 对 core 是 api 依赖**：core 的 public 类型成为 api 的 public 类型，兼容层可见
- **兼容层对 core/api 是 implementation 依赖**：内部实现细节不泄漏

### 2.3 状态存储设计

| 数据 | 存储位置 | 持久化 |
|------|---------|--------|
| 玩家侧边栏收藏 | `BookmarkManager` 内存 Map | 通过 CompatibleLevel 的 PlayerData 机制持久化到 world NBT |
| 玩家 JEI 书签 | JEI 内部 API | JEI 自己管理 |
| 外部 mod 注册信息 | `ExternalIntegrationRegistry` 内存 Set | 不持久化（运行时注册） |

---

## 3. 功能详解

### 3.1 侧边栏 UI

#### 3.1.1 布局

```
┌─────────────────────────┬──────────┐
│                         │  [≡]     │  ← 展开/收起按钮（右上角）
│      原版工作台          │          │
│       3×3 网格          │  [slot]  │
│                         │  [slot]  │
│                         │  [slot]  │
│                         │   ...    │
│                         │  [slot]  │  ← 最多 9 个收藏槽
└─────────────────────────┴──────────┘
        默认状态: 侧边栏隐藏（收起）
```

- **位置**：工作台 UI **右侧**，覆盖部分原版网格右侧空间
- **宽度**：30 像素（侧边栏本身）+ 按钮区域
- **初始状态**：隐藏（收起至按钮可见）
- **按钮**：工作台 UI 右上角固定位置，点击切换展开/收起，tooltip 显示 "FTB Quest On Hand"
- **展开后**：侧边栏从右向左滑入，带 200ms ease-out 动画
- **收起后**：仅按钮可见，点击重新展开

#### 3.1.2 收藏槽规格

- **槽数量**：每行 1 个，共 9 行，总计 **9 个收藏槽**
- **槽尺寸**：18×18 像素（与原版背包槽一致）
- **槽样式**：与原版物品槽外观一致，支持物品悬停tooltip
- **支持拖拽**：从侧边栏拖物品到工作台网格；从工作台/背包拖物品到侧边栏添加收藏
- **右键点击槽**：移除该槽物品

#### 3.1.3 交互逻辑

| 操作 | 行为 |
|------|------|
| 点击右上角按钮 | 展开/收起侧边栏（切换） |
| 左键点击空槽 | 等待玩家从背包/工作台拖入物品 |
| 左键点击有物品的槽 | 将物品"放置"到工作台（模拟点击，填充对应格子） |
| 右键点击有物品的槽 | 从收藏中移除该物品 |
| 拖拽物品到槽 | 添加到收藏 |
| 拖拽物品出槽 | 从收藏中移除 |
| 槽已满时拖入新物品 | 拒绝，播放"无效"音效 |

### 3.2 FTB Quest 集成

#### 3.2.1 读取目标物品（非奖励）

FTB Quests 中任务目标（Objectives）的类型包括：

| 目标类型 | 是否读取 |
|---------|---------|
| `ItemObjective`（收集物品） | ✅ 读取 `count` 个 `item` |
| `StatObjective`（统计） | ❌ 不读取 |
| `KillObjective`（击杀） | ❌ 不读取 |
| `CraftObjective`（合成） | ⚠️ 读取目标物品 |

**读取时机**：
- 玩家打开工作台 UI 时，按钮旁出现 **"同步"** 小按钮（⚡ 图标），提示有可同步的任务
- 玩家可选择同步**当前已接取但未完成**的任务目标物品，或**所有已接取任务**的目标物品
- 不同步已完成任务（任务进度已满视为已完成）

#### 3.2.2 同步流程

```
1. 玩家点击"同步"按钮
2. 弹出小型浮窗（单列列表），显示当前可同步的任务条目
   - 每条显示：任务图标 + 任务名 + 目标物品缩略图
3. 玩家勾选要同步的任务（支持多选），点击确认
4. 目标物品逐一加入侧边栏收藏（若已存在则跳过）
5. 同步完成后显示 Toast 提示："已同步 X 个物品到收藏"
```

#### 3.2.3 任务数据来源

通过 FTB Quests Mod API（`FTBQuestsAPI.getQuests()`）遍历玩家已接取任务，
筛选 `ItemObjective` 类型目标，提取 `item` 字段转换为 `ItemStack`。

### 3.3 JEI 书签同步

#### 3.3.1 行为

玩家点击"同步"后，JEI 书签列表中添加对应物品。
JEI 提供 `BookmarkPlugin` / `IGuiBookmarkPanel` API，
通过 `addBookmark(ItemStack)` 方法添加书签。

#### 3.3.2 与侧边栏收藏的关系

- JEI 书签和侧边栏收藏是**独立的两个集合**，同步操作可分别进行
- JEI 书签没有数量限制
- JEI 书签的持久化由 JEI 自身管理

### 3.4 外部 mod 接入 API

#### 3.4.1 注册机制

外部 mod 在初始化时通过以下方式注册：

```java
// 在外部 mod 的初始化阶段调用
ExternalIntegrationRegistry.register(myIntegration);
```

#### 3.4.2 可集成的功能

实现 `ExternalIntegration` 接口后，外部存储系统可以：

| 方法 | 作用 |
|------|------|
| `getModId()` | 返回唯一标识，如 `"ae2"`、`"refinedstorage"` |
| `fetchStoredItems()` | 将存储系统中的物品列表暴露给侧边栏/JEI 同步 |
| `containsItem(item)` | 查询某物品是否在外部存储中存在（用于高亮/筛选） |

#### 3.4.3 集成优先级

若多个外部 mod 注册了同一物品，以**注册顺序倒序**优先（即后注册的优先）。

---

## 4. 核心 API 设计

### 4.1 ItemStack（核心模型）

```java
package com.ftbquestonhand.model;

public final class ItemStack {
    String registryName;    // 物品注册名，如 "minecraft:diamond"
    int count;             // 数量
    String nbt;            // NBT JSON 字符串，可为 null

    // equals/hashCode/toString 实现
}
```

> ⚠️ 核心模型使用 String 表示物品注册名，兼容层负责与 Minecraft 的 `ResourceLocation` / `Identifier` 相互转换。

### 4.2 BookmarkManager（核心逻辑）

```java
package com.ftbquestonhand.core;

public class BookmarkManager {
    // === 侧边栏收藏 ===
    public static void addToSidebar(String playerId, ItemStack item);
    public static void removeFromSidebar(String playerId, ItemStack item);
    public static List<ItemStack> getSidebarBookmarks(String playerId);
    public static void setSidebarBookmarks(String playerId, List<ItemStack> items);
    public static void clearSidebar(String playerId);

    // === JEI 书签（核心层仅占位，JEI操作由兼容层实现）===
    public static void addToJei(String playerId, ItemStack item);
    public static void removeFromJei(String playerId, ItemStack item);
    public static List<ItemStack> getJeiBookmarks(String playerId);

    // === FTB Quest 同步 ===
    public static void syncQuestObjectivesToSidebar(String questId, List<ItemStack> objectives, String playerId);
    public static void syncQuestObjectivesToJei(String questId, List<ItemStack> objectives, String playerId);

    // === 外部集成 ===
    public static void registerExternal(ExternalIntegration external);
    public static List<ExternalIntegration> getRegisteredExternals();
}
```

### 4.3 QuestSync（API 接口）

```java
package com.ftbquestonhand.api;

public interface QuestSync {
    /**
     * 获取指定任务的 ItemObjective 类型目标物品
     */
    List<ItemStack> getQuestObjectives(String questId);

    /**
     * 获取玩家当前所有已接取且未完成的任务 ID 列表
     */
    List<String> getActiveQuests(String playerId);

    // === 默认实现：同步到侧边栏 / JEI ===
    default void syncToSidebar(String questId, String playerId) { ... }
    default void syncToJei(String questId, String playerId) { ... }
}
```

### 4.4 SidebarProvider（API 接口）

```java
package com.ftbquestonhand.api;

public interface SidebarProvider {
    /** 返回侧边栏当前显示的物品列表 */
    List<ItemStack> getDisplayItems(String playerId);

    /** 物品被点击时触发 */
    void onItemClicked(String playerId, ItemStack item, int slotIndex);

    /** 侧边栏展开/收起状态变更时触发 */
    default void onSidebarToggled(String playerId, boolean expanded) {}

    /** 获取侧边栏宽度（像素） */
    default int getSidebarWidth() { return 30; }

    /** 获取侧边栏高度（像素） */
    default int getSidebarHeight() { return 200; }
}
```

### 4.5 ExternalIntegration（API 接口）

```java
package com.ftbquestonhand.api;

public interface ExternalIntegration {
    /** 外部 mod ID，如 "ae2" */
    String getModId();

    /** 从外部存储拉取所有物品 */
    List<ItemStack> fetchStoredItems();

    /** 检查物品是否存在于外部存储 */
    boolean containsItem(ItemStack item);

    /** 显示名称 */
    default String getDisplayName() { return getModId(); }
}
```

---

## 5. 兼容层实现要点

### 5.1 NeoForge 1.21+

- **UI 系统**：使用 NeoForge 新 UI 系统（`Menu` / `Screen` 扩展，或 `UiTheme`）
- **FTB Quests API**：`dev.architectury:architectury-forge` 桥接，或直接依赖 `dev.ftb.mods:ftb-quests`
- **JEI API**：通过 `mezz.jei:jei-4FMLForge` 插件
- **PlayerData 持久化**：使用 NeoForge 的 `PlayerDataManager`（或 `PersistentCapabilities`）
- **事件总线**：NeoForge 事件总线（`NeoForge.EVENT_BUS`）

### 5.2 Forge 1.20.x

- **UI 系统**：Mixin + 替换原版 `CraftingScreen` / `GuiCrafting`
- **FTB Quests API**：`dev.ftb.mods:ftb-quests-forge` 依赖
- **JEI API**：`mezz.jei:jei-1FMLForge` 依赖
- **PlayerData 持久化**：Forge 的 `PersistentDataHandler`（`Capabilities`）
- **事件总线**：MinecraftForge 事件总线

### 5.3 Fabric 1.20.x

- **UI 系统**：Fabric 的 `ScreenRegistry` + Mixin
- **FTB Quests API**：`dev.ftb.mods:ftb-quests-fabric` 依赖
- **JEI**：Fabric 上用 REI（`mezzi.jei` 无 Fabric 支持），暂不包含 JEI 同步
- **WAIU**：Fabric 的 `ScreenProvider` / 手写原生 UI
- **PlayerData 持久化**：Fabric 的 `PersistentDataHandler`
- **事件总线**：Fabric 事件总线

---

## 6. 持久化方案

### 6.1 侧边栏数据格式（NBT）

每个玩家的收藏数据存储为以下 NBT 结构：

```nbt
{
    "ftb_quest_on_hand": {
        "sidebar": [
            { "item": "minecraft:diamond", "Count": 12, "tag": null },
            { "item": "minecraft:emerald", "Count": 5,  "tag": "{display:{Name:'\"Emerald\"'}}" }
        ]
    }
}
```

### 6.2 持久化时机

- 玩家退出世界时写入
- 每 60 秒定时写入（防止崩溃丢失）
- 通过 `PlayerEvent.SaveToFile` / `PlayerDataChangeEvent` 触发

---

## 7. 文件结构

```
ftb-quest-on-hand/
├── docs/
│   └── DESIGN.md               ← 本文档
│
├── core/src/main/java/com/ftbquestonhand/
│   ├── core/
│   │   └── BookmarkManager.java
│   └── model/
│       ├── ItemStack.java
│       └── BookmarkCategory.java
│
├── api/src/main/java/com/ftbquestonhand/
│   └── api/
│       ├── QuestSync.java
│       ├── SidebarProvider.java
│       └── ExternalIntegration.java
│
├── neoforge/src/main/java/com/ftbquestonhand/neoforge/
│   ├── FTBQuestOnHandNeoForge.java       ← mod 入口
│   ├── integration/
│   │   ├── NeoForgeQuestSync.java        ← FTB Quests API 实现
│   │   ├── NeoForgeSidebarProvider.java  ← 侧边栏 UI 实现
│   │   └── NeoForgeJeiIntegration.java   ← JEI 书签实现
│   ├── event/
│   │   ├── PlayerEvents.java
│   │   └── GuiEvents.java
│   └── data/
│       └── PlayerData.java
│
├── forge/src/main/java/com/ftbquestonhand/forge/
│   └── ... （结构同上）
│
├── fabric/src/main/java/com/ftbquestonhand/fabric/
│   └── ... （结构同上）
│
├── settings.gradle
├── build.gradle
└── README.md
```

---

## 8. 待确认 / 开放问题

| # | 问题 | 状态 |
|---|------|------|
| 1 | JEI 书签在 Fabric 侧是否改用 REI？ | 🔵 待确认 |
| 2 | 侧边栏是否需要支持分类 Tab（如"全部""任务""外部存储"）？ | 🔵 待确认 |
| 3 | 侧边栏收藏是否需要支持搜索/过滤？ | 🔵 待确认 |
| 4 | 是否需要支持服务端（Dedicated Server）运行？ | 🔵 待确认（无 UI 时降级为纯数据管理） |
| 5 | NeoForge 版本号最终确定（当前用 21.4.218）？ | 🔵 待确认 |

---

## 9. 里程碑

- [ ] **M1**: core + api 层完成，单模块可编译通过
- [ ] **M2**: NeoForge 兼容层基础结构，可打开/关闭侧边栏
- [ ] **M3**: FTB Quests 任务目标读取 + 同步到侧边栏
- [ ] **M4**: JEI 书签同步
- [ ] **M5**: 外部 mod API 注册机制 + 示例集成代码
- [ ] **M6**: Forge / Fabric 兼容层
- [ ] **M7**: 持久化 + 玩家数据迁移
- [ ] **M8**: CI/CD 自动构建 + 自动化测试
