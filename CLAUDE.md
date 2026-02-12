# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

**Bedrock Loader** 是一个Minecraft Java版的Fabric模组,用于加载Minecraft基岩版(Bedrock Edition)的插件包,包括自定义方块、实体、模型和纹理等内容。

- **Minecraft版本**: 1.21.1
- **构建系统**: Gradle + Fabric Loom 1.10
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
- ✨ **支持Permutations（条件变体）**: 根据方块状态动态切换材质、模型和变换

**Permutations系统** (v1.1新增):
- **材质动态切换**: 根据permutation条件改变minecraft:material_instances
- **Geometry动态切换**: 根据permutation条件替换minecraft:geometry（实现开关门等效果）
- **多面材质映射**: 支持north/south/east/west/up/down独立纹理
- **Transformation变体**: 支持每个状态独立的旋转/缩放/位移
- **条件表达式**: 支持`query.block_state('property') == 'value'`语法和&&运算
- **性能优化**: 预烘焙策略，运行时O(1)组件查询

**重要类**:
- `BlockContext` - 方块创建工厂
- `BlockDataDriven` - 数据驱动的方块实现
- `BlockDataDriven.getGeometryIdentifier()` - 提取当前状态的geometry标识符
- `BlockEntityDataDriven` - 数据驱动的方块实体实现
- `BedrockMaterialHelper` - 材质实例动态创建辅助类

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

**模型变体系统** (v1.1增强):
- 使用Fabric的`BlockStateResolver` API为每个BlockState注册独立模型
- `BedrockGeometryModel.getModelVariant()` - 根据components创建模型变体
- 支持材质、geometry、transformation的动态组合
- 自动缓存和复用相同配置的模型

**渲染层**: 根据Material Instance的`render_method`选择:
- `alpha_test` → RenderLayer.getCutout()
- `blend` → RenderLayer.getTranslucent()

**实体渲染**: 注册`EntityDataDrivenRenderer`和`BedrockGeometryModel`到实体渲染器注册表。

**关键类**:
- `BedrockModelLoadingPlugin` - Fabric模型加载插件
- `BedrockGeometryModel.Factory` - 几何体模型工厂
- `BedrockMaterialHelper` - 动态材质创建辅助类

## 开发建议

### 添加新的基岩方块支持

1. 在行为包中定义方块: `blocks/xxx.block.json`
2. 在资源包中定义纹理和模型: `blocks.json`, `terrain_texture.json`
3. 将基岩包放入 `config/bedrock-loader/`
4. 重启游戏,模组会自动加载和注册

### 使用Permutations实现动态方块

#### 示例1: 材质切换（颜色变体）

```json
{
  "minecraft:block": {
    "description": {
      "identifier": "mymod:colored_block",
      "properties": {
        "mymod:color": ["red", "blue", "green"]
      }
    },
    "components": {
      "minecraft:geometry": "geometry.cube",
      "minecraft:material_instances": {
        "*": { "texture": "default_texture", "render_method": "opaque" }
      }
    },
    "permutations": [
      {
        "condition": "query.block_state('mymod:color') == 'red'",
        "components": {
          "minecraft:material_instances": {
            "*": { "texture": "red_texture" }
          }
        }
      },
      {
        "condition": "query.block_state('mymod:color') == 'blue'",
        "components": {
          "minecraft:material_instances": {
            "*": { "texture": "blue_texture" }
          }
        }
      }
    ]
  }
}
```

#### 示例2: 模型切换（开关门）

```json
{
  "minecraft:block": {
    "description": {
      "identifier": "mymod:custom_door",
      "properties": {
        "mymod:open": [true, false]
      }
    },
    "components": {
      "minecraft:geometry": "geometry.door_closed",
      "minecraft:material_instances": {
        "*": { "texture": "door", "render_method": "alpha_test" }
      }
    },
    "permutations": [
      {
        "condition": "query.block_state('mymod:open') == 'true'",
        "components": {
          "minecraft:geometry": "geometry.door_open"
        }
      }
    ]
  }
}
```

#### 示例3: 多面材质

```json
{
  "minecraft:block": {
    "description": {
      "identifier": "mymod:directional_block"
    },
    "components": {
      "minecraft:geometry": "geometry.cube",
      "minecraft:material_instances": {
        "north": { "texture": "front_texture" },
        "south": { "texture": "back_texture" },
        "east": { "texture": "side_texture" },
        "west": { "texture": "side_texture" },
        "up": { "texture": "top_texture" },
        "down": { "texture": "bottom_texture" }
      }
    }
  }
}
```

#### 示例4: 旋转变换

```json
{
  "minecraft:block": {
    "description": {
      "identifier": "mymod:rotatable_block",
      "properties": {
        "minecraft:cardinal_direction": ["north", "south", "east", "west"]
      }
    },
    "components": {
      "minecraft:geometry": "geometry.custom_model",
      "minecraft:material_instances": {
        "*": { "texture": "model_texture" }
      }
    },
    "permutations": [
      {
        "condition": "query.block_state('minecraft:cardinal_direction') == 'north'",
        "components": {
          "minecraft:transformation": { "rotation": [0, 180, 0] }
        }
      },
      {
        "condition": "query.block_state('minecraft:cardinal_direction') == 'south'",
        "components": {
          "minecraft:transformation": { "rotation": [0, 0, 0] }
        }
      },
      {
        "condition": "query.block_state('minecraft:cardinal_direction') == 'east'",
        "components": {
          "minecraft:transformation": { "rotation": [0, 270, 0] }
        }
      },
      {
        "condition": "query.block_state('minecraft:cardinal_direction') == 'west'",
        "components": {
          "minecraft:transformation": { "rotation": [0, 90, 0] }
        }
      }
    ]
  }
}
```

#### 材质实例键说明

支持的材质实例键包括：

- `*`: 通配符，应用于所有面
- `side`: 简写键，应用于四个水平侧面（north、south、east、west）
- `up`, `down`, `north`, `south`, `east`, `west`: 单独的面

**示例：使用 side 简写键**

```json
{
  "minecraft:material_instances": {
    "*": { "texture": "default_texture" },
    "side": { "texture": "side_texture" },
    "up": { "texture": "top_texture" },
    "down": { "texture": "bottom_texture" }
  }
}
```

在这个例子中：
- `side` 会自动展开应用到 north、south、east、west 四个侧面
- `up` 和 `down` 分别应用到顶面和底面
- 如果同时定义了 `side` 和具体方向键（如 `north`），具体方向键会覆盖 `side` 的设置（取决于JSON中的定义顺序）

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
- ✅ 自定义方块和模型
- ✅ 实体模型和组件系统
- ✅ 资源包转换系统
- ✅ **Permutations静态变体系统 (v1.1)**

已完成功能:
- ✅ 方块状态系统和permutations支持
- ✅ 材质动态切换
- ✅ Geometry动态切换
- ✅ 多面材质映射（north/south/east/west/up/down）
- ✅ Transformation变换系统
- ✅ 基岩几何体渲染
- ✅ 实体组件系统
- ✅ Tag系统（从基岩版JSON生成Java版Tag文件）

Molang支持：
- ✅ `query.block_state('property') == 'value'` - 方块自身状态查询（静态，初始化时求值）
- ✅ `query.block_state('property') != 'value'` - 不等于运算符
- ✅ 逻辑与运算符：`condition1 && condition2`（多条件组合）
- ✅ 逻辑或运算符：`condition1 || condition2`（任一条件满足即可）
- ✅ 运算符优先级：`&&` 高于 `||`（即 `a && b || c && d` 解析为 `(a && b) || (c && d)`）

性能特性：
- ✅ Permutation条件在初始化时求值（静态预烘焙）
- ✅ 每个BlockState一份预计算的Components
- ✅ 运行时O(1)组件查询，无额外开销

已知问题:
- 碰撞箱支持有限
- Permutations仅支持静态条件（不支持邻居查询等动态条件）
- Molang表达式仅支持block_state查询（不支持括号分组、`!`非运算符等高级语法）
- 部分基岩特性尚未实现
