# 资源包加密/解密功能使用指南

## 概述

BedrockLoader 支持对远程分发的资源包进行 AES-256-CFB8 加密保护。启用后：

- **服务端**始终持有未加密的原始包，仅在 HTTP 分发时提供加密版本
- **客户端**将密文缓存到磁盘（`remote/` 目录），每次启动通过 Challenge-Response 握手获取解密密钥，在内存中解密
- **明文从不写入客户端磁盘**

## 快速开始

### 服务端配置

编辑 `config/bedrock-loader/server.yml`：

```yaml
# 启用HTTP服务器
enabled: true
port: 8080
host: "0.0.0.0"

# 启用资源包加密
encryption-enabled: true

# 加密密钥（推荐使用 "auto" 自动生成）
encryption-server-secret: "auto"
```

仅需这两个字段即可启用加密。首次启动时会自动生成 `server_secret` 并持久化到 `config/bedrock-loader/.server_secret` 文件。

### 客户端配置

**无需任何加密相关配置。** 客户端只需正常配置远程同步：

```yaml
# config/bedrock-loader/client.yml
enabled: true
serverUrl: "http://your-server-ip:8080"
```

加密的检测、密钥交换、解密全部自动完成。

## 工作原理

### 加密流程（服务端启动时）

```
1. 读取 server.yml 中的 encryption-server-secret
   ├── "auto" → 从 .server_secret 文件加载（首次自动生成）
   └── 自定义值 → 直接使用

2. 从 server_secret 生成：
   ├── server_token（公开，发布在 manifest 中）
   └── shared_secret（内部使用，用于验证 Challenge-Response）

3. PackKeyManager 为每个资源包生成独立的 AES-256 密钥
   └── 持久化到 .keys.json（服务端重启后密钥不变）

4. EncryptedPackCache 预加密所有包并缓存
   └── 检测原始文件 MD5 变化，自动重新加密
```

### 客户端同步流程

```
1. 获取 manifest.json
   └── 检测 encryption.enabled == true

2. 下载密文到 remote/ 目录（与非加密模式的下载逻辑一致）

3. Challenge-Response 密钥交换（每个包一次）：
   ├── POST /keys/challenge → 获取一次性 challenge（30秒过期）
   ├── 从 manifest 中的 server_token + mod 内嵌 MOD_KEY 派生 shared_secret
   ├── 计算 HMAC = HMAC-SHA256(shared_secret, challenge + "|" + filename)
   └── POST /keys/exchange → 提交 HMAC 换取 AES 解密密钥

4. 从磁盘读取密文 → AES-256-CFB8 解密 → 存入内存（InMemoryPackStore）

5. BedrockAddonsLoader 从 InMemoryPackStore 加载解密后的包
```

### 加密文件格式

```
[16 字节随机 IV] + [AES-256-CFB8 加密的 ZIP 数据]
```

- 使用 `javax.crypto.Cipher("AES/CFB8/NoPadding")`
- CFB8 模式无需填充，输出大小 = 输入大小 + 16 字节 IV

## 安全模型

### 防护层级

| 攻击方式 | 防护效果 |
|---------|---------|
| 浏览器直接访问下载链接 | **已防护** - 下载到的是密文，无法解压 |
| 浏览器访问密钥接口 | **已防护** - 需要 Challenge-Response HMAC 验证 |
| curl/脚本模拟请求 | **已防护** - 不知道 shared_secret，无法计算有效 HMAC |
| 反编译 mod 提取 MOD_KEY | **部分防护** - MOD_KEY 经 XOR 编码拆分存储，ProGuard 混淆类名/方法名 |

### Shared Secret 派生机制

```
MOD_KEY（硬编码在 mod JAR 中，ProGuard 混淆保护）
    +
server_token（从 manifest 公开获取）
    ↓
shared_secret = deriveSharedSecret(serverToken)
    Step 1: HMAC-SHA256(MOD_KEY, serverToken)
    Step 2: XOR with token bytes
    Step 3: SHA-256(xored + SALT)
    Step 4: HMAC-SHA256(intermediate, MOD_KEY + step1_result)
```

- **浏览器用户**：能看到 `server_token`，但不知道 `MOD_KEY` 和派生函数，无法推导 `shared_secret`
- **逆向工程者**：需要反编译 mod → 找到混淆后的 `PackEncryption` 类 → 理解多轮派生逻辑
- **安全边界**：这是客户端 DRM 的固有限制，无法完全防止反编译。生产环境建议配合 HTTPS 保护传输层

### Challenge 安全机制

- **一次性使用**：每个 challenge 验证后立即失效
- **30 秒过期**：防止重放攻击
- **绑定文件名**：HMAC 包含 filename，一个 challenge 只能换取一个包的密钥
- **HMAC 失败即消耗**：防止暴力尝试

## HTTP API

### GET /manifest.json

返回资源包清单。加密模式下包含 `encryption` 配置：

```json
{
  "version": "3.0",
  "generated_at": 1700000000000,
  "packs": [
    {
      "name": "my-pack.zip",
      "type": "pack",
      "uuid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      "md5": "abc123...",
      "size": 1048576,
      "url": "/packs/my-pack.zip",
      "encrypted": true
    }
  ],
  "encryption": {
    "enabled": true,
    "algorithm": "AES-256-CFB8",
    "server_token": "64位十六进制字符串"
  }
}
```

注意：加密模式下 `md5` 和 `size` 对应的是**密文数据**，而非原始文件。

### GET /packs/{filename}

下载资源包。加密模式下返回加密后的数据。

### POST /keys/challenge

获取一次性 challenge。

请求体：
```json
{ "client_id": "随机标识（可选）" }
```

响应：
```json
{
  "challenge": "32位十六进制字符串",
  "expires_at": 1700000030000
}
```

### POST /keys/exchange

提交 HMAC 换取解密密钥。

请求体：
```json
{
  "challenge": "从 /keys/challenge 获取的值",
  "filename": "my-pack.zip",
  "hmac": "HMAC-SHA256 计算结果"
}
```

成功响应：
```json
{ "key": "64位十六进制 AES-256 密钥" }
```

失败响应（403）：
```json
{ "error": "Authentication failed" }
```

## 服务端生成的文件

启用加密后，服务端会在 `config/bedrock-loader/` 下生成以下文件：

| 文件 | 说明 | 安全性 |
|------|------|--------|
| `.server_secret` | 自动生成的 server_secret（`encryption-server-secret: "auto"` 时） | **敏感** - 不要泄露 |
| `.keys.json` | 每个包的 AES-256 加密密钥映射 | **敏感** - 不要泄露 |
| `server.yml` | 服务端配置 | 包含 secret 配置，注意保护 |

## 注意事项

### 内存占用

所有解密后的包会驻留在内存中（`InMemoryPackStore`）。启动日志会打印内存占用信息：

```
[BedrockLoader/InMemoryPackStore] Total memory packs: 3, total size: 15.2 MB
```

对于大量或大体积的资源包，请注意分配足够的 JVM 堆内存。

### 向后兼容

- 加密为可选功能，默认关闭（`encryption-enabled: false`）
- 关闭加密时，行为与之前完全一致
- manifest 版本从 2.0 升级到 3.0，旧版客户端收到 3.0 manifest 时仍能正常读取包列表（忽略不认识的 `encryption` 字段）

### 密钥轮换

如需更换加密密钥：

1. 删除 `config/bedrock-loader/.keys.json`
2. 重启服务端
3. 新密钥会自动生成，客户端下次同步时会重新下载密文并获取新密钥

如需更换 server_secret（同时使所有已派生的 shared_secret 失效）：

1. 删除 `config/bedrock-loader/.server_secret`（或修改 `server.yml` 中的 `encryption-server-secret`）
2. 删除 `.keys.json`
3. 重启服务端

### mcaddon 支持

加密对 `.mcaddon` 文件透明支持。mcaddon 本质也是 ZIP 文件，整包加密后客户端解密到内存，`InMemoryZipPack` 能正确读取其中的子包路径。

### 多版本兼容

加密相关代码（`PackEncryption`、`InMemoryZipPack` 等）不涉及 Minecraft API，无需 Stonecutter 条件编译，所有版本（1.20.4 - 1.21.11）共用同一份代码。

## 故障排查

### 客户端日志关键词

```
# 成功流程
[BedrockLoader/KeyExchange] Challenge obtained: a1b2c3d4...
[BedrockLoader/KeyExchange] Key exchanged for: my-pack.zip
[BedrockLoader/RemoteSync] Decrypting: my-pack.zip (1.5 MB)
[BedrockLoader/RemoteSync] Decrypted to memory: my-pack.zip (42 entries)
[BedrockLoader/InMemoryPackStore] Total memory packs: 1, total size: 3.2 MB

# 认证失败
[BedrockLoader/KeyExchange] Key exchange authentication failed for: my-pack.zip
```

### 服务端日志关键词

```
# 启动成功
[BedrockLoader] 初始化资源包加密系统...
[BedrockLoader] 资源包加密系统初始化完成 (server_token: a1b2c3d4e5f6...)
[BedrockLoader/HttpServer] Encryption routes registered: POST /keys/challenge, POST /keys/exchange

# Challenge 验证
[BedrockLoader/ChallengeManager] Challenge created: a1b2c3d4...
[BedrockLoader/ChallengeManager] Challenge verified and consumed for: my-pack.zip

# 加密缓存
[BedrockLoader/EncryptedPackCache] Encrypting pack: my-pack.zip (1.5 MB)
[BedrockLoader/EncryptedPackCache] Pack encrypted: my-pack.zip (1.5 MB -> 1.5 MB)
```

### 常见问题

**Q: 客户端报 "Authentication failed"**
A: 检查客户端和服务端的 mod 版本是否一致。MOD_KEY 硬编码在 mod 中，版本不同可能导致 shared_secret 不匹配。

**Q: 服务端重启后客户端需要重新下载吗？**
A: 不需要。密钥持久化在 `.keys.json` 中，重启后密钥不变，密文 MD5 不变，客户端判断为"已是最新"，只需重新执行密钥交换即可。

**Q: 能否同时分发加密和非加密的包？**
A: 目前不支持。`encryption-enabled` 是全局开关，启用后所有包都会被加密。
