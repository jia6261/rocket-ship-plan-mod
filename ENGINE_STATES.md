# 发动机红石与材质状态

`rocketengine:redstone_engine` 使用 `power=0..15` 保存邻近红石信号等级；方块模型和 Sable 推进器共享同一状态。

- `power=0`：使用 `engine_off.json`，发动机停止、无火焰、推力为零。
- `power=1..14`：使用 `engine_on_p01.json` 至 `engine_on_p14.json`；火焰尺寸随红石等级增大，推力为最大值的 `power / 15`。
- `power=15`：使用完整火焰模型 `engine_on.json`，推力达到最大值。
- 外围火焰使用原材质 `texture5.png`；核心火焰使用从 [Blockbench 分享模型](https://blckbn.ch/dv54iG)导入的新贴图 `texture6.png`。游戏资源的 `texture1.png`–`texture4.png` 也同步自该分享项目。
- 全模型 UV 下移一格的改动已撤销，所有游戏状态均恢复下移前坐标。原始 `model (2).json` 与根目录 `texture1.png`–`texture5.png` 保持未覆盖。
- 开机模型使用 translucent 渲染以保留新材质的半透明像素；停机模型使用 cutout 渲染。
- 只有组装进 Sable 子层结构后才会产生飞行器推力；无需 Create、Create: Aeronautics 或 Simulated。
