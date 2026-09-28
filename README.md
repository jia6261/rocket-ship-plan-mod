# Rocket Engine Plan — Sable 附属模组

把原 Blockbench 发动机模型做成可在 Sable 物理结构上工作的红石火箭发动机。**不需要 Create、Create: Aeronautics 或 Simulated。**

## 支持版本与依赖

- Minecraft Java Edition **1.21.1**
- **NeoForge 21.1.247+**
- **Sable 2.x**

对本模组来说，Sable 是唯一需要额外安装的模组；Minecraft 和 NeoForge 为基础运行环境。Sable 当前的 NeoForge 元数据把 Create/Flywheel 标记为可选依赖，不要求安装航空学。

## 游戏效果

- 将发动机放进 Sable 组装的子层结构中，给发动机红石信号即可产生推力；可使用 Sable 自带的组装工具/命令，不需要航空学。
- 红石信号强度 **1–15 对应 1/15–15/15 油门**；无信号时没有推力。
- 油门在 Sable physics tick 上持续计算，推力沿发动机喷口朝向施加；未组装的静态方块不会自行推进。
- 方块朝向为水平四向。断电时显示停机状态；红石激活时切换为黄芯橙焰。
- 最大推力当前设为 **8 Sable 推力单位**，可在 `RedstoneEngineBlockEntity.MAX_THRUST` 调整。
- 合成配方只使用原版铁锭、铜锭、红石和火焰弹。

## 构建

安装 JDK 21 后运行：

```bash
./gradlew build
```

成品位于 `build/libs/rocketengine-0.3.0.jar`。整合包需要安装 Minecraft、NeoForge 和 Sable；不需要安装 Create 或 Create: Aeronautics。

## 模型

模型源来自仓库原始 `model (2).json`，已转换成 Minecraft Java 方块模型并保留原 `main` / `fire` 的状态效果：

- `src/main/resources/assets/rocketengine/models/block/engine_off.json`
- `src/main/resources/assets/rocketengine/models/block/engine_on.json`
- `src/main/resources/assets/rocketengine/textures/block/engine_flame.png`

运行时模型由 `assets/rocketengine/blockstates/redstone_engine.json` 按 `powered` 状态切换；Sable physics actor 负责在组装结构上实际施力。

## 上游参考

- [Sable Modrinth 页面](https://modrinth.com/project/T9PomCSv)：支持 Minecraft 1.21.1、NeoForge，并说明 Sable 子层结构与 `/sable` 命令。
- [Sable NeoForge 依赖声明](https://github.com/ryanhcode/sable/blob/main/neoforge/src/main/resources/META-INF/neoforge.mods.toml)：Sable 的必需依赖为 Minecraft 与 NeoForge；Create/Flywheel 在其当前元数据中是可选依赖。
- [Sable 推力 actor 接口](https://github.com/ryanhcode/sable/blob/main/common/src/main/java/dev/ryanhcode/sable/api/block/propeller/BlockEntitySubLevelPropellerActor.java)：本模组通过 Sable physics tick 提交推力。

## 自动构建与发布

`.github/workflows/release.yml` 会在每次推送到 `main` 时用 Java 21 构建模组，并将 JAR 发布到 GitHub Releases。每个构建使用独立的 `auto-运行编号-提交短 SHA` 标签；同一次 workflow 重跑会更新该 release 的 JAR。也可以从 GitHub Actions 页面手动运行。
