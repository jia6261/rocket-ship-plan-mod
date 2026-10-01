# 发动机红石与火焰状态

`rocketengine:redstone_engine` 使用 `power=0..15` 保存邻近红石信号等级；该状态同时驱动方块模型和 Sable 推进器，确保视觉与推力一致。

- `power=0`：使用 `engine_off.json`，发动机停止、无火焰、推力为零。
- `power=1..14`：使用 `engine_on_p01.json` 至 `engine_on_p14.json`。火焰沿喷口方向逐级变长、变粗；推力为最大值的 `power / 15`。
- `power=15`：使用完整火焰模型 `engine_on.json`，推力达到最大值。
- 所有开机模型的 `fire` 几何都引用原始 Blockbench 材质 `texture5.png`；不会使用另绘火焰贴图。只有几何尺寸随信号改变。
- 每个等级在四种水平朝向上都映射到对应旋转的模型。
- 只有组装进 Sable 子层结构后才会产生飞行器推力；未组装时红石仍会切换火焰外观。无需 Create、Create: Aeronautics 或 Simulated。

中间火焰模型由完整模型的火焰几何以喷口为中心同步缩放长度和截面生成；根目录中的 Blockbench 状态模型、原始 `texture1.png`–`texture5.png` 与 `engine_state_assets.zip` 都保留并使用原始材质。
