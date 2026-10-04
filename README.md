# Rocket Engine Plan — Sable 附属模组

把 Blockbench 发动机模型做成可在 Sable 物理结构上工作的红石火箭发动机。**不需要 Create、Create: Aeronautics 或 Simulated。**

## 支持版本与依赖

- Minecraft Java Edition **1.21.1**
- **NeoForge 21.1.247+**
- **Sable 2.x**

Sable 是本模组唯一需要额外安装的模组；Minecraft 和 NeoForge 为基础运行环境。无需安装航空学。

## 游戏效果

- 发动机可以朝 **上、下、东、西、南、北** 六个方向放置。朝向跟随放置时玩家视线最近的方向的反向；模型喷口表示废气喷射方向，火箭推力与喷口/火焰喷射方向相反。
- 红石强度 **0–15** 对应 **0–100% 推力**。发动机在 Sable 组装的子层结构中沿喷口反方向产生推力；例如要向上飞，喷口/火焰应朝下放置。未组装的静态方块不会自行推进。
- 红石等级同时控制火焰尺寸：断电时停机、无火焰；信号越强，推力和火焰越大。

本次采用附件里的新版 Blockbench 模型几何，并导入更新的 `texture3.png`；核心火焰继续使用 `texture6.png`，外围火焰使用 `texture5.png`。根目录原有模型与材质文件保持不覆盖；附件源文件快照保存在 `source_assets/blockbench_2026-10-03/`。

满档最大推力为 **128 Sable 推力单位**，红石等级按 `power / 15` 线性缩放，可在 `RedstoneEngineBlockEntity.MAX_THRUST` 调整。合成配方只使用原版铁锭、铜锭、红石和火焰弹。

## 构建

安装 JDK 21 后运行：

```bash
./gradlew build
```

成品位于 `build/libs/rocketengine-0.5.1.jar`。整合包需要安装 Minecraft、NeoForge 和 Sable。

## 模型与材质

- `engine_off.json`：红石等级 0；停机、无火焰，cutout 渲染。
- `engine_on_p01.json` 至 `engine_on_p14.json`：红石等级 1–14；火焰随推力逐级变大。
- `engine_on.json`：红石等级 15；满档火焰，translucent 渲染以保留材质透明像素。
- 每个红石模型另有 `_vertical` 版本，供朝上/朝下的方块状态使用；`redstone_engine.json` 覆盖 6 个朝向与 16 个红石等级，共 96 种状态。
- 运行时贴图位于 `src/main/resources/assets/rocketengine/textures/block/`；新版 `texture3.png` 来自本次附件，核心火焰是 `texture6.png`，外围火焰是 `texture5.png`。

## 上游参考

- [Sable Modrinth 页面](https://modrinth.com/project/T9PomCSv)
- [Sable NeoForge 依赖声明](https://github.com/ryanhcode/sable/blob/main/neoforge/src/main/resources/META-INF/neoforge.mods.toml)
- [Sable 推力 actor 接口](https://github.com/ryanhcode/sable/blob/main/common/src/main/java/dev/ryanhcode/sable/api/block/propeller/BlockEntitySubLevelPropellerActor.java)

## 自动构建与发布

`.github/workflows/release.yml` 会在每次推送到 `main` 时用 Java 21 构建模组，并将 JAR 发布到 GitHub Releases。每个构建使用独立的 `auto-运行编号-提交短 SHA` 标签。
