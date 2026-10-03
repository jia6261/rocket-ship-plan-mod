# 发动机红石与材质状态

`rocketengine:redstone_engine` 使用 `power=0..15` 保存邻近红石信号等级，并使用六向 `facing` 状态控制模型和 Sable 推力方向。

- 可朝上、下、东、西、南、北六个方向放置；`facing`、模型喷口和 Sable actor 的方向一致。
- `power=0`：使用 `engine_off.json`，发动机停止、无火焰、推力为零。
- `power=1..14`：使用 `engine_on_p01.json` 至 `engine_on_p14.json`；火焰尺寸随红石等级增大，推力为最大值的 `power / 15`。
- `power=15`：使用完整火焰模型 `engine_on.json`，最大推力为 **128 Sable 推力单位**。
- 朝上、朝下分别使用 `_vertical` 模型版本；横向模型按方块朝向旋转。方块状态覆盖 6 个朝向 × 16 个红石等级，共 96 种组合。
- 外围火焰使用 `texture5.png`，核心火焰使用 `texture6.png`；新版模型和更新的 `texture3.png` 来自本次附件。原有 root 资产未覆盖，附件源文件快照放在 `source_assets/blockbench_2026-10-03/`。
- 开机模型使用 `translucent` 渲染以保留材质透明像素；停机模型使用 `cutout` 渲染。
- 只有组装进 Sable 子层结构后才会产生飞行器推力；无需 Create、Create: Aeronautics 或 Simulated。
