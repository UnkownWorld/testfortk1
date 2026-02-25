# OpenClaw Zero Token 技术实现文档

## 1. 项目概述

### 1.1 项目简介

OpenClaw Zero Token 是一个多渠道 AI 网关项目，核心目标是**免除 API Token 费用**，通过模拟浏览器登录捕获会话凭证，实现对各大 AI 平台的免费访问。

### 1.2 支持的平台

| 平台 | 状态 | 模型 |
|-----|------|------|
| DeepSeek | ✅ 当前支持 | deepseek-chat, deepseek-reasoner |
| 豆包 (Doubao) | ✅ 当前支持 | doubao（via doubao-free-api） |
| Claude Web | ✅ 当前支持 | claude-3-5-sonnet-20241022, claude-3-opus-20240229, claude-3-haiku-20240307 |
| ChatGPT Web | 🔜 计划中 | - |

### 1.3 技术栈

- **后端**: Node.js 22.12+ / TypeScript
- **前端**: Lit 3.x Web Components
- **安卓应用**: Kotlin + Jetpack Compose
- **通信协议**: WebSocket / HTTP REST API
- **浏览器自动化**: Playwright CDP

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              OpenClaw Zero Token                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐  │
│  │   Web UI    │    │  CLI/TUI    │    │   Gateway   │    │  Channels   │  │
│  │  (Lit 3.x)  │    │             │    │  (Port API) │    │ (Telegram…) │  │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘    └──────┬──────┘  │
│         │                  │                  │                  │          │
│         └──────────────────┴──────────────────┴──────────────────┘          │
│                                    │                                         │
│                           ┌────────▼────────┐                               │
│                           │   Agent Core    │                               │
│                           │  (PI-AI Engine) │                               │
│                           └────────┬────────┘                               │
│                                    │                                         │
│  ┌─────────────────────────────────┼─────────────────────────────────────┐  │
│  │                          Provider Layer                               │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │  │
│  │  │ DeepSeek Web │  │ Doubao Proxy │  │   OpenAI     │  │ Anthropic   │  │  │
│  │  │ (Zero Token) │  │ (Zero Token) │  │   (Token)    │  │  (Token)    │  │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 安卓应用架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Android Application                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                           UI Layer (Jetpack Compose)                 │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │   │
│  │  │  RootScreen  │  │  ChatSheet   │  │ SettingsSheet│               │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌─────────────────────────────────▼───────────────────────────────────┐   │
│  │                        ViewModel Layer                               │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │   │
│  │  │MainViewModel │  │ChatController│  │VoiceManager  │               │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌─────────────────────────────────▼───────────────────────────────────┐   │
│  │                         Service Layer                                │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │   │
│  │  │NodeService   │  │GatewaySession│  │DiscoverySvc  │               │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌─────────────────────────────────▼───────────────────────────────────┐   │
│  │                         Node Capabilities                            │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │   │
│  │  │CameraHandler │  │ScreenHandler │  │LocationHdlr  │               │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 免 Token 实现原理

### 3.1 核心流程

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        DeepSeek Web 认证流程                                │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. 启动浏览器                                                              │
│     ┌─────────────┐                                                        │
│     │ openclaw    │ ──启动──▶ Chrome (CDP Port: 18892)                     │
│     │ gateway     │           带用户数据目录                                │
│     └─────────────┘                                                        │
│                                                                             │
│  2. 用户登录                                                                │
│     ┌─────────────┐                                                        │
│     │ 用户在浏览器 │ ──访问──▶ https://chat.deepseek.com                    │
│     │ 中手动登录  │           扫码/账号密码登录                             │
│     └─────────────┘                                                        │
│                                                                             │
│  3. 捕获凭证                                                                │
│     ┌─────────────┐                                                        │
│     │ Playwright  │ ──监听──▶ 网络请求                                     │
│     │ CDP 连接    │           拦截 Authorization Header                    │
│     └─────────────┘           获取 Cookie                                   │
│                                                                             │
│  4. 存储凭证                                                                │
│     ┌─────────────┐                                                        │
│     │ auth.json   │ ◀──保存── { cookie, bearer, userAgent }               │
│     └─────────────┘                                                        │
│                                                                             │
│  5. API 调用                                                                │
│     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐               │
│     │ DeepSeek    │ ──▶ │ DeepSeek    │ ──▶ │ chat.deep-  │               │
│     │ WebClient   │     │ Web API     │     │ seek.com    │               │
│     └─────────────┘     └─────────────┘     └─────────────┘               │
│         使用存储的 Cookie + Bearer Token                                    │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 关键技术点

| 技术点 | 实现方式 |
|-------|---------|
| **浏览器自动化** | Playwright CDP 连接 Chrome |
| **凭证捕获** | 监听网络请求，提取 Authorization Header |
| **PoW 挑战** | WASM SHA3 计算反爬答案 |
| **流式响应** | SSE 解析 + 自定义标签解析器 |

---

## 4. 安卓应用实现

### 4.1 项目结构

```
apps/android/
├── app/
│   ├── src/main/
│   │   ├── java/ai/openclaw/android/
│   │   │   ├── MainActivity.kt           # 主Activity
│   │   │   ├── MainViewModel.kt          # 主ViewModel
│   │   │   ├── NodeApp.kt                # Application类
│   │   │   ├── NodeForegroundService.kt  # 前台服务
│   │   │   ├── gateway/                  # 网关通信模块
│   │   │   │   ├── GatewaySession.kt     # WebSocket会话
│   │   │   │   ├── GatewayDiscovery.kt   # 服务发现
│   │   │   │   ├── GatewayEndpoint.kt    # 端点定义
│   │   │   │   └── GatewayTls.kt         # TLS配置
│   │   │   ├── node/                     # 节点能力模块
│   │   │   │   ├── CameraHandler.kt      # 相机处理
│   │   │   │   ├── ScreenHandler.kt      # 屏幕捕获
│   │   │   │   ├── LocationHandler.kt    # 位置服务
│   │   │   │   └── InvokeDispatcher.kt   # 命令分发
│   │   │   ├── chat/                     # 聊天模块
│   │   │   │   ├── ChatController.kt     # 聊天控制器
│   │   │   │   └── ChatModels.kt         # 数据模型
│   │   │   ├── voice/                    # 语音模块
│   │   │   │   ├── VoiceWakeManager.kt   # 语音唤醒
│   │   │   │   └── TalkModeManager.kt    # 对话模式
│   │   │   └── ui/                       # UI组件
│   │   │       ├── RootScreen.kt         # 主屏幕
│   │   │       ├── ChatSheet.kt          # 聊天界面
│   │   │       └── SettingsSheet.kt      # 设置界面
│   │   ├── res/                          # 资源文件
│   │   └── AndroidManifest.xml           # 清单文件
│   └── build.gradle.kts                  # 构建配置
├── gradle/                               # Gradle包装器
├── build.gradle.kts                      # 项目构建配置
└── settings.gradle.kts                   # 项目设置
```

### 4.2 核心组件说明

#### 4.2.1 GatewaySession（网关会话）

负责与 OpenClaw Gateway 建立 WebSocket 连接，处理认证和消息传输。

**主要功能：**
- WebSocket 连接管理
- 设备认证（基于公钥签名）
- 请求/响应模式通信
- 事件订阅和分发
- TLS 证书固定

**关键代码示例：**
```kotlin
class GatewaySession(
  private val scope: CoroutineScope,
  private val identityStore: DeviceIdentityStore,
  private val deviceAuthStore: DeviceAuthStore,
  private val onConnected: (serverName: String?, remoteAddress: String?, mainSessionKey: String?) -> Unit,
  private val onDisconnected: (message: String) -> Unit,
  private val onEvent: (event: String, payloadJson: String?) -> Unit,
) {
  fun connect(
    endpoint: GatewayEndpoint,
    token: String?,
    password: String?,
    options: GatewayConnectOptions,
    tls: GatewayTlsParams? = null,
  ) { /* ... */ }
  
  suspend fun request(method: String, paramsJson: String?, timeoutMs: Long = 15_000): String { /* ... */ }
}
```

#### 4.2.2 NodeForegroundService（前台服务）

保持应用在后台运行，处理节点命令和事件。

**主要功能：**
- 保持后台运行状态
- 处理来自 Gateway 的命令
- 管理相机、屏幕、位置等硬件资源
- 发送节点事件到 Gateway

#### 4.2.3 CameraHandler（相机处理）

处理相机相关命令，支持拍照和视频录制。

**支持的命令：**
- `camera.capture` - 拍照
- `camera.startVideo` - 开始录像
- `camera.stopVideo` - 停止录像

#### 4.2.4 ChatController（聊天控制器）

管理聊天会话和消息发送。

**主要功能：**
- 管理 WebSocket 连接
- 发送和接收聊天消息
- 处理流式响应
- 管理会话历史

### 4.3 通信协议

#### 4.3.1 WebSocket 帧格式

```json
// 请求帧
{
  "type": "req",
  "id": "uuid",
  "method": "method.name",
  "params": { /* 参数对象 */ }
}

// 响应帧
{
  "type": "res",
  "id": "uuid",
  "ok": true,
  "payload": { /* 响应数据 */ }
}

// 事件帧
{
  "type": "event",
  "event": "event.name",
  "payload": { /* 事件数据 */ }
}
```

#### 4.3.2 主要 API 方法

| 方法 | 描述 |
|------|------|
| `connect` | 建立连接并认证 |
| `chat.send` | 发送聊天消息 |
| `chat.abort` | 中止当前对话 |
| `node.invoke` | 调用节点命令 |
| `node.event` | 发送节点事件 |
| `sessions.list` | 列出会话 |
| `sessions.get` | 获取会话详情 |

### 4.4 权限配置

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.SEND_SMS" />
```

---

## 5. 构建和部署

### 5.1 环境要求

- Node.js >= 22.12.0
- pnpm >= 9.0.0
- Android SDK 36
- JDK 17
- Chrome 浏览器

### 5.2 构建步骤

#### 5.2.1 构建后端

```bash
# 安装依赖
pnpm install

# 编译项目
pnpm build
```

#### 5.2.2 构建安卓应用

```bash
cd apps/android

# 调试版本
./gradlew assembleDebug

# 发布版本
./gradlew assembleRelease
```

### 5.3 运行步骤

```bash
# 启动 Gateway
node openclaw.mjs gateway

# 访问 Web UI
open http://127.0.0.1:3001
```

---

## 6. 安全注意事项

1. **凭证存储**: Cookie 和 Bearer Token 存储在本地 `auth.json`，**绝不提交到 Git**
2. **会话有效期**: Web 会话可能过期，需要定期重新登录
3. **使用限制**: Web API 可能有速率限制，不适合高频调用
4. **合规使用**: 仅用于个人学习研究，请遵守平台服务条款

---

## 7. 开发路线

### 当前重点
- ✅ DeepSeek Web 认证（稳定）
- ✅ 豆包 via doubao-free-api
- ✅ Claude Web 认证（稳定）
- 🔧 提高凭证捕获可靠性
- 📝 文档改进

### 计划功能
- 🔜 ChatGPT Web 认证支持
- 🔜 过期会话自动刷新

---

## 8. 许可证

[MIT License](LICENSE)

---

## 9. 致谢

- [OpenClaw](https://github.com/openclaw/openclaw) - 原始项目
- [DeepSeek](https://deepseek.com) - 优秀的 AI 模型
