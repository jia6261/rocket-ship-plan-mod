# 发动机、RCS 与接口状态

## 主发动机

`rocketengine:redstone_engine` 使用六向 `facing` 表示喷口/废气方向；Sable 实际推力指向 `facing.getOpposite()`。

- `power=0..15`：邻近红石信号强度，驱动推力与 16 档火焰模型。
- `running=false`：断电或岩浆燃料耗尽，使用无火焰模型，Sable 推力为零。
- `running=true`：红石信号大于零且燃料箱含岩浆；当前模型档位仍由红石 `power` 决定。
- 内置燃料箱容量 **4000 mB**；满档耗油 **20 mB/s**。发动机后侧（喷口相反面）可用岩浆桶加注/空桶取出。
- 后侧向 NeoForge `Capabilities.FluidHandler.BLOCK` 暴露仅接受岩浆的流体处理器，因此 Create 流体管道可以兼容连接；Create 非本模组必需依赖。
- `power=1..15` 的有燃料状态使用对应火焰模型；`power=0` 或 `running=false` 使用 `engine_off`。每个状态另有竖直模型。
- 比冲检测器能读取相邻发动机；主发动机标称比冲为 **320 s**。当前燃料流量未纳入比冲计算。

## RCS

- `rocketengine:rcs_thruster` 使用六向 `facing` 与 `power=0..15`，满档最大推力 **16 Sable 单位**，小于主发动机的 128。
- 正推力值由 Sable API 转成反向于喷口的反作用力；多个方向独立红石控制，可用于平移和力矩控制。
- 当前 RCS 不消耗岩浆；标称比冲 **80 s**。RCS 的红石/朝向组合共 96 种模型状态，模型由主发动机模型缩放生成。

## 检测器与对接接口

- `rocketengine:specific_impulse_detector` 检测紧邻一格内的发动机/RCS；空手右键在聊天栏输出标称比冲、有效油门与当前推力。比较器输出按 0–320 s 归一化到 0–15，多个相邻推进器取最大比冲。
- `rocketengine:precision_docking_port` 有朝向模型但碰撞形状为空，玩家可从开口穿过。它是可穿行的对接框，不执行飞船自动吸附或合并。
