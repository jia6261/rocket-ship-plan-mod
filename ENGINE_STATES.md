# 发动机状态与红石推力

此模组只要求 Sable，不需要 Create、Create: Aeronautics 或 Simulated。状态和推力由 `rocketengine:redstone_engine` 方块驱动：

- `powered=false`：采用停止模型 `src/main/resources/assets/rocketengine/models/block/engine_off.json`，无推力。
- `powered=true`：采用开启模型 `src/main/resources/assets/rocketengine/models/block/engine_on.json`，显示火焰。
- 红石信号强度 1–15 对应线性油门 1/15–15/15；Sable physics tick 根据油门向喷口方向施加推力。
- 将方块组装进 Sable 子层结构后即可推进；未组装时红石只切换外观。
- 合成配方仅使用原版物品。

原 Blockbench 导入变体 `engine_off.json` / `engine_on.json` 和 `engine_state_assets.zip` 仍保留在仓库根目录作为模型源文件；游戏中使用 `src/main/resources` 下经转换的模型。
