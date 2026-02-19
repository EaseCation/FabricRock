# Standalone Pack Distribution Server

**[English](#overview) | [简体中文](#概述)**

## Overview

The Standalone Pack Distribution Server is a lightweight HTTP server that distributes Bedrock resource packs to clients without requiring a full Minecraft/Fabric server. It shares the same codebase as the embedded HTTP server in the Bedrock Loader mod, supporting all features including AES-256-CFB8 encryption with Challenge-Response key exchange.

## Quick Start

### Build

```bash
./gradlew :standalone-server:shadowJar
```

The output JAR is at `standalone-server/build/libs/bedrock-pack-server-1.0.0.jar`.

### Run

```bash
# Default settings (packs from ./config/bedrock-loader/, port from server.yml)
java -jar bedrock-pack-server-1.0.0.jar

# Specify pack directory and port
java -jar bedrock-pack-server-1.0.0.jar -d /srv/packs -p 9090
```

### Command Line Options

| Option | Short | Description | Default |
|--------|-------|-------------|---------|
| `--pack-dir <path>` | `-d` | Pack directory path | `config/bedrock-loader` |
| `--config <path>` | `-c` | Config file path | `<pack-dir>/server.yml` |
| `--port <port>` | `-p` | Server port (overrides config) | from `server.yml` |
| `--host <host>` | | Bind address (overrides config) | from `server.yml` |
| `--base-url <url>` | | Base URL for download links (overrides config) | from `server.yml` |
| `--help` | | Show help message | |

## Configuration

The server uses the same `server.yml` format as the Fabric mod. On first startup, a default config file is auto-generated:

```yaml
# Enable HTTP server
enabled: true

# Server port
port: 8080

# Bind address
host: "0.0.0.0"

# Base URL for download links (auto-detected if empty)
base-url: ""

# Enable resource pack encryption
encryption-enabled: false

# Encryption key ("auto" for auto-generation)
encryption-server-secret: "auto"
```

## Pack Directory Structure

Place your Bedrock packs in the pack directory:

```
config/bedrock-loader/          # or your custom --pack-dir
├── server.yml                  # Server configuration
├── my-resource-pack.mcpack     # Single pack file
├── my-addon.mcaddon            # Multi-pack addon file
├── my-pack-folder/             # Unpacked folder (auto-zipped)
│   └── manifest.json
├── my-addon-folder/            # Addon folder (multiple sub-packs)
│   ├── resource_pack/
│   │   └── manifest.json
│   └── behavior_pack/
│       └── manifest.json
├── .cache/                     # Auto-generated ZIP cache
├── .server_secret              # Auto-generated encryption key
└── .keys.json                  # Per-pack encryption keys
```

### Supported Formats

| Format | Description |
|--------|-------------|
| `.zip` / `.mcpack` | Single resource or behavior pack |
| `.mcaddon` | Multi-pack addon (ZIP containing multiple packs) |
| Folder with `manifest.json` | Auto-packaged to ZIP with content-hash caching |
| Folder with sub-pack directories | Auto-packaged as addon (`.mcaddon`) |

## HTTP API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/ping` | GET | Health check, returns `OK` |
| `/manifest.json` | GET | Pack manifest with MD5 checksums |
| `/packs/{filename}` | GET | Download a pack file |
| `/keys/challenge` | POST | Get encryption challenge (encryption mode only) |
| `/keys/exchange` | POST | Exchange HMAC for decryption key (encryption mode only) |

## Encryption

Enable encryption by setting `encryption-enabled: true` in `server.yml`. See [encryption-guide.md](encryption-guide.md) for details.

## Deployment Examples

### Systemd Service

```ini
[Unit]
Description=Bedrock Pack Distribution Server
After=network.target

[Service]
Type=simple
User=minecraft
WorkingDirectory=/opt/bedrock-packs
ExecStart=/usr/bin/java -jar /opt/bedrock-packs/bedrock-pack-server-1.0.0.jar -d /opt/bedrock-packs/data -p 8080
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

### Docker

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY bedrock-pack-server-1.0.0.jar .
COPY packs/ /app/packs/
EXPOSE 8080
CMD ["java", "-jar", "bedrock-pack-server-1.0.0.jar", "-d", "/app/packs", "-p", "8080"]
```

```bash
docker build -t bedrock-pack-server .
docker run -d -p 8080:8080 -v /path/to/packs:/app/packs bedrock-pack-server
```

---

## 概述

独立资源包分发服务器是一个轻量级 HTTP 服务器，用于向客户端分发基岩版资源包，无需运行完整的 Minecraft/Fabric 服务器。它与 Bedrock Loader 模组中嵌入的 HTTP 服务器共享相同的代码库，支持所有功能，包括 AES-256-CFB8 加密和 Challenge-Response 密钥交换。

## 快速开始

### 构建

```bash
./gradlew :standalone-server:shadowJar
```

输出 JAR 位于 `standalone-server/build/libs/bedrock-pack-server-1.0.0.jar`。

### 运行

```bash
# 默认设置（从 ./config/bedrock-loader/ 读取资源包，端口由 server.yml 决定）
java -jar bedrock-pack-server-1.0.0.jar

# 指定资源包目录和端口
java -jar bedrock-pack-server-1.0.0.jar -d /srv/packs -p 9090
```

### 命令行参数

| 参数 | 缩写 | 说明 | 默认值 |
|------|------|------|--------|
| `--pack-dir <path>` | `-d` | 资源包目录路径 | `config/bedrock-loader` |
| `--config <path>` | `-c` | 配置文件路径 | `<pack-dir>/server.yml` |
| `--port <port>` | `-p` | 服务器端口（覆盖配置文件） | 来自 `server.yml` |
| `--host <host>` | | 绑定地址（覆盖配置文件） | 来自 `server.yml` |
| `--base-url <url>` | | 下载链接基础 URL（覆盖配置文件） | 来自 `server.yml` |
| `--help` | | 显示帮助信息 | |

## 配置

服务器使用与 Fabric 模组相同的 `server.yml` 格式。首次启动时会自动生成默认配置文件：

```yaml
# 启用 HTTP 服务器
enabled: true

# 服务器端口
port: 8080

# 绑定地址
host: "0.0.0.0"

# 下载链接基础 URL（留空自动检测）
base-url: ""

# 启用资源包加密
encryption-enabled: false

# 加密密钥（"auto" 表示自动生成）
encryption-server-secret: "auto"
```

## 资源包目录结构

将基岩版资源包放入资源包目录：

```
config/bedrock-loader/          # 或自定义的 --pack-dir
├── server.yml                  # 服务器配置
├── my-resource-pack.mcpack     # 单个包文件
├── my-addon.mcaddon            # 多包 addon 文件
├── my-pack-folder/             # 未打包的文件夹（自动压缩）
│   └── manifest.json
├── my-addon-folder/            # Addon 文件夹（多子包目录）
│   ├── resource_pack/
│   │   └── manifest.json
│   └── behavior_pack/
│       └── manifest.json
├── .cache/                     # 自动生成的 ZIP 缓存
├── .server_secret              # 自动生成的加密密钥
└── .keys.json                  # 每个包的加密密钥
```

### 支持的格式

| 格式 | 说明 |
|------|------|
| `.zip` / `.mcpack` | 单个资源包或行为包 |
| `.mcaddon` | 多包 addon（包含多个子包的 ZIP） |
| 含 `manifest.json` 的文件夹 | 自动打包为 ZIP，使用内容哈希缓存 |
| 含子包目录的文件夹 | 自动打包为 addon (`.mcaddon`) |

## HTTP API

| 端点 | 方法 | 说明 |
|------|------|------|
| `/ping` | GET | 健康检查，返回 `OK` |
| `/manifest.json` | GET | 资源包清单（含 MD5 校验和） |
| `/packs/{filename}` | GET | 下载资源包文件 |
| `/keys/challenge` | POST | 获取加密 challenge（仅加密模式） |
| `/keys/exchange` | POST | 提交 HMAC 换取解密密钥（仅加密模式） |

## 加密

在 `server.yml` 中设置 `encryption-enabled: true` 启用加密。详见 [encryption-guide.md](encryption-guide.md)。

## 部署示例

### Systemd 服务

```ini
[Unit]
Description=Bedrock Pack Distribution Server
After=network.target

[Service]
Type=simple
User=minecraft
WorkingDirectory=/opt/bedrock-packs
ExecStart=/usr/bin/java -jar /opt/bedrock-packs/bedrock-pack-server-1.0.0.jar -d /opt/bedrock-packs/data -p 8080
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

### Docker

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY bedrock-pack-server-1.0.0.jar .
COPY packs/ /app/packs/
EXPOSE 8080
CMD ["java", "-jar", "bedrock-pack-server-1.0.0.jar", "-d", "/app/packs", "-p", "8080"]
```

```bash
docker build -t bedrock-pack-server .
docker run -d -p 8080:8080 -v /path/to/packs:/app/packs bedrock-pack-server
```
