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
- `rocketengine:precision_docking_port` 有 `facing` 与 `status=idle|armed|capturing|locked` 方块状态；红石信号启动自动对接，必须在两艘飞船的接口上都接通红石。
- 两端在 8 格内且端面朝向彼此时，接口会用受限的等大反向吸引冲量拉近两端，同时调用两艘船上已有、方向合适的 RCS 喷口，根据相对位置/速度与法线误差修正平移和姿态。RCS 的红石油门与自动辅助油门并存，实际使用两者中较高的一档。
- 锚点相距不超过 0.1 格且法线误差不超过 8° 后，由 Sable `FixedConstraintHandle` 创建固定关节。
- 断开任一端红石会移除固定关节/取消吸附。固定关节运行时动态创建；加载或重启后接口会重新搜寻，并在仍对齐时重建。
- 碰撞形状始终为空；视觉状态灯只改变模型，玩家在吸附或锁定时均可从空心中心穿行。接口支持六个 `facing` 方向，不会合并两艘船的子层。
