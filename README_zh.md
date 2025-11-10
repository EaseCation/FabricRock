# Bedrock Loader

**[English](README.md) | 简体中文**

_**游戏版本提示:** 仅支持 Minecraft Java 版 1.20.6

_**注意:** 此项目目前处于早期开发阶段，尚未准备好供正式使用。_


Bedrock Loader 是一个面向 Minecraft Java 版的开创性模组，旨在弥合 Java 版和基岩版之间的差距，允许直接将基岩版插件包加载到 Java 版中。作为一个 Fabric 模组，Bedrock Loader 致力于通过将基岩版丰富多样的插件包（包括自定义方块、模型和实体）整合到 Java 版的生态系统中，来增强 Minecraft 的游戏体验。

## 安装与设置

在开始之前，请确保您已在 Minecraft Java 版中安装了 Fabric Loader 和 Fabric API。按照以下步骤设置 Bedrock Loader：

1. 从 Releases 部分下载最新版本的 Bedrock Loader。
2. 将下载的 `.jar` 文件放入您的 Minecraft 目录中的 `mods` 文件夹。
3. 下载并安装依赖模组：[Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api) 和 [Fabric Language Kotlin](https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin)。
4. 使用 Fabric 配置文件启动 Minecraft Java 版。

## 使用 Bedrock Loader

要使用 Bedrock Loader 加载基岩版插件包，请按照以下步骤操作：

1. 将您的基岩版插件包（`.zip` 或 `.mcpack` 文件）放入 Minecraft 游戏目录中的 `config/bedrock-loader` 目录。如果 `config/bedrock-loader` 目录不存在，请手动创建它。
2. 重启 Minecraft 游戏。Bedrock Loader 将自动检测并加载插件包到您的 Minecraft Java 版中。

## 远程资源包同步

Bedrock Loader 支持从远程服务器自动同步资源包。此功能允许服务器管理员集中管理和分发插件包给客户端。

### 目录结构

模组使用两个独立的目录来管理插件包：

- **`config/bedrock-loader/`** - 用于手动放置的插件包（永远不会被远程同步修改）
- **`config/bedrock-loader/remote/`** - 用于远程下载的插件包（自动管理）

这种分离确保您的自定义插件包永远不会受到远程同步的影响。

### 客户端配置

在 `config/bedrock-loader/` 目录中创建一个 `client.yml` 文件，使用以下配置：

```yaml
# 启用或禁用远程同步
enabled: true

# 服务器 URL（包括协议和端口）
serverUrl: "http://localhost:8080"

# HTTP 请求超时时间（秒）
timeoutSeconds: 10

# 在同步期间显示 UI 进度窗口
showUI: true

# 发生任何错误时自动取消同步
autoCancelOnError: false

# 自动删除从服务器上已删除的资源包
# 仅影响 remote/ 目录中的资源包
autoCleanupRemovedPacks: true
```

### 配置选项说明

- **`enabled`**: 启用/禁用远程同步。设置为 `false` 时仅使用本地资源包。
- **`serverUrl`**: 托管资源包的 HTTP 服务器 URL。
- **`timeoutSeconds`**: 网络请求超时时长。
- **`showUI`**: 在下载期间显示图形进度窗口（仅客户端）。
- **`autoCancelOnError`**: 是否在发生任何错误时取消同步。
- **`autoCleanupRemovedPacks`**: 自动删除服务器上不再存在的远程下载资源包。

### 工作原理

1. 游戏启动时，客户端连接到配置的服务器
2. 服务器提供可用资源包的清单及 MD5 校验和
3. 客户端将本地资源包（在 `remote/` 中）与服务器清单进行比较
4. 缺失或过时的资源包自动下载到 `remote/` 目录
5. 如果启用，从服务器删除的资源包将从 `remote/` 目录中移除
6. 所有资源包（手动和远程）都会加载到游戏中

## 从源代码运行和打包

要运行和打包 Bedrock Loader 以供分发，请使用以下 Gradle 任务：

- 在开发环境中运行：
    ```bash
    ./gradlew runClient
    ```

- 打包发布版本：
    ```bash
    ./gradlew build
    ```
打包后的模组将位于 `build/libs` 目录中，可供分发。

## 早期开发阶段

Bedrock Loader 目前处于早期开发阶段。

大量功能尚未实现，我们当前的重点是实现架构需求，如自定义方块和模型，以及实体模型。

存在许多 bug，模组在当前状态下几乎无法使用。

我们正在积极解决这些问题并添加新功能。

## TODO 列表

- [x] 基本模组结构
- [x] 插件包加载与资源包初始化
- [x] 基岩版模型烘焙
- [ ] 自定义方块
  - [x] 单立方体纹理
  - [x] 碰撞箱
  - [ ] 方块状态
  - [x] 方块模型
  - [x] 方块实体
  - [ ] 方块战利品表
  - [ ] 方块标签
  - [ ] 方块声音
- [ ] 物品
- [x] 实体
