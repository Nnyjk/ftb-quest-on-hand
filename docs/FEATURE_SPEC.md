# FTB Quest On Hand — 功能详细规格

> 每个功能的完整交互逻辑、状态定义、边界情况
> 目标读者：开发者（你我）

---

## F1. 批量添加 JEI 书签

### F1.1 触发入口

在 **FTB Quests 任务书界面**中，每个任务条目的右侧区域增加一个按钮：

```
┌─────────────────────────────────────────────────────┐
│ [★] 制作一把钻石剑                    [任务书图标]    │
│ 奖励：附魔台 ×1                         [+] [⚡]     │  ← [⚡] 即批量同步按钮
└─────────────────────────────────────────────────────┘
                                                    ↑
                                            鼠标悬停 tooltip："同步到 JEI 书签"
```

- **按钮位置**：任务书任务条目的右上角，与"领取奖励"按钮并排
- **按钮样式**：⚡ 图标（Lightning bolt），与 JEI 书签的星标风格统一
- **悬停提示**：`"同步到 JEI 书签 (X 个物品)"` — 实时显示待同步物品数量

### F1.2 点击后的行为

#### 步骤 1：确认浮窗

点击 ⚡ 按钮后，弹出小型确认浮窗：

```
┌────────────────────────────────┐
│ ⚡ 同步到 JEI 书签              │
│                                │
│  将添加以下物品到 JEI 书签：    │
│                                │
│  ◇ 钻石锭 ×3                   │
│  ◇ 木棍   ×2                   │
│  ◇ 铁锭   ×5                   │
│  ◇ 红石   ×10                  │
│                                │
│  已存在的物品将自动跳过         │
│                                │
│      [确认]      [取消]         │
└────────────────────────────────┘
```

#### 步骤 2：执行同步

点击 **确认** 后：

1. 遍历该任务所有 `ItemObjective`
2. 对每个目标物品，检查是否已存在于玩家 JEI 书签中
3. 未存在的物品调用 JEI API 添加到书签
4. 已存在的物品静默跳过（不报错，不显示）
5. 显示完成 Toast：`"已同步 N 个物品到 JEI 书签"`

#### 步骤 3：结果反馈

```
成功（全部新增）：
  Toast: "✓ 已同步 4 个物品到 JEI 书签"

部分跳过（部分已存在）：
  Toast: "✓ 已同步 2 个物品（2 个已存在）"

全部已存在（无新增）：
  Toast: "⚡ 所有物品已在书签中"（保持 2 秒后消失）

失败（JEI 未安装 / API 不可用）：
  Toast: "✗ 同步失败：JEI 不可用"（红色警告）
```

### F1.3 边界情况

| 场景 | 行为 |
|------|------|
| 任务无 ItemObjective（只有统计/击杀目标） | 按钮显示但禁用，悬停提示"该任务无物品目标" |
| JEI 未安装 | 按钮仍显示，Toast 报错，不崩溃 |
| 玩家没有打开 JEI 的书签面板 | 同步仍然成功（JEI 内部数据更新，下次打开书签时可见） |
| 玩家正在查看 JEI 书签界面 | 书签列表实时刷新显示新增条目（如果 JEI 支持运行时刷新） |
| 任务已完成（所有目标达成） | 按钮仍然可用，允许重复同步（适合重新开始任务时使用） |
| 物品有 NBT（如耐久、组件） | 以 `registryName + NBT` 为去重键，相同 NBT 才算"已存在" |

### F1.4 快捷键

- **Shift + 点击任务奖励图标**：触发该任务的批量同步（无需移动鼠标到按钮）
- **右键点击 ⚡ 按钮**：打开 JEI 并定位到第一个待同步物品（需要 JEI 已安装）

---

## F2. 书签上下文标注

### F2.1 标注内容

每个来自 FTB Quests 的书签条目，在 JEI 物品Tooltip 下方附加额外行：

```
┌─────────────────────────────────────────────────────┐
│ 钻石锭                                          ⭐ │
│ [物品图标]                                         │
│                                                [☆] │
├─────────────────────────────────────────────────────┤
│  1/3                                            │  ← 第一行：数量（未完成）
│  📖 任务：制作一把钻石剑                         │  ← 第二行：任务来源
└─────────────────────────────────────────────────────┘
```

**Tooltip 分行结构**：

```
第1行：物品显示名 + 数量    （原版 JEI）
第2行：[📖 制作一把钻石剑]   （新增强制标注，蓝色链接样式）
第3行：任务进度（如果能读取到） （可选）
```

### F2.2 过滤按钮

在 JEI 书签面板的顶部增加过滤开关：

```
┌─────────────────────────────────────────────────────┐
│ ⭐ 书签                          [📖 仅显示任务书签] │
│                                                     │
│  全部书签（42）  |  任务书签（12）  |  手动书签（30）│
└─────────────────────────────────────────────────────┘
```

- **全部书签**：原始 JEI 行为，显示所有书签
- **任务书签**：只显示通过 FTB Quest On Hand 同步的条目
- **手动书签**：只显示玩家手动逐个添加的条目（通过 JEI 原生方式添加的）

### F2.3 任务书签识别机制

判断一个书签是否"来自任务"的依据：

在添加 JEI 书签时，将以下信息写入 JEI 书签的**内部扩展数据**：

```java
// 写入 JEI 书签的扩展数据
BookmarkData {
    String source = "ftb_quest";          // 固定值，标识来源
    String questId = "chapter_1/quest_5"; // FTB Quest 内部 ID
    String questName = "制作一把钻石剑";   // 任务显示名（用于纯文本显示）
    long addedAt = System.currentTimeMillis();
}
```

**识别方式**：读取书签时，检查 `source == "ftb_quest"` 字段存在则为任务书签。

### F2.4 边界情况

| 场景 | 行为 |
|------|------|
| 任务被删除 / 任务书更新后任务消失 | 书签仍保留原标注，Tooltip 显示任务名为最后已知值（缓存） |
| 玩家手动在 JEI 中删除了某个任务书签 | 不触发任何事件，正常删除 |
| 任务被修改（目标物品变更） | 已添加的书签不变（新物品需要再次同步） |
| 同一物品被多个任务引用 | 每个任务各产生一条书签（JEI 支持同一物品多条书签） |

---

## F3. 任务进度联动

### F3.1 进度数据来源

FTB Quests 1.20.x 提供以下 API 获取玩家任务进度：

```java
// FTB Quests Mod API 伪代码
PlayerData playerData = FTBQuestsAPI.getPlayerData(player);
TaskProgress taskProgress = playerData.getTaskProgress(questId);

// 获取某具体目标（ItemObjective）的当前完成数量
int current = taskProgress.getCounter(objectiveId);  // 当前完成数
int target = objective.getCount();                    // 目标数量
boolean completed = taskProgress.isCompleted(objectiveId);
```

### F3.2 书签进度 Tooltip

当鼠标悬停在带任务标注的 JEI 书签上时，在原有 Tooltip 基础上追加一行进度：

```
钻石锭
📖 制作一把钻石剑
▶ 还需 2 个  ●●●○○                 ← 新增行
```

**进度显示规则**：

| 状态 | 显示格式 | 颜色 |
|------|---------|------|
| 未开始（current = 0） | `▶ 还需 X 个` | 红色 |
| 进行中（0 < current < target） | `▶ 还需 X 个` | 黄色 |
| 已完成（current ≥ target） | `✓ 已完成` | 绿色 |

**数据刷新时机**：
- 玩家打开 JEI 书签面板时读取一次
- 玩家从任意界面返回 JEI 时刷新
- **不主动轮询**（避免性能开销）

### F3.3 侧边栏进度显示

侧边栏中每个目标物品行实时显示进度：

```
  ◇ 钻石锭       2/3     ●●●○○
  ◇ 木棍         1/2     ●●○○○
  ◆ 铁锭         3/3     ●●●●●
                        ↑
                    进度条（5像素刻度）
```

**进度条规格**：
- 宽度：40px
- 高度：5px
- 颜色：未完成=灰色底+黄色填充；已完成=绿色

### F3.4 边界情况

| 场景 | 行为 |
|------|------|
| FTB Quests 未安装 | 进度列显示 `--`，同步按钮仍然可用 |
| FTB Quests 版本不兼容（API 签名变更） | 降级为"无法读取进度"，按钮功能正常 |
| 任务目标物品有特殊 NBT（如耐久度） | 进度按物品数量计，不按 NBT 计 |
| 目标物品在游戏中被禁用（mod 移除） | 显示 `?` 而非数字，避免报错 |
| 目标物品为不可堆叠物品（工具/武器） | 进度仍按数量计（即使数量为 1） |

---

## F4. 工作台侧边栏

### F4.1 显示条件

侧边栏在以下条件**全部满足**时显示：

1. 玩家正打开工作台 GUI（`CraftingScreen` / `StonecutterScreen` 等）
2. 玩家已设置"聚焦任务"（非空）
3. 侧边栏未被玩家手动收起

### F4.2 布局规格

```
┌──────────────────────────────────────┬──────────────────────────┐
│                                      │ ▼ 制作一把钻石剑    [⚙]  │
│                                      ├──────────────────────────┤
│          原版工作台                    │                          │
│           3×3 网格                    │  ◇ 钻石锭      2/3  ●   │
│                                      │  ◇ 木棍        1/2  ●   │
│                                      │  ◆ 铁锭        3/3  ✓   │
│                                      │  ◇ 红石        0/10 ●   │
│                                      │                          │
│                                      ├──────────────────────────┤
│                                      │  [⚡ 同步 JEI 书签]       │
└──────────────────────────────────────┴──────────────────────────┘
                                                    ↑
                                           右上角设置按钮（齿轮）
```

**尺寸规格**：

| 元素 | 规格 |
|------|------|
| 侧边栏总宽度 | 120px（不含原版工作台） |
| 侧边栏高度 | 与工作台 GUI 等高（~166px） |
| 任务选择器高度 | 20px |
| 目标物品行高度 | 24px |
| 同步按钮高度 | 24px |
| 任务选择器宽度 | 116px（侧边栏宽 - 4px padding） |

### F4.3 任务选择器（下拉框）

点击任务选择器，展开任务列表：

```
┌─────────────────────────────┐
│ ▼ 制作一把钻石剑           🔽│  ← 当前选中任务（收起状态）
└─────────────────────────────┘
          ↓ 点击展开
┌─────────────────────────────┐
│ 制作一把钻石剑           ✓ │  ← 当前选中（有勾选标记）
│ 建造一个高炉             ○ │
│ 制作剪刀                 ○ │
│ 烘焙曲奇                 ○ │
├─────────────────────────────┤
│ 🔍 搜索任务...              │  ← 搜索框
└─────────────────────────────┘
```

**任务列表规则**：
- 只显示玩家**已接取且未关闭**的任务
- 已完成任务显示为灰色（不可选择）
- 当前聚焦任务在最上方
- 最多显示 50 个任务（超出后搜索过滤）

**搜索行为**：
- 实时过滤，输入即显示
- 模糊匹配（包含即匹配，不要求连续）
- 空搜索显示全部列表

### F4.4 目标物品行

```
  [图标]  钻石锭    2/3   ●●●○○
     ↑       ↑       ↑        ↑
   物品    名称   进度    进度条
   图标
```

**物品图标**：
- 从 `ItemStack` 渲染真实物品图标
- 图标尺寸：16×16px

**名称显示**：
- 显示 `DisplayName`（本地化后的中文/英文名称）
- 超长名称截断，显示 `...`

**进度显示**：
- `current/target` 格式，如 `2/3`
- 已完成（current ≥ target）：文字变绿，显示 `✓`
- 未完成：正常颜色

**进度条**：
- 5 个刻度点（根据 target 等比映射）
- 每个刻度：4px × 4px 圆点
- 未完成：灰色；已完成（current≥target）：绿色

### F4.5 同步按钮

```
┌─────────────────────────────┐
│  ⚡ 同步 JEI 书签            │  ← 可用状态
└─────────────────────────────┘
┌─────────────────────────────┐
│  ⚡ 已全部同步 (3/3)         │  ← 所有物品已在书签中
└─────────────────────────────┘
┌─────────────────────────────┐
│  ⚡ JEI 未安装              │  ← JEI 不可用，禁用
└─────────────────────────────┘
```

**按钮状态**：

| 状态 | 文字 | 样式 |
|------|------|------|
| 有待同步物品 | `⚡ 同步 JEI 书签` | 正常（可点击） |
| 全部已同步 | `⚡ 已全部同步 (N/N)` | 灰色（禁用） |
| JEI 未安装 | `⚡ JEI 未安装` | 灰色（禁用），tooltip 说明 |
| 正在同步 | `⚡ 同步中...` | 旋转图标，禁用 |

**点击行为**：执行 F1 的批量同步逻辑。

### F4.6 收起 / 展开行为

- **收起按钮**：侧边栏右上角的 `[−]` 按钮，点击后侧边栏滑出
- **展开入口**：工作台右上角出现 `[+]` 按钮，点击后侧边栏滑入
- **动画**：200ms ease-out 滑动
- **收起状态持久化**：记录到玩家偏好设置，下次打开工作台保持收起

### F4.7 设置菜单

点击齿轮图标 `[⚙]`，弹出设置浮窗：

```
┌──────────────────────────────────────┐
│  ⚙ FTB Quest On Hand 设置            │
│                                      │
│  ☑ 打开工作台时自动展开侧边栏         │
│  ☑ 显示进度条                        │
│  ☑ 同步时自动关闭侧边栏               │
│  ☐ 侧边栏显示在左侧                  │
│                                      │
│  [重置偏好设置]                       │
└──────────────────────────────────────┘
```

| 设置项 | 默认值 | 说明 |
|--------|--------|------|
| 自动展开侧边栏 | 开 | 打开工作台时自动展开（无聚焦任务时自动收起） |
| 显示进度条 | 开 | 关闭后可节省侧边栏宽度（隐藏进度条列） |
| 同步后自动关闭 | 关 | 同步 JEI 书签后自动收起侧边栏 |
| 侧边栏位置 | 右侧 | 左侧或右侧（影响布局镜像） |

### F4.8 边界情况

| 场景 | 行为 |
|------|------|
| 聚焦任务已被删除/完成 | 显示最后状态，标注"任务已完成"，允许切换其他任务 |
| 无已接取任务 | 侧边栏不显示（即使自动展开也跳过），工作台正常 |
| 任务目标物品已被禁用 | 该行显示为 `? × ?`，不显示数量 |
| 侧边栏展开但玩家打开的是箱子 UI | 侧边栏自动收起（只在工作台生效） |
| 玩家在游戏中途退出 Minecraft | 聚焦任务 ID 持久化到 PlayerData，重登后恢复 |

---

## F5. 核心 API 完整规格

### F5.1 ItemStack

```java
package com.ftbquestonhand.model;

public final class ItemStack {
    private final String registryName;  // 非空，如 "minecraft:diamond"
    private final int count;            // >= 1
    private final String nbt;           // 可为 null，JSON 字符串

    // Getters
    public String getRegistryName();
    public int getCount();
    public String getNbt();

    // 工厂方法
    public static ItemStack of(String registryName, int count);
    public static ItemStack of(String registryName, int count, String nbt);

    // 比较：两个 ItemStack 相等当且仅当 registryName + count + nbt 均相等
    @Override public boolean equals(Object o);
    @Override public int hashCode();
}
```

### F5.2 QuestObjectiveProgress

```java
package com.ftbquestonhand.model;

public final class QuestObjectiveProgress {
    private final String objectiveId;   // FTB Quests 内部 ID
    private final ItemStack item;        // 目标物品
    private final int current;           // 当前完成数量
    private final int target;            // 目标数量

    public boolean isCompleted() { return current >= target; }
    public int getRemaining()   { return Math.max(0, target - current); }

    // getters...
}
```

### F5.3 QuestContextManager

```java
package com.ftbquestonhand.core;

public class QuestContextManager {

    // === 聚焦任务 ===
    public static void setFocusedQuest(String playerId, String questId);
    public static String getFocusedQuest(String playerId);
    public static void clearFocusedQuest(String playerId);

    // === 聚焦任务的目标进度 ===
    // 返回的列表按任务内目标顺序排列
    public static List<QuestObjectiveProgress> getFocusedObjectives(String playerId);

    // === JEI 书签操作 ===
    // 参数 item 可带 NBT（如原版钻石锭不含 NBT，mod 物品可能有组件标签）
    public static boolean addToJeiBookmarks(String playerId, ItemStack item);
    public static boolean removeFromJeiBookmarks(String playerId, ItemStack item);
    public static boolean isInJeiBookmarks(String playerId, ItemStack item);

    // === 书签查询 ===
    public static List<ItemStack> getJeiBookmarks(String playerId);
    public static List<ItemStack> getQuestBookmarks(String playerId);   // 仅任务来源
    public static List<ItemStack> getManualBookmarks(String playerId);    // 仅手动添加

    // === 外部集成注册 ===
    public static void registerExternalIntegration(ExternalIntegration integration);
    public static List<ExternalIntegration> getExternalIntegrations();
}
```

### F5.4 QuestSync

```java
package com.ftbquestonhand.api;

public interface QuestSync {

    // === 任务数据 ===
    List<ItemStack> getQuestObjectives(String questId);
    String getQuestName(String questId);

    // === 进度数据 ===
    // key = objectiveId, value = current count
    Map<String, Integer> getObjectiveProgress(String questId, String playerId);

    // === 玩家任务列表 ===
    // 返回该玩家已接取（但未必进行中）的所有任务 ID
    List<String> getAcceptedQuests(String playerId);

    // === 批量操作 ===
    default void batchAddObjectivesToJei(String questId, String playerId) {
        List<ItemStack> items = getQuestObjectives(questId);
        for (ItemStack item : items) {
            if (!QuestContextManager.isInJeiBookmarks(playerId, item)) {
                QuestContextManager.addToJeiBookmarks(playerId, item);
            }
        }
    }

    // === 任务存在性检查 ===
    boolean isQuestExists(String questId);
    boolean isQuestAccepted(String questId, String playerId);
    boolean isQuestCompleted(String questId, String playerId);
}
```

### F5.5 ExternalIntegration

```java
package com.ftbquestonhand.api;

public interface ExternalIntegration {

    String getModId();                          // 唯一标识
    String getDisplayName();                     // 显示名

    // 从外部存储拉取所有物品（主动查询）
    List<ItemStack> fetchStoredItems();

    // 查询物品是否存在于外部存储
    boolean containsItem(ItemStack item);

    // 可选：获取物品在外部存储中的数量
    default int getStoredCount(ItemStack item) {
        return containsItem(item) ? item.getCount() : 0;
    }

    // 可选：外部存储的图标（用于侧边栏标注）
    default String getStorageIcon() { return null; }
}
```

---

## F6. 数据持久化规格

### F6.1 NBT 结构

```nbt
FTBQOH_DATA: compound {
    focusedQuest: string,           // 当前聚焦任务 ID，无则为 ""

    jeiBookmarks: list [
        compound {
            item: compound {
                id: string,         // registryName
                Count: short,
                tag: string         // NBT JSON，可为 ""
            },
            source: string,         // "ftb_quest" | "manual" | "<modId>"
            questId: string,        // 仅 source=="ftb_quest" 时有值
            questName: string,      // 任务显示名缓存
            addedAt: long           // 时间戳
        }
    ],

    preferences: compound {
        autoExpand: byte,           // 0/1
        showProgressBar: byte,      // 0/1
        autoCloseAfterSync: byte,   // 0/1
        sidebarLeft: byte           // 0/1
    }
}
```

### F6.2 持久化时机

| 触发条件 | 时机 |
|---------|------|
| 玩家退出世界 | PlayerQuitEvent → 写入 NBT |
| 定时保存 | 每 60 秒写入一次（防止崩溃丢失） |
| 聚焦任务变更 | 立即写入（低频操作） |
| 书签变更 | 立即写入（变更后 500ms 防抖） |

### F6.3 数据迁移

mod 版本升级时，通过 `DataFixers` / `Capabilities` 版本号机制处理 NBT 结构迁移。

---

## F7. 事件系统

### F7.1 核心事件（core 层）

```java
// 事件广播（core 层只定义接口，由兼容层触发）
public interface QuestEvents {
    interface QuestAccepted    { void onQuestAccepted(String playerId, String questId); }
    interface QuestCompleted   { void onQuestCompleted(String playerId, String questId); }
    interface ObjectiveUpdated { void onObjectiveUpdated(String playerId, String questId, String objectiveId, int current, int target); }
    interface BookmarkAdded    { void onBookmarkAdded(String playerId, ItemStack item); }
    interface BookmarkRemoved  { void onBookmarkRemoved(String playerId, ItemStack item); }
}
```

### F7.2 兼容层职责

| 事件 | 触发时机 | 兼容层行为 |
|------|---------|-----------|
| `QuestAccepted` | 玩家接取新任务 | 刷新任务选择器列表 |
| `ObjectiveUpdated` | 玩家获得/使用物品，进度变化 | 刷新侧边栏进度显示 |
| `BookmarkAdded` | 书签新增 | 更新 JEI 书签列表显示 |
| `BookmarkRemoved` | 书签删除 | 更新 JEI 书签列表显示 |

---

## F8. 技术风险与依赖

| 风险 | 影响 | 缓解方案 |
|------|------|---------|
| FTB Quests API 版本差异 | 不同版本签名/行为不同 | 提供 `QuestSync` 接口，三路兼容层各自实现版本适配 |
| JEI API 在 Fabric 上不存在 | REI 书签功能不可用 | Fabric 版本仅实现侧边栏 + 任务进度，JEI 部分降级提示 |
| 工作台 UI Mixin 冲突 | 与其他 mod 的 GUI Mixin 冲突 | 提供配置项允许禁用特定功能，Mixin 顺序可配置 |
| 大量玩家同时触发进度更新 | 性能问题 | 进度读取采用懒加载，不主动轮询，事件驱动更新 |

---

## F9. 待确认问题（更新）

| # | 问题 | 优先级 |
|---|------|--------|
| 1 | REI 书签在 Fabric 版本的等效实现？ | P0 |
| 2 | Mixin 优先级设置（防止与其他 mod 冲突） | P1 |
| 3 | 是否需要支持任务书界面的移动端（触屏）操作？ | P2 |
| 4 | FTB Quests 的 `isQuestCompleted` 在任务关闭后返回什么？ | P1 |
| 5 | 书签去重粒度：`registryName + count + NBT` 还是只比较 `registryName`？ | P0 |
