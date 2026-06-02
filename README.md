# Minecraft_Native_Antichcat
# native-anticheat

一个基于 **Fabric** 平台、面向 Minecraft **1.21.x** 的协作型反作弊工具。客户端与服务端协同工作：客户端在加入服务器时主动上报已加载的 mod 列表及环境指纹，服务端通过白名单校验与多维度信号综合判断，对可疑玩家打上**对玩家不可见**的"疑似作弊"标签。

> ⚠️ **重要声明**：本工具属于**协作型（cooperative）反作弊**，依赖客户端诚实上报，**无法对抗有能力的攻击者**。它适合作为社区服务器的**辅助风控信号**，绝不应作为唯一防线。真正可靠的反作弊必须依赖**服务端行为分析**。

---

## ✨ 功能特性

- 🔍 **Mod 列表上报**：客户端加入时自动收集所有已加载 mod 并上报服务端
- ✅ **白名单校验**：服务端比对白名单，识别非白名单 mod
- 🔐 **挑战-应答机制**：服务端下发随机 nonce，客户端签名回传，防止重放攻击
- 🧪 **多维度环境扫描**：检测可疑类、线程、JVM 参数、系统属性等作弊痕迹
- 🛡️ **完整性自检**：HMAC 签名 + Java Agent 注入检测
- 🏷️ **静默标记**：可疑玩家标签仅存于服务端，玩家无任何感知
- 📝 **可配置白名单**：JSON 配置文件，支持自定义合法 mod 列表

---

## 📦 项目结构

```
native-anticheat/
├── server/                          # 服务端 mod
│   └── src/main/java/com/example/nativeanticheat/
│       ├── NativeAnticheat.java     # 服务端主入口
│       ├── ModListPayload.java      # 网络载荷定义
│       ├── ModWhitelist.java        # 白名单管理
│       └── SuspectManager.java      # 疑似作弊标记管理
│
└── client/                          # 客户端 mod
    └── src/main/java/com/example/nativeanticheat/client/
        ├── NativeAnticheatClient.java  # 客户端主入口
        ├── ModListPayload.java         # 网络载荷（与服务端同步）
        ├── ChallengePayload.java       # 挑战载荷
        ├── ModCollector.java           # mod 信息收集
        ├── IntegrityChecker.java       # 完整性 / 签名
        ├── EnvironmentScanner.java     # 多维度环境扫描
        └── HandshakeManager.java       # 握手通信逻辑
```

---

## 🔧 环境要求

| 项目 | 版本 |
|------|------|
| Minecraft | 1.21.x（推荐 1.21.1） |
| Fabric Loader | ≥ 0.16.0 |
| Fabric API | 0.102.0+1.21.1 或更高 |
| Java | 21+ |
| Gradle | 8.x（含 Loom 1.7） |

---

## 🚀 安装方法

### 服务端

1. 将编译好的 `native-anticheat-server-1.0.0.jar` 放入服务器的 `mods/` 目录
2. 确保已安装 **Fabric API**
3. 启动服务器，首次运行会在 `config/native-anticheat/whitelist.json` 生成默认白名单
4. 编辑白名单后重启服务器

### 客户端

1. 将编译好的 `native-anticheat-client-1.0.0.jar` 放入客户端 `.minecraft/mods/` 目录
2. 确保已安装 **Fabric API**
3. 启动游戏即可

> 客户端**未安装**本 mod 的玩家在加入服务器时会被标记为 `__no_anticheat_client__`（行为可按服务端策略调整）。

---

## ⚙️ 配置说明

### 白名单文件

路径：`config/native-anticheat/whitelist.json`

```json
[
  "minecraft",
  "fabricloader",
  "fabric-api",
  "java",
  "native-anticheat"
]
```

- 数组中的每一项为 **mod ID**（不是 mod 名称）
- 不在白名单中的 mod 将触发可疑标记
- 修改后需**重启服务器**生效

> 💡 可通过客户端日志或 `/datapacks`、mod 管理界面查看 mod ID。

---

## 🔄 工作流程

```
┌──────────┐                              ┌──────────┐
│  客户端  │                              │  服务端  │
└────┬─────┘                              └────┬─────┘
     │                                         │
     │  1. 玩家加入服务器                       │
     │ ───────────────────────────────────────>│
     │                                         │
     │         2. 检测客户端是否安装本 mod      │
     │            (canSend 判断)                │
     │                                         │
     │  3. 下发随机挑战 nonce (S2C)             │
     │ <───────────────────────────────────────│
     │                                         │
     │  4. 收集 mod 列表 + 环境扫描 + HMAC 签名 │
     │                                         │
     │  5. 上报 ModListPayload (C2S)           │
     │ ───────────────────────────────────────>│
     │                                         │
     │         6. 校验 nonce + 签名             │
     │            白名单比对 + 可疑项分析        │
     │            静默标记可疑玩家               │
     │                                         │
```

---

## 🌐 网络协议

### 通道标识

| 通道 | 方向 | 说明 |
|------|------|------|
| `native-anticheat:challenge` | S2C | 服务端下发随机 nonce |
| `native-anticheat:mod_list` | C2S | 客户端上报 mod 列表与指纹 |

### ModListPayload 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `modIds` | `List<String>` | 已加载 mod ID 列表 |
| `fileHashes` | `List<String>` | 各 mod 的 `fabric.mod.json` SHA-256 |
| `suspiciousHits` | `List<String>` | 环境扫描发现的可疑项 |
| `clientNonce` | `long` | 服务端下发 nonce 的回显 |
| `integritySignature` | `String` | HMAC-SHA256 签名 |

---

## 🔬 检测维度

| 维度 | 检测方法 | 绕过难度 |
|------|----------|----------|
| Mod 列表 | Fabric Loader API 枚举 | 低 |
| 文件哈希 | 对 mod 元数据求 SHA-256 | 中 |
| 已加载类扫描 | `Class.forName` 探测作弊客户端特征类 | 中 |
| 可疑线程名 | 遍历 `ThreadGroup` | 中 |
| JVM 参数 | `RuntimeMXBean` 检测 `-javaagent` | 中 |
| 系统属性 | 检测 Mixin 调试标志 | 低 |
| 防重放 | 服务端 nonce 挑战 | 高 |
| 防篡改 | HMAC 签名 | 中 |

### 已识别的作弊客户端特征

当前内置检测以下客户端的特征包名（可在 `EnvironmentScanner.java` 中扩展）：

- Wurst (`net.wurstclient`)
- Meteor Client (`meteordevelopment`)
- Lambda (`com.lambda`)
- 部分基于 Alpine 事件库的客户端 (`me.zero.alpine`)
- 其他（`dev.boze`、`wtf.expensive` 等）

---

## 🏗️ 构建方法

```bash
# 服务端
cd server
./gradlew build

# 客户端
cd client
./gradlew build
```

构建产物位于各自的 `build/libs/` 目录下。

---

## ⚠️ 局限性与已知问题

本工具是**协作型反作弊**，存在以下根本性局限：

1. **客户端运行在不可信环境**
   任何客户端检测理论上都可被绕过。

2. **共享密钥可被逆向**
   `IntegrityChecker.SHARED_SECRET` 硬编码在客户端，可通过反编译提取。HMAC 签名只能防"无脑伪造"，挡不住有心人。

3. **枚举结果可被 Hook**
   作弊客户端可通过 Mixin 注入 `FabricLoader.getAllMods()`，返回虚假 mod 列表。

4. **特征检测易被规避**
   修改包名、类名即可绕过基于特征字符串的检测。

5. **无法检测纯外部作弊**
   如外部自动点击器、硬件宏、网络层修改等不在 JVM 内的作弊手段。

### 推荐做法

- 将本工具的上报数据作为**辅助风控信号**之一
- 与服务端的**行为检测**（移动速度、命中判定、飞行、挖矿速度等）结合
- 建立**综合评分系统**，多信号加权后再决定是否人工复核或处置
- **不要**仅凭客户端上报直接封禁玩家

---

## 🗺️ 路线图（TODO）

- [ ] 服务端 `NonceStore`（nonce 存储与超时清理）
- [ ] 综合风控评分系统
- [ ] 管理员查询命令 `/anticheat suspects`
- [ ] 热重载白名单命令 `/anticheat reload`
- [ ] 服务端行为检测模块（移动 / 命中 / 速度）
- [ ] 上报数据持久化与查询接口
- [ ] 白名单支持文件哈希精确匹配（防止同 ID 不同版本）
- [ ] 多语言支持

---

## 📄 许可证

MIT License

---

## 🤝 贡献

欢迎提交 Issue 与 Pull Request。提交前请确保：

- 代码遵循项目现有风格
- 新增检测维度需在 README 的"检测维度"表中补充说明
- 不引入对玩家可见的封禁逻辑（标记应保持静默，处置交由服务器管理员）

---

## 💬 致谢

- [Fabric](https://fabricmc.net/) — Mod 加载器与 API
- Minecraft 反作弊社区的经验分享

---

> **再次提醒**：反作弊是攻防博弈的持续过程，没有一劳永逸的方案。本工具旨在提高作弊成本，请理性看待其能力边界。
