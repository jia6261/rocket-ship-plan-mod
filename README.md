# Rocket Engine Plan — Sable 附属模组

把 Blockbench 发动机模型做成可在 Sable 物理结构上工作的红石火箭发动机，并加入 RCS、比冲检测器和可穿行对接接口。**模组本身只要求 Sable；Create 是可选项，仅在你要用 Create 流体管道供液时需要安装。**

## 支持版本与依赖

- Minecraft Java Edition **1.21.1**
- **NeoForge 21.1.247+**
- **Sable 2.x**

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
- 每个 RCS 方块可独立接红石信号；需组装进 Sable 子层结构才会产生飞船推力。当前 RCS 不消耗岩浆。

### 比冲检测器

- 把检测器放在主发动机或 RCS **紧邻的一格**，空手右键即可读取标称比冲、有效油门和当前推力输出。
- 比冲是设计标称值：主发动机 **320 s**，RCS **80 s**。当前游戏未模拟推进剂质量流量，因此该读数**不是从燃料消耗实时推算**。
- 检测器可驱动比较器：输出按 **0–320 s** 线性归一化至红石 **0–15**；多个相邻推进器取最高比冲。

### 精确对接接口

- 这是一个**无碰撞的空心接口框**，玩家可以从开口穿过；建造时请保持接口另一侧和上方有足够的通行空间。
- 它不自动吸附或合并两艘 Sable 飞船；当前作用是提供可穿行的对接/舱门开口。

## 配方与物品

新方块都加入“火箭发动机计划”创造模式物品栏，并有原版材料配方和掉落表。发动机、RCS 与对接框共享本模组的像素材质风格；RCS 使用缩小后的发动机模型。

## 构建

安装 JDK 21 后运行：

```bash
./gradlew build
```

成品位于 `build/libs/rocketengine-0.6.0.jar`。`.github/workflows/release.yml` 会在每次推送到 `main` 后自动构建并发布 JAR。

## 上游参考

- [Sable Modrinth 页面](https://modrinth.com/project/T9PomCSv)
- [Sable 推力 actor 接口](https://github.com/ryanhcode/sable/blob/main/common/src/main/java/dev/ryanhcode/sable/api/block/propeller/BlockEntitySubLevelPropellerActor.java)
- [NeoForge 流体能力说明](https://docs.neoforged.net/docs/1.21.1/datastorage/capabilities/)
