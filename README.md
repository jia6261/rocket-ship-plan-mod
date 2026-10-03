# Rocket Engine Plan — Sable 附属模组

把 Blockbench 发动机模型做成可在 Sable 物理结构上工作的红石火箭发动机。**不需要 Create、Create: Aeronautics 或 Simulated。**

## 支持版本与依赖

- Minecraft Java Edition **1.21.1**
- **NeoForge 21.1.247+**
- **Sable 2.x**

Sable 是本模组唯一需要额外安装的模组；Minecraft 和 NeoForge 为基础运行环境。无需安装航空学。

## 游戏效果

红石强度 **0–15** 对应 **0–100% 推力**，发动机在 Sable 组装的子层结构中沿喷口朝向产生推力；未组装的静态方块不会自行推进。红石等级同时控制火焰尺寸，断电时停机、无火焰。

已从 [Blockbench 分享模型](https://blckbn.ch/dv54iG)导入新的核心火焰贴图。外围火焰继续使用原始 `texture5.png`，新增的核心火焰材质使用 `texture6.png`（为避免分享文件中两个材质都叫 `texture5` 而另行命名）。原先“所有 UV 下移一格”的改动已撤销，模型 UV 恢复到下移前的位置；贴图像素不做平移。

满档最大推力现为 **128 Sable 推力单位**，是上一版的 **16 倍**；红石等级仍按 `power / 15` 线性缩放，可在 `RedstoneEngineBlockEntity.MAX_THRUST` 调整。合成配方只使用原版铁锭、铜锭、红石和火焰弹。

## 构建

安装 JDK 21 后运行：

```bash
./gradlew build
```

成品位于 `build/libs/rocketengine-0.4.4.jar`。整合包需要安装 Minecraft、NeoForge 和 Sable。

## 模型与材质

- `engine_off.json`：红石等级 0；使用 cutout 渲染。
- `engine_on_p01.json` 至 `engine_on_p14.json`：红石等级 1–14；火焰随推力逐级变大。
- `engine_on.json`：红石等级 15；使用半透明渲染以保留新核心火焰材质的 alpha。
- 新增核心火焰材质位于 `src/main/resources/assets/rocketengine/textures/block/texture6.png`；外围火焰使用原始 `texture5.png`。引擎游戏资源里的 `texture1.png`–`texture4.png` 也同步自分享模型。
- 所有运行时模型的 UV 均恢复为下移前的坐标；原始 `model (2).json` 和根目录的 `texture1.png`–`texture5.png` 未覆盖。

`assets/rocketengine/blockstates/redstone_engine.json` 按 `power=0..15` 和发动机朝向选择对应模型；同一 `power` 状态决定 Sable actor 的实际油门与推力。

## 上游参考

- [Sable Modrinth 页面](https://modrinth.com/project/T9PomCSv)
- [Sable NeoForge 依赖声明](https://github.com/ryanhcode/sable/blob/main/neoforge/src/main/resources/META-INF/neoforge.mods.toml)
- [Sable 推力 actor 接口](https://github.com/ryanhcode/sable/blob/main/common/src/main/java/dev/ryanhcode/sable/api/block/propeller/BlockEntitySubLevelPropellerActor.java)

## 自动构建与发布

`.github/workflows/release.yml` 会在每次推送到 `main` 时用 Java 21 构建模组，并将 JAR 发布到 GitHub Releases。每个构建使用独立的 `auto-运行编号-提交短 SHA` 标签。
