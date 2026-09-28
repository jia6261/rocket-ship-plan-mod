# Rocket Engine Plan — Create: Aeronautics 附属模组

把原 Blockbench 发动机模型做成可实际推动 Sable 飞行器的红石火箭发动机。

## 支持版本

- Minecraft Java Edition **1.21.1**
- **NeoForge 21.1.247+**
- Create **6.0.10+**
- Create: Aeronautics **1.3.2+**
- Sable **2.x**（Create: Aeronautics 所需）

## 游戏效果

- 将发动机组装进 Create: Aeronautics / Sable 物理结构后，给它红石信号即可产生推力。
- 红石信号强度 **1–15 对应 1/15–15/15 油门**；无信号时没有推力。
- 油门在 Sable physics tick 上持续计算，推力沿发动机喷口朝向施加；静态世界中的方块不会自行推进。
- 方块朝向为水平四向，放置时喷口朝向方块正面。红石激活时模型切换到黄芯橙焰效果。
- 最大推力当前设为 **8 Sable 推力单位**，可在 `RedstoneEngineBlockEntity.MAX_THRUST` 调整。

## 构建

安装 JDK 21 后运行：

```bash
./gradlew build
```

成品位于 `build/libs/rocketengine-0.2.0.jar`。开发运行需要 NeoForge、Create、Create: Aeronautics 和 Sable；发给玩家的整合包也必须安装这些依赖。

## 模型

模型源来自仓库原始 `model (2).json`，已转换成 Minecraft Java 方块模型并保留原 `main` / `fire` 的状态效果：

- `src/main/resources/assets/rocketengine/models/block/engine_off.json`
- `src/main/resources/assets/rocketengine/models/block/engine_on.json`
- `src/main/resources/assets/rocketengine/textures/block/engine_flame.png`

运行时模型由 `assets/rocketengine/blockstates/redstone_engine.json` 按 `powered` 状态切换；Sable physics actor 则负责实际施力。

## 上游参考

- [Create: Aeronautics 官方源码](https://github.com/Creators-of-Aeronautics/Simulated-Project) 与 [Modrinth 兼容信息](https://modrinth.com/project/oWaK0Q19)：确认 1.21.1 / NeoForge 目标。
- [Sable 推力 actor 接口](https://github.com/ryanhcode/sable/blob/main/common/src/main/java/dev/ryanhcode/sable/api/block/propeller/BlockEntitySubLevelPropellerActor.java)：方块实体通过 physics tick 将 `BlockEntityPropeller` 提供的推力写入 Sable 的 propulsion force group。

## 自动构建与发布

`.github/workflows/release.yml` 会在每次推送到 `main` 时用 Java 21 构建模组，并将 JAR 发布到 GitHub Releases。每个构建使用独立的 `auto-运行编号-提交短 SHA` 标签；若同一次 workflow 重跑，会更新该 release 的 JAR。也可以从 GitHub Actions 页面手动运行。
