# FTB Quest On Hand

为工作台 UI 添加侧边栏，并将 FTB Quests 任务奖励一键同步到侧边栏 / JEI 收藏栏。

## 功能

1. **工作台侧边栏** — 在工作台界面新增一条可定制的侧边栏，用于展示收藏物品
2. **FTB Quest → 侧边栏同步** — 一键将任务奖励物品添加至侧边栏收藏
3. **FTB Quest → JEI 收藏同步** — 一键将任务奖励物品添加至 JEI 收藏栏
4. **外部 mod 接入 API** — 为 AE2、精致存储等提供标准集成接口

## 架构

采用 **核心逻辑 + 三路兼容层** 设计：

```
ftb-quest-on-hand/
├── core/         # 纯 Java 实现，无任何 mod 依赖
├── api/          # 对外 API 接口定义
├── neoforge/     # NeoForge (1.21+) 兼容层
├── forge/        # Forge (1.20.x) 兼容层
└── fabric/       # Fabric (1.20.x) 兼容层
```

### 核心模块 (core)

- `BookmarkManager` — 收藏数据管理
- `ItemStack` — 物品栈模型
- `BookmarkCategory` — 收藏分类枚举

### API 模块 (api)

- `QuestSync` — FTB Quest 同步接口
- `SidebarProvider` — 侧边栏 UI 接口
- `ExternalIntegration` — 外部 mod 集成接口

## 编译

```bash
./gradlew build
```

各平台构建产物位于对应子模块的 `build/libs/` 目录。

## License

MIT
