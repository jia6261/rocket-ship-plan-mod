# 发动机状态与红石推力

此仓库现已包含 NeoForge 1.21.1 的 Create: Aeronautics 附属模组实现。模型状态和实际推力由 `rocketengine:redstone_engine` 方块驱动：

- `powered=false`：采用停止模型 `src/main/resources/assets/rocketengine/models/block/engine_off.json`，无推力。
- `powered=true`：采用开启模型 `src/main/resources/assets/rocketengine/models/block/engine_on.json`，显示火焰。
- 红石信号强度 1–15 对应线性油门 1/15–15/15；Sable physics tick 根据油门向喷口方向施加推力。
- 只有组装进入 Sable 物理结构后会产生飞行器推力；未组装时红石只切换外观。

原 Blockbench 导入变体 `engine_off.json` / `engine_on.json` 和 `engine_state_assets.zip` 仍保留在仓库根目录作为模型源文件；游戏中使用上述 `src/main/resources` 下经转换的模型。
