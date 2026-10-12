# Rocket Engine Plan — Sable 附属模组

把 Blockbench 发动机模型做成可在 Sable 物理结构上工作的红石火箭发动机，并加入 RCS、比冲检测器、可穿行对接接口和太空维度。模组需要 **Sable**；跨维度整船迁移还要求服务端安装 **Dimensional Sable 1.0.5+**。Create 是可选项，仅在使用 Create 流体管道供液时需要安装。

## 支持版本与依赖

- Minecraft Java Edition **1.21.1**
- **NeoForge 21.1.247+**
- **Sable 2.x**
- **Dimensional Sable 1.0.5+**（服务端必需；单人游戏的整合服务端也需要安装）
- Create 可选，只用于 Create 流体管道供液

## 游戏功能

### 岩浆火箭发动机

- 支持上、下、东、西、南、北六向放置。`facing`/模型喷口代表废气喷射方向，实际推力朝喷口反方向；要向上飞时，喷口应朝下。
- 发动机必须有岩浆且收到红石信号才运行。红石强度 **0–15** 控制推力与火焰大小；燃料耗尽会自动停机。
- 发动机内置 **4000 mB** 岩浆箱，满档每秒消耗 **20 mB**，可持续约 **200 秒**。
- **手动加注/取出：**手持岩浆桶/空桶，对准发动机喷口后方（与 `facing` 相反）的燃料接口右键。只有从后侧才能操作。
- **管道供液：**NeoForge `IFluidHandler` 流体能力只在后侧接口开放，Create 流体管道可以直接连接并输入岩浆；无需给本模组添加 Create 硬依赖。
- 空手右键可查看当前燃料、红石等级和是否运行。未组装进 Sable 子层结构的静态发动机不会自行推进。

### RCS 姿态控制喷口

- `RCS 姿态控制喷口`可朝六向放置，红石 0–15 线性调节推力，最大为 **16 Sable 推力单位**，低于主发动机，适合布置在飞船边缘进行平移或力矩控制。
- 每个 RCS 方块可独立接红石信号；需组装进 Sable 子层结构才会产生飞船推力。对接期间接口也可自动调用方向合适的船载 RCS 校正位置/姿态；手动红石油门仍有效。当前 RCS 不消耗岩浆。

### 比冲检测器

- 把检测器放在主发动机或 RCS **紧邻的一格**，空手右键即可读取标称比冲、有效油门和当前推力输出。
- 比冲是设计标称值：主发动机 **320 s**，RCS **80 s**。当前游戏未模拟推进剂质量流量，因此该读数**不是从燃料消耗实时推算**。
- 检测器可驱动比较器：输出按 **0–320 s** 线性归一化至红石 **0–15**；多个相邻推进器取最高比冲。

### 精确对接接口

- 两艘 Sable 飞船上的接口都接通红石后进入 **已启动** 状态；接口需安装在 Sable 子层飞船上。
- 两端面朝彼此且进入 **8 格搜寻范围** 后，接口的弱吸引冲量会拉近两船，同时调用船载 **RCS 喷口**按相对位置/速度自动校正平移和姿态；需布置方向合适的 RCS。端面相对、姿态误差小于约 **8°** 且前缘锚点距离小于 **0.1 格** 时创建 Sable **Fixed Constraint**，将两艘船物理锁定。
- 任意一端断开红石即可解除物理锁；右键可查看当前状态（未启动、已启动、吸附中、已锁定）。
- 碰撞形状始终为空，空心中心和锁定后的通道均允许玩家穿过；接口不会把船块合并成同一个子层。
- 支持上、下、东、西、南、北六向放置；对接方向以接口的 `facing` 面为准。

### 太空维度

- 只有驾驶员处于 Sable 飞船子层上时，飞船上升到主世界 **Y > 9999** 才会自动进入 `rocketengine:space`。不在飞船上的玩家不会被单独送入太空。
- 飞船会连同 Dimensional Sable 能识别的连接子层一起迁移；若迁移失败，不会把驾驶员单独传走。
- 太空维度使用空洞地形。玩家、其他实体和飞船均无重力；Sable 飞船的维度重力、空气阻力和气压也设为零。
- 在太空维度驾驶飞船向下飞，驾驶员低于 **Y=0** 时整船及连接子层会一并迁回主世界。
- **Dimensional Sable 1.0.5+ 是服务端必需依赖**。多人游戏中安装到服务端；单人游戏需将其安装在该游戏实例中。Dimensional Sable 自身的跨模组机械装置兼容范围请参照其说明。

## 配方与物品

新方块都加入“火箭发动机计划”创造模式物品栏，并有原版材料配方和掉落表。发动机、RCS 与对接框共享本模组的像素材质风格；RCS 使用缩小后的发动机模型。

## 构建

安装 JDK 21 后运行：

```bash
./gradlew build
```

成品位于 `build/libs/rocketengine-0.8.2.jar`。`.github/workflows/release.yml` 会在每次推送到 `main` 后自动构建并发布 JAR。

## 上游参考

- [Sable Modrinth 页面](https://modrinth.com/project/T9PomCSv)
- [Dimensional Sable Modrinth 页面](https://modrinth.com/project/Iq9c4mLa)
- [Sable 推力 actor 接口](https://github.com/ryanhcode/sable/blob/main/common/src/main/java/dev/ryanhcode/sable/api/block/propeller/BlockEntitySubLevelPropellerActor.java)
- [NeoForge 流体能力说明](https://docs.neoforged.net/docs/1.21.1/datastorage/capabilities/)
