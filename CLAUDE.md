# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

**Bedrock Loader** 是一个Minecraft Java版的Fabric模组,用于加载Minecraft基岩版(Bedrock Edition)的插件包,包括自定义方块、实体、模型和纹理等内容。

- **Minecraft版本**: 1.20.6
- **构建系统**: Gradle + Fabric Loom 1.9-SNAPSHOT
- **编程语言**: Kotlin (主要) + Java (少量Mixin)
- **Java版本**: 21
- **依赖**: Fabric API, Fabric Language Kotlin

## 常用命令

### 开发与测试
```bash
# 在开发环境中运行客户端
./gradlew runClient

# 在开发环境中运行服务端
./gradlew runServer

# 在开发环境中运行数据生成器
./gradlew runDatagen

# 构建模组(输出到 build/libs/)
./gradlew build

# 清理构建产物
./gradlew clean
```

### 代码质量
```bash
# 编译Kotlin代码
./gradlew compileKotlin

# 编译Java代码(主要是Mixin)
./gradlew compileJava
```

## 核心架构

### 模块结构

项目使用Fabric Loom的分离源集特性(`splitEnvironmentSourceSets`):

```
src/
├── main/          # 通用代码(服务端和客户端共享)
│   ├── java/      # Java代码(主要是Mixin)
│   │   └── net/easecation/bedrockloader/mixin/
│   ├── kotlin/    # Kotlin主代码
│   │   └── net/easecation/bedrockloader/
│   │       ├── bedrock/           # 基岩版数据定义
│   │       ├── loader/            # 插件包加载系统
│   │       ├── block/             # 方块系统
│   │       ├── entity/            # 实体系统
│   │       ├── resourcepack/      # 资源包管理
│   │       └── util/              # 工具类
│   └── resources/
│       └── fabric.mod.json
└── client/        # 客户端专用代码
    ├── kotlin/
    │   └── net/easecation/bedrockloader/
    │       ├── loader/            # 客户端加载器
    │       └── render/            # 渲染系统
    └── resources/
```

### 数据流核心

**初始化流程**:
1. `BedrockLoader.onInitialize()` - 服务端初始化
2. `BedrockAddonsLoader.load()` - 从 `config/bedrock-loader/` 加载所有基岩包
3. `BedrockBehaviorPackLoader.load()` - 注册方块、实体到Minecraft注册表
4. `BedrockLoaderClient.onInitializeClient()` - 客户端初始化
5. `BedrockResourcePackLoader.load()` - 生成Java格式资源包并注册渲染器

**关键数据容器**:
- `BedrockPackContext` - 统一上下文,包含资源包和行为包的所有数据
- `BedrockAddonsRegistry` - 全局注册表,存储已注册的方块、实体、物品等
- `BedrockResourceContext` - 纹理、模型、渲染控制器等资源数据
- `BedrockBehaviorContext` - 方块和实体的行为定义

### 方块系统

**数据流**: 基岩方块定义(JSON) → `BlockBehaviour` → `BlockDataDriven` → Minecraft方块注册表

**关键特性**:
- 支持方块状态(states): Boolean, Int, String, Range
- 支持放置特性(traits): 方向放置、位置放置
- 支持自定义碰撞箱和选择框
- 支持基岩几何体模型或标准立方体模型
- 支持材质实例(Material Instances)和渲染层
- 支持方块实体(Block Entity)

**重要类**:
- `BlockContext` - 方块创建工厂
- `BlockDataDriven` - 数据驱动的方块实现
- `BlockEntityDataDriven` - 数据驱动的方块实体实现

### 实体系统

**数据流**: 基岩实体定义(JSON) → `EntityBehaviour` → `EntityDataDriven` → Minecraft实体注册表

**组件系统**:
实体通过组件系统定义行为,支持的组件包括:
- `ComponentHealth` - 生命值
- `ComponentMovement` - 移动速度
- `ComponentPhysics` - 物理特性(重力、碰撞)
- `ComponentScale` - 大小缩放
- `ComponentRideable` - 可骑乘
- `ComponentPushable` - 可推动性
- 等等...

**重要类**:
- `EntityDataDriven` - 数据驱动的实体实现
- `EntityComponents` - 实体组件容器
- `EntityDataDrivenRenderer` - 实体渲染器

### 资源包转换系统

**基岩资源 → Java资源转换**:

1. **纹理转换**:
   - 从基岩包中提取PNG/JPG/TGA纹理
   - 转换为标准PNG格式
   - 保存到临时Java资源包: `run/bedrock-loader-resource/`

2. **模型转换**:
   - 基岩几何体(Geometry) → ModelPartData树 → ModelPart
   - 通过`BedrockGeometryModel`实现模型烘焙和渲染
   - 支持方块和实体几何体

3. **材质映射**:
   - `terrain_texture.json` 定义方块纹理别名
   - `item_texture.json` 定义物品纹理别名
   - Material Instances 定义材质到纹理的映射

**重要类**:
- `BedrockResourcePackLoader` - 资源包加载器
- `BedrockGeometryModel` - 几何体模型实现
- `BedrockRenderUtil` - 模型转换工具
- `BedrockLoaderResourcePackProvider` - 资源包提供者

### 客户端渲染

**模型加载插件**: 通过`ModelLoadingPlugin`拦截模型加载,为基岩方块和物品提供自定义模型。

**渲染层**: 根据Material Instance的`render_method`选择:
- `alpha_test` → RenderLayer.getCutout()
- `blend` → RenderLayer.getTranslucent()

**实体渲染**: 注册`EntityDataDrivenRenderer`和`BedrockGeometryModel`到实体渲染器注册表。

## 开发建议

### 添加新的基岩方块支持

1. 在行为包中定义方块: `blocks/xxx.block.json`
2. 在资源包中定义纹理和模型: `blocks.json`, `terrain_texture.json`
3. 将基岩包放入 `config/bedrock-loader/`
4. 重启游戏,模组会自动加载和注册

### 添加新的基岩实体支持

1. 在行为包中定义实体: `entities/xxx.json`,配置组件
2. 在资源包中定义实体资源: `entities/xxx.entity.json`
3. 定义几何体、纹理、渲染控制器
4. 如需生物蛋,设置 `is_spawnable: true`

### 调试技巧

- 检查日志输出了解加载过程
- 临时Java资源包位置: `run/bedrock-loader-resource/`
- 基岩包放置位置: `config/bedrock-loader/`
- 使用IntelliJ IDEA的Kotlin调试功能

### 代码风格

- Kotlin代码使用data class处理JSON反序列化
- 使用Kotlin的扩展函数简化代码
- Mixin仅用于必要的注入点(如资源包提供者)
- 使用Identifier作为资源标识符

## 项目状态

项目目前处于早期开发阶段,许多功能尚未完成。当前重点:
- 自定义方块和模型
- 实体模型和组件系统
- 资源包转换系统

已知问题:
- 方块状态系统不完整
- 碰撞箱支持有限
- 许多基岩特性尚未实现
