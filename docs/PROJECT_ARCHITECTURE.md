# OpenClaw Zero Token 项目架构文档

## 目录

1. [项目概述](#1-项目概述)
2. [核心功能模块](#2-核心功能模块)
3. [系统架构](#3-系统架构)
4. [后端模块详解](#4-后端模块详解)
5. [安卓应用模块详解](#5-安卓应用模块详解)
6. [类图与流程图](#6-类图与流程图)
7. [API 接口文档](#7-api-接口文档)
8. [配置说明](#8-配置说明)

---

## 1. 项目概述

### 1.1 项目简介

OpenClaw Zero Token 是一个多渠道 AI 网关项目，核心目标是**免除 API Token 费用**，通过模拟浏览器登录捕获会话凭证，实现对各大 AI 平台的免费访问。

### 1.2 技术栈

| 层级 | 技术 |
|------|------|
| 后端运行时 | Node.js 22.12+ / TypeScript |
| 前端 UI | Lit 3.x Web Components |
| 安卓应用 | Kotlin + Jetpack Compose |
| iOS 应用 | Swift / SwiftUI |
| 通信协议 | WebSocket / HTTP REST API |
| 浏览器自动化 | Playwright CDP |
| 数据序列化 | JSON / Protocol Buffers |

### 1.3 支持的平台

| 平台 | 状态 | 认证方式 | 模型 |
|------|------|----------|------|
| DeepSeek | ✅ | Web Cookie + Bearer | deepseek-chat, deepseek-reasoner |
| 豆包 (Doubao) | ✅ | doubao-free-api 代理 | doubao |
| Claude Web | ✅ | 浏览器会话 | claude-3-5-sonnet, claude-3-opus, claude-3-haiku |
| ChatGPT Web | 🔜 | 计划中 | - |

---

## 2. 核心功能模块

### 2.1 功能模块总览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          OpenClaw Zero Token 功能模块                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   Gateway    │  │   Channels   │  │    Agent     │  │   Browser    │    │
│  │   网关服务   │  │   通道管理   │  │   AI 代理    │  │   浏览器     │    │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘    │
│                                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   Memory     │  │    Tools     │  │    Skills    │  │    TTS       │    │
│  │   记忆系统   │  │   工具集     │  │   技能插件   │  │   语音合成   │    │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘    │
│                                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   Plugins    │  │    Hooks     │  │    Cron      │  │   Security   │    │
│  │   插件系统   │  │   钩子机制   │  │   定时任务   │  │   安全模块   │    │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 模块功能说明

#### 2.2.1 Gateway（网关服务）

**功能描述**：核心通信枢纽，处理所有客户端连接和消息路由。

**主要职责**：
- WebSocket 服务器管理
- 设备认证与授权
- 消息路由与广播
- 会话状态管理
- 配置热更新

**关键文件**：
- `src/gateway/server.ts` - 网关主服务器
- `src/gateway/auth.ts` - 认证逻辑
- `src/gateway/server-methods/` - API 方法实现

#### 2.2.2 Channels（通道管理）

**功能描述**：管理多个消息通道（Slack、Discord、Telegram、WhatsApp 等）。

**支持的通道**：
| 通道 | 文件位置 | 功能 |
|------|----------|------|
| Slack | `src/slack/` | 消息收发、线程管理、反应处理 |
| Discord | `src/discord/` | 消息收发、语音消息、权限管理 |
| Telegram | `src/telegram/` | Bot API、Webhook、内联按钮 |
| WhatsApp | `src/web/` | 二维码登录、消息同步 |
| Signal | `src/signal/` | 消息收发、反应处理 |
| iMessage | `src/imessage/` | macOS iMessage 集成 |
| LINE | `src/line/` | LINE Bot 集成 |

#### 2.2.3 Agent（AI 代理）

**功能描述**：AI 模型调用和对话管理的核心模块。

**主要组件**：
- **PI Engine**：核心推理引擎
- **Model Catalog**：模型目录管理
- **Auth Profiles**：认证配置管理
- **Sandbox**：安全沙箱环境

**关键文件**：
- `src/agents/pi-embedded.ts` - 嵌入式 PI 引擎
- `src/agents/models-config.ts` - 模型配置
- `src/agents/auth-profiles/` - 认证配置
- `src/agents/sandbox/` - 沙箱环境

#### 2.2.4 Browser（浏览器自动化）

**功能描述**：浏览器自动化和网页交互。

**主要功能**：
- Playwright CDP 集成
- 网页截图和导航
- 表单填写和点击
- 文件下载和上传

**关键文件**：
- `src/browser/server.ts` - 浏览器服务器
- `src/browser/pw-tools-core.ts` - Playwright 工具核心
- `src/browser/client.ts` - 客户端连接

#### 2.2.5 Memory（记忆系统）

**功能描述**：向量数据库和长期记忆存储。

**主要功能**：
- 向量嵌入存储
- 语义搜索
- 记忆检索和更新
- 多后端支持（SQLite、OpenAI、Voyage）

**关键文件**：
- `src/memory/manager.ts` - 记忆管理器
- `src/memory/embeddings.ts` - 嵌入处理
- `src/memory/search-manager.ts` - 搜索管理

#### 2.2.6 Tools（工具集）

**功能描述**：AI 可调用的工具集合。

**工具列表**：
| 工具 | 文件 | 功能 |
|------|------|------|
| web-fetch | `src/agents/tools/web-fetch.ts` | 网页抓取 |
| web-search | `src/agents/tools/web-search.ts` | 网页搜索 |
| bash | `src/agents/bash-tools.ts` | 命令执行 |
| memory | `src/agents/tools/memory-tool.ts` | 记忆操作 |
| sessions | `src/agents/tools/sessions-send-tool.ts` | 会话管理 |
| discord-actions | `src/agents/tools/discord-actions.ts` | Discord 操作 |
| slack-actions | `src/agents/tools/slack-actions.ts` | Slack 操作 |
| telegram-actions | `src/agents/tools/telegram-actions.ts` | Telegram 操作 |
| whatsapp-actions | `src/agents/tools/whatsapp-actions.ts` | WhatsApp 操作 |
| image-tool | `src/agents/tools/image-tool.ts` | 图像生成 |
| tts-tool | `src/agents/tools/tts-tool.ts` | 语音合成 |
| cron-tool | `src/agents/tools/cron-tool.ts` | 定时任务 |
| subagents-tool | `src/agents/tools/subagents-tool.ts` | 子代理 |
| browser-tool | `src/agents/tools/browser-tool.ts` | 浏览器操作 |

#### 2.2.7 Skills（技能插件）

**功能描述**：可扩展的技能插件系统。

**主要功能**：
- 技能安装和管理
- 工作区技能
- 捆绑技能
- 技能刷新

**关键文件**：
- `src/agents/skills.ts` - 技能管理
- `src/agents/skills-install.ts` - 技能安装
- `src/agents/skills/workspace.ts` - 工作区技能

#### 2.2.8 TTS（语音合成）

**功能描述**：文本转语音服务。

**关键文件**：
- `src/tts/tts.ts` - TTS 核心实现
- `src/tts/tts-core.ts` - TTS 核心逻辑

#### 2.2.9 Plugins（插件系统）

**功能描述**：插件加载和管理。

**关键文件**：
- `src/plugins/loader.ts` - 插件加载器
- `src/plugins/runtime.ts` - 插件运行时
- `src/plugins/hooks.ts` - 插件钩子

#### 2.2.10 Hooks（钩子机制）

**功能描述**：事件钩子和生命周期管理。

**关键文件**：
- `src/hooks/hooks.ts` - 钩子定义
- `src/hooks/loader.ts` - 钩子加载器
- `src/hooks/bundled/` - 内置钩子

#### 2.2.11 Cron（定时任务）

**功能描述**：定时任务调度和执行。

**关键文件**：
- `src/cron/service.ts` - 任务服务
- `src/cron/isolated-agent.ts` - 隔离代理
- `src/cron/schedule.ts` - 调度逻辑

#### 2.2.12 Security（安全模块）

**功能描述**：安全审计和权限控制。

**关键文件**：
- `src/security/audit.ts` - 安全审计
- `src/security/skill-scanner.ts` - 技能扫描
- `src/security/dangerous-tools.ts` - 危险工具检测

---

## 3. 系统架构

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              用户界面层                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Web UI    │  │   CLI/TUI   │  │  Android    │  │    iOS      │        │
│  │  (Lit 3.x)  │  │  (Terminal) │  │  (Kotlin)   │  │  (Swift)    │        │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘        │
└─────────┼────────────────┼────────────────┼────────────────┼───────────────┘
          │                │                │                │
          └────────────────┴────────────────┴────────────────┘
                                    │
                           ┌────────▼────────┐
                           │   Gateway API   │
                           │  (WebSocket)    │
                           └────────┬────────┘
                                    │
┌───────────────────────────────────┼───────────────────────────────────────┐
│                           核心服务层                                        │
├───────────────────────────────────┼───────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐ │ ┌─────────────┐  ┌─────────────┐      │
│  │   Auth      │  │  Session    │  │ │   Config    │  │   Event     │      │
│  │   认证      │  │  会话管理   │◄├─┤►│   配置      │  │   事件      │      │
│  └─────────────┘  └─────────────┘ │ └─────────────┘  └─────────────┘      │
│                                    │                                        │
│  ┌─────────────┐  ┌─────────────┐ │ ┌─────────────┐  ┌─────────────┐      │
│  │   Agent     │  │   Memory    │  │ │   Tools     │  │   Skills    │      │
│  │   AI代理    │◄─┼─┤►  记忆系统  │◄├─┤►│   工具集    │  │   技能      │      │
│  └─────────────┘  └─────────────┘ │ └─────────────┘  └─────────────┘      │
│                                    │                                        │
└───────────────────────────────────┼───────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼───────────────────────────────────────┐
│                           通道适配层                                        │
├───────────────────────────────────┼───────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐ │ ┌─────────────┐  ┌─────────────┐      │
│  │   Slack     │  │  Discord    │  │ │  Telegram   │  │  WhatsApp   │      │
│  └─────────────┘  └─────────────┘ │ └─────────────┘  └─────────────┘      │
│  ┌─────────────┐  ┌─────────────┐ │ ┌─────────────┐  ┌─────────────┐      │
│  │   Signal    │  │  iMessage   │  │ │    LINE     │  │    Web      │      │
│  └─────────────┘  └─────────────┘ │ └─────────────┘  └─────────────┘      │
└───────────────────────────────────┼───────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼───────────────────────────────────────┐
│                           Provider 层                                       │
├───────────────────────────────────┼───────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐ │ ┌─────────────┐  ┌─────────────┐      │
│  │ DeepSeek    │  │  Claude     │  │ │   OpenAI    │  │  Anthropic  │      │
│  │   Web       │  │    Web      │◄├─┤►│   (Token)   │  │   (Token)   │      │
│  │ (ZeroToken) │  │ (ZeroToken) │ │ └─────────────┘  └─────────────┘      │
│  └─────────────┘  └─────────────┘ │                                        │
│  ┌─────────────┐  ┌─────────────┐ │ ┌─────────────┐  ┌─────────────┐      │
│  │  Doubao     │  │   Gemini    │  │ │   Groq      │  │   Ollama    │      │
│  │   Proxy     │  │             │◄├─┤►│             │  │   (Local)   │      │
│  └─────────────┘  └─────────────┘ │ └─────────────┘  └─────────────┘      │
└───────────────────────────────────┼───────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼───────────────────────────────────────┐
│                           基础设施层                                        │
├───────────────────────────────────┼───────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐ │ ┌─────────────┐  ┌─────────────┐      │
│  │   Browser   │  │   Process   │  │ │    File     │  │   Network   │      │
│  │ Automation  │  │  Supervisor │◄├─┤►│   System    │  │   Layer     │      │
│  └─────────────┘  └─────────────┘ │ └─────────────┘  └─────────────┘      │
│  ┌─────────────┐  ┌─────────────┐ │ ┌─────────────┐  ┌─────────────┐      │
│  │   Daemon    │  │   Logger    │  │ │   Config    │  │   Security  │      │
│  │   Service   │  │   System    │◄├─┤►│   Store     │  │   Module    │      │
│  └─────────────┘  └─────────────┘ │ └─────────────┘  └─────────────┘      │
└───────────────────────────────────┴───────────────────────────────────────┘
```

### 3.2 数据流图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              消息处理流程                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  用户消息                                                                    │
│      │                                                                       │
│      ▼                                                                       │
│  ┌─────────────┐                                                            │
│  │   Channel   │  Slack/Discord/Telegram/WhatsApp/...                       │
│  │   Monitor   │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Inbound   │  消息预处理、媒体处理、权限检查                             │
│  │   Handler   │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Auto      │  命令解析、指令处理、会话管理                               │
│  │   Reply     │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Agent     │  模型选择、认证配置、上下文构建                             │
│  │   Runner    │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Provider  │  DeepSeek/Claude/OpenAI/...                                │
│  │   API       │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Stream    │  流式响应、工具调用、内容块处理                             │
│  │   Handler   │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Delivery  │  消息分块、格式转换、发送策略                               │
│  │   Pipeline  │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Channel   │  发送响应到用户                                             │
│  │   Send      │                                                            │
│  └─────────────┘                                                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. 后端模块详解

### 4.1 Gateway 模块

#### 4.1.1 核心类

```typescript
// src/gateway/server.ts

/**
 * Gateway 服务器类
 * 负责 WebSocket 连接管理、消息路由、认证授权
 */
export class GatewayServer {
  // WebSocket 服务器实例
  private wss: WebSocketServer;
  
  // 已连接的客户端映射
  private clients: Map<string, ClientConnection>;
  
  // 会话存储
  private sessions: SessionStore;
  
  // 配置管理器
  private configManager: ConfigManager;
  
  /**
   * 启动网关服务器
   * @param port 监听端口
   */
  async start(port: number): Promise<void>;
  
  /**
   * 处理 WebSocket 连接
   * @param ws WebSocket 连接
   * @param req HTTP 请求
   */
  private handleConnection(ws: WebSocket, req: IncomingMessage): void;
  
  /**
   * 处理客户端消息
   * @param clientId 客户端 ID
   * @param message 消息内容
   */
  private handleMessage(clientId: string, message: string): void;
  
  /**
   * 广播消息到所有客户端
   * @param event 事件名称
   * @param payload 消息负载
   */
  broadcast(event: string, payload: unknown): void;
}
```

#### 4.1.2 API 方法

| 方法 | 描述 | 权限 |
|------|------|------|
| `connect` | 建立连接并认证 | public |
| `chat.send` | 发送聊天消息 | authenticated |
| `chat.abort` | 中止当前对话 | authenticated |
| `sessions.list` | 列出会话 | authenticated |
| `sessions.get` | 获取会话详情 | authenticated |
| `node.invoke` | 调用节点命令 | node |
| `node.event` | 发送节点事件 | node |
| `config.get` | 获取配置 | authenticated |
| `config.set` | 设置配置 | admin |
| `agents.list` | 列出代理 | authenticated |
| `models.list` | 列出模型 | authenticated |

### 4.2 Agent 模块

#### 4.2.1 核心类

```typescript
// src/agents/pi-embedded.ts

/**
 * 嵌入式 PI 引擎
 * 处理 AI 对话的核心引擎
 */
export class PIEmbedded {
  // 模型配置
  private modelConfig: ModelConfig;
  
  // 认证配置
  private authProfiles: AuthProfiles;
  
  // 工具注册表
  private tools: ToolRegistry;
  
  // 记忆管理器
  private memory: MemoryManager;
  
  /**
   * 运行对话
   * @param messages 消息历史
   * @param options 运行选项
   * @returns 异步生成器，产生响应块
   */
  async *run(
    messages: Message[],
    options: RunOptions
  ): AsyncGenerator<ResponseChunk>;
  
  /**
   * 处理工具调用
   * @param toolCall 工具调用请求
   * @returns 工具执行结果
   */
  private async handleToolCall(toolCall: ToolCall): Promise<ToolResult>;
  
  /**
   * 构建系统提示
   * @param context 上下文信息
   * @returns 系统提示字符串
   */
  private buildSystemPrompt(context: Context): string;
}
```

#### 4.2.2 模型配置

```typescript
// src/agents/models-config.ts

/**
 * 模型配置管理器
 * 管理所有 AI 模型的配置和认证
 */
export class ModelsConfig {
  // 提供商配置
  private providers: Map<string, ProviderConfig>;
  
  // 模型目录
  private catalog: ModelCatalog;
  
  /**
   * 获取模型配置
   * @param modelId 模型 ID
   * @returns 模型配置
   */
  getModelConfig(modelId: string): ModelConfig;
  
  /**
   * 获取认证配置
   * @param provider 提供商名称
   * @returns 认证配置
   */
  getAuthProvider(provider: string): AuthConfig;
  
  /**
   * 刷新模型列表
   */
  async refreshModels(): Promise<void>;
}
```

### 4.3 Channels 模块

#### 4.3.1 Slack 通道

```typescript
// src/slack/monitor.ts

/**
 * Slack 消息监控器
 * 监听和处理 Slack 消息
 */
export class SlackMonitor {
  // Slack 客户端
  private client: WebClient;
  
  // 消息处理器
  private messageHandler: MessageHandler;
  
  /**
   * 启动监控
   */
  async start(): Promise<void>;
  
  /**
   * 处理消息事件
   * @param event 消息事件
   */
  private async handleMessageEvent(event: SlackEvent): Promise<void>;
  
  /**
   * 发送消息
   * @param channel 频道 ID
   * @param message 消息内容
   */
  async sendMessage(channel: string, message: string): Promise<void>;
}
```

#### 4.3.2 Discord 通道

```typescript
// src/discord/monitor.ts

/**
 * Discord 消息监控器
 * 监听和处理 Discord 消息
 */
export class DiscordMonitor {
  // Discord 客户端
  private client: Client;
  
  /**
   * 启动监控
   */
  async start(): Promise<void>;
  
  /**
   * 处理消息创建事件
   * @param message 消息对象
   */
  private async handleMessageCreate(message: Message): Promise<void>;
}
```

#### 4.3.3 Telegram 通道

```typescript
// src/telegram/bot.ts

/**
 * Telegram Bot 管理器
 * 管理 Telegram Bot 的消息收发
 */
export class TelegramBot {
  // Bot 实例
  private bot: Telegraf;
  
  /**
   * 启动 Bot
   */
  async start(): Promise<void>;
  
  /**
   * 处理文本消息
   * @param ctx 上下文
   */
  private async handleTextMessage(ctx: Context): Promise<void>;
  
  /**
   * 发送消息
   * @param chatId 聊天 ID
   * @param text 消息文本
   */
  async sendMessage(chatId: number, text: string): Promise<void>;
}
```

### 4.4 Tools 模块

#### 4.4.1 工具基类

```typescript
// src/agents/tools/common.ts

/**
 * 工具基类
 * 所有工具的基类定义
 */
export abstract class BaseTool {
  // 工具名称
  abstract name: string;
  
  // 工具描述
  abstract description: string;
  
  // 参数 Schema
  abstract parameters: JSONSchema;
  
  /**
   * 执行工具
   * @param params 参数
   * @param context 执行上下文
   * @returns 执行结果
   */
  abstract async execute(
    params: Record<string, unknown>,
    context: ToolContext
  ): Promise<ToolResult>;
}
```

#### 4.4.2 Web Fetch 工具

```typescript
// src/agents/tools/web-fetch.ts

/**
 * 网页抓取工具
 * 抓取网页内容并提取文本
 */
export class WebFetchTool extends BaseTool {
  name = 'web_fetch';
  description = '抓取网页内容';
  
  /**
   * 执行网页抓取
   * @param params 包含 url 的参数
   * @returns 网页内容
   */
  async execute(params: { url: string }): Promise<ToolResult> {
    // 1. 验证 URL
    // 2. 发送 HTTP 请求
    // 3. 解析 HTML
    // 4. 提取文本内容
    // 5. 返回结果
  }
}
```

### 4.5 Memory 模块

```typescript
// src/memory/manager.ts

/**
 * 记忆管理器
 * 管理向量存储和语义搜索
 */
export class MemoryManager {
  // 向量存储
  private store: VectorStore;
  
  // 嵌入模型
  private embeddings: Embeddings;
  
  /**
   * 添加记忆
   * @param content 记忆内容
   * @param metadata 元数据
   */
  async addMemory(content: string, metadata: Record<string, unknown>): Promise<void>;
  
  /**
   * 搜索记忆
   * @param query 查询文本
   * @param limit 返回数量
   * @returns 相关记忆列表
   */
  async searchMemory(query: string, limit: number): Promise<Memory[]>;
  
  /**
   * 删除记忆
   * @param id 记忆 ID
   */
  async deleteMemory(id: string): Promise<void>;
}
```

---

## 5. 安卓应用模块详解

### 5.1 应用架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Android Application                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                           UI Layer (Jetpack Compose)                 │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │   │
│  │  │  RootScreen  │  │  ChatSheet   │  │SettingsSheet │               │   │
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

### 5.2 核心类详解

#### 5.2.1 NodeRuntime

```kotlin
// apps/android/app/src/main/java/ai/openclaw/android/NodeRuntime.kt

/**
 * 节点运行时
 * 管理安卓节点的核心运行时环境
 * 
 * 职责：
 * - 管理 Gateway 连接
 * - 协调各个功能模块
 * - 处理节点命令
 * - 管理生命周期
 */
class NodeRuntime(context: Context) {
    // 应用上下文
    private val appContext = context.applicationContext
    
    // 协程作用域
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 安全偏好设置
    val prefs = SecurePrefs(appContext)
    
    // Canvas 控制器
    val canvas = CanvasController()
    
    // 相机管理器
    val camera = CameraCaptureManager(appContext)
    
    // 位置管理器
    val location = LocationCaptureManager(appContext)
    
    // 屏幕录制管理器
    val screenRecorder = ScreenRecordManager(appContext)
    
    // SMS 管理器
    val sms = SmsManager(appContext)
    
    // Gateway 会话
    private val operatorSession = GatewaySession(...)
    private val nodeSession = GatewaySession(...)
    
    // 聊天控制器
    private val chat = ChatController(...)
    
    // 语音唤醒管理器
    private val voiceWake = VoiceWakeManager(...)
    
    // 对话模式管理器
    private val talkMode = TalkModeManager(...)
    
    /**
     * 连接到 Gateway
     * @param endpoint Gateway 端点
     */
    fun connect(endpoint: GatewayEndpoint) {
        // 1. 解析 TLS 参数
        // 2. 建立连接
        // 3. 进行认证
        // 4. 启动心跳
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        // 1. 关闭会话
        // 2. 清理资源
        // 3. 更新状态
    }
}
```

#### 5.2.2 GatewaySession

```kotlin
// apps/android/app/src/main/java/ai/openclaw/android/gateway/GatewaySession.kt

/**
 * Gateway 会话
 * 管理 WebSocket 连接和消息传输
 * 
 * 职责：
 * - WebSocket 连接管理
 * - 设备认证
 * - 请求/响应模式通信
 * - 事件订阅和分发
 */
class GatewaySession(
    private val scope: CoroutineScope,
    private val identityStore: DeviceIdentityStore,
    private val deviceAuthStore: DeviceAuthStore,
    private val onConnected: (serverName: String?, remoteAddress: String?, mainSessionKey: String?) -> Unit,
    private val onDisconnected: (message: String) -> Unit,
    private val onEvent: (event: String, payloadJson: String?) -> Unit,
) {
    // WebSocket 客户端
    private var socket: WebSocket? = null
    
    // 待处理的请求
    private val pending = ConcurrentHashMap<String, CompletableDeferred<RpcResponse>>()
    
    // 连接状态
    private val isClosed = AtomicBoolean(true)
    
    // 连接延迟
    private val connectDeferred = CompletableDeferred<Unit>()
    
    /**
     * 连接到 Gateway
     * @param endpoint Gateway 端点
     * @param token 认证令牌
     * @param password 密码
     * @param options 连接选项
     * @param tls TLS 参数
     */
    fun connect(
        endpoint: GatewayEndpoint,
        token: String?,
        password: String?,
        options: GatewayConnectOptions,
        tls: GatewayTlsParams? = null,
    ) {
        // 1. 构建 OkHttpClient
        // 2. 创建 WebSocket 连接
        // 3. 等待连接挑战
        // 4. 发送认证请求
        // 5. 处理认证响应
    }
    
    /**
     * 发送请求
     * @param method 方法名
     * @param paramsJson 参数 JSON
     * @param timeoutMs 超时时间
     * @returns 响应 JSON
     */
    suspend fun request(
        method: String,
        paramsJson: String?,
        timeoutMs: Long = 15_000
    ): String {
        // 1. 生成请求 ID
        // 2. 创建请求帧
        // 3. 发送请求
        // 4. 等待响应
        // 5. 返回结果
    }
    
    /**
     * 发送节点事件
     * @param event 事件名
     * @param payloadJson 负载 JSON
     */
    suspend fun sendNodeEvent(event: String, payloadJson: String) {
        // 构建事件帧并发送
    }
}
```

#### 5.2.3 CameraHandler

```kotlin
// apps/android/app/src/main/java/ai/openclaw/android/node/CameraHandler.kt

/**
 * 相机处理器
 * 处理相机相关命令
 * 
 * 支持的命令：
 * - camera.capture: 拍照
 * - camera.startVideo: 开始录像
 * - camera.stopVideo: 停止录像
 */
class CameraHandler(
    private val appContext: Context,
    private val camera: CameraCaptureManager,
    private val prefs: SecurePrefs,
    private val connectedEndpoint: () -> GatewayEndpoint?,
    private val externalAudioCaptureActive: MutableStateFlow<Boolean>,
    private val showCameraHud: (String, CameraHudKind, Long?) -> Unit,
    private val triggerCameraFlash: () -> Unit,
    private val invokeErrorFromThrowable: (Throwable) -> InvokeResult,
) {
    /**
     * 处理相机命令
     * @param command 命令名
     * @param paramsJson 参数 JSON
     * @returns 执行结果
     */
    suspend fun handleCommand(command: String, paramsJson: String?): InvokeResult {
        return when (command) {
            "camera.capture" -> handleCapture(paramsJson)
            "camera.startVideo" -> handleStartVideo(paramsJson)
            "camera.stopVideo" -> handleStopVideo(paramsJson)
            else -> InvokeResult.error("UNKNOWN_COMMAND", "Unknown camera command: $command")
        }
    }
    
    /**
     * 处理拍照命令
     */
    private suspend fun handleCapture(paramsJson: String?): InvokeResult {
        // 1. 解析参数
        // 2. 检查权限
        // 3. 拍照
        // 4. 上传图片
        // 5. 返回结果
    }
}
```

#### 5.2.4 ChatController

```kotlin
// apps/android/app/src/main/java/ai/openclaw/android/chat/ChatController.kt

/**
 * 聊天控制器
 * 管理聊天会话和消息发送
 * 
 * 职责：
 * - 管理聊天会话
 * - 发送和接收消息
 * - 处理流式响应
 * - 管理会话历史
 */
class ChatController(
    private val scope: CoroutineScope,
    private val session: GatewaySession,
    private val json: Json,
    private val supportsChatSubscribe: Boolean,
) {
    // 当前会话 ID
    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> = _sessionId.asStateFlow()
    
    // 消息列表
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    // 流式响应文本
    private val _streamingAssistantText = MutableStateFlow<String?>(null)
    val streamingAssistantText: StateFlow<String?> = _streamingAssistantText.asStateFlow()
    
    /**
     * 发送消息
     * @param message 消息内容
     * @param thinkingLevel 思考级别
     * @param attachments 附件列表
     */
    fun sendMessage(
        message: String,
        thinkingLevel: String,
        attachments: List<OutgoingAttachment>
    ) {
        // 1. 构建消息
        // 2. 添加到历史
        // 3. 发送到 Gateway
        // 4. 处理响应
    }
    
    /**
     * 中止当前对话
     */
    fun abort() {
        // 发送中止请求
    }
    
    /**
     * 加载会话
     * @param sessionKey 会话密钥
     */
    fun load(sessionKey: String) {
        // 加载会话历史
    }
}
```

---

## 6. 类图与流程图

### 6.1 核心类图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              核心类关系图                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐         ┌─────────────────┐                           │
│  │  GatewayServer  │────────►│    Session      │                           │
│  │                 │  1:N    │                 │                           │
│  │ - wss           │         │ - id            │                           │
│  │ - clients       │         │ - messages      │                           │
│  │ - sessions      │         │ - config        │                           │
│  └────────┬────────┘         └────────┬────────┘                           │
│           │                           │                                     │
│           │ uses                      │ has                                 │
│           ▼                           ▼                                     │
│  ┌─────────────────┐         ┌─────────────────┐                           │
│  │    AuthManager  │         │    Message      │                           │
│  │                 │         │                 │                           │
│  │ - authenticate  │         │ - role          │                           │
│  │ - authorize     │         │ - content       │                           │
│  └─────────────────┘         │ - attachments   │                           │
│                              └─────────────────┘                           │
│           │                                                                  │
│           │ creates                                                          │
│           ▼                                                                  │
│  ┌─────────────────┐         ┌─────────────────┐                           │
│  │   AgentRunner   │────────►│    Provider     │                           │
│  │                 │  uses   │                 │                           │
│  │ - run()         │         │ - call()        │                           │
│  │ - handleTool()  │         │ - stream()      │                           │
│  └────────┬────────┘         └─────────────────┘                           │
│           │                                                                  │
│           │ uses                                                             │
│           ▼                                                                  │
│  ┌─────────────────┐         ┌─────────────────┐                           │
│  │  ToolRegistry   │────────►│    BaseTool     │                           │
│  │                 │  1:N    │                 │                           │
│  │ - register()    │         │ - name          │                           │
│  │ - execute()     │         │ - execute()     │                           │
│  └─────────────────┘         └─────────────────┘                           │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 消息处理流程图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              消息处理流程                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────┐                                                                │
│  │  用户   │                                                                │
│  └────┬────┘                                                                │
│       │                                                                      │
│       │ 发送消息                                                             │
│       ▼                                                                      │
│  ┌─────────────┐                                                            │
│  │   Channel   │                                                            │
│  │   Monitor   │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         │ 1. 接收消息                                                        │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Inbound   │                                                            │
│  │   Handler   │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         │ 2. 预处理消息                                                      │
│         │    - 解析内容                                                      │
│         │    - 处理媒体                                                      │
│         │    - 检查权限                                                      │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Command   │                                                            │
│  │   Parser    │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         │ 3. 解析命令                                                        │
│         │    - 检测指令                                                      │
│         │    - 提取参数                                                      │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Session   │                                                            │
│  │   Manager   │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         │ 4. 管理会话                                                        │
│         │    - 加载历史                                                      │
│         │    - 构建上下文                                                    │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │    Agent    │                                                            │
│  │    Runner   │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         │ 5. 调用 AI 模型                                                    │
│         │    - 选择模型                                                      │
│         │    - 构建提示                                                      │
│         │    - 发送请求                                                      │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Provider  │                                                            │
│  │    API      │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         │ 6. 流式响应                                                        │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Stream    │                                                            │
│  │   Handler   │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         │ 7. 处理响应                                                        │
│         │    - 解析内容                                                      │
│         │    - 处理工具调用                                                  │
│         │    - 格式化输出                                                    │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Delivery  │                                                            │
│  │   Pipeline  │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         │ 8. 发送响应                                                        │
│         │    - 分块消息                                                      │
│         │    - 格式转换                                                      │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Channel   │                                                            │
│  │    Send     │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         │ 9. 返回给用户                                                      │
│         ▼                                                                    │
│  ┌─────────┐                                                                │
│  │  用户   │                                                                │
│  └─────────┘                                                                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.3 认证流程图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              设备认证流程                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐                                                            │
│  │   Client    │                                                            │
│  │  (Android)  │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         │ 1. 连接请求                                                        │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Gateway   │                                                            │
│  │   Server    │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         │ 2. 发送挑战 (nonce)                                                │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Client    │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         │ 3. 签名挑战                                                        │
│         │    - 使用私钥签名                                                  │
│         │    - 包含设备 ID                                                   │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Gateway   │                                                            │
│  │   Server    │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         │ 4. 验证签名                                                        │
│         │    - 使用公钥验证                                                  │
│         │    - 检查设备 ID                                                   │
│         │                                                                    │
│         │ 5. 返回会话令牌                                                    │
│         ▼                                                                    │
│  ┌─────────────┐                                                            │
│  │   Client    │                                                            │
│  │  (已认证)   │                                                            │
│  └─────────────┘                                                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. API 接口文档

### 7.1 WebSocket 帧格式

#### 请求帧

```json
{
  "type": "req",
  "id": "uuid-v4",
  "method": "method.name",
  "params": {
    // 参数对象
  }
}
```

#### 响应帧

```json
{
  "type": "res",
  "id": "uuid-v4",
  "ok": true,
  "payload": {
    // 响应数据
  }
}
```

#### 错误响应帧

```json
{
  "type": "res",
  "id": "uuid-v4",
  "ok": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Error description"
  }
}
```

#### 事件帧

```json
{
  "type": "event",
  "event": "event.name",
  "payload": {
    // 事件数据
  }
}
```

### 7.2 主要 API 方法

#### connect

建立连接并认证。

**请求参数**：
```json
{
  "minProtocol": 1,
  "maxProtocol": 1,
  "client": {
    "id": "client-id",
    "displayName": "My Device",
    "version": "1.0.0",
    "platform": "android",
    "mode": "node"
  },
  "role": "node",
  "device": {
    "id": "device-id",
    "publicKey": "base64-url-encoded-public-key",
    "signature": "base64-url-encoded-signature",
    "signedAt": 1234567890000,
    "nonce": "challenge-nonce"
  }
}
```

**响应**：
```json
{
  "ok": true,
  "payload": {
    "server": {
      "host": "server-name"
    },
    "auth": {
      "deviceToken": "device-token",
      "role": "node"
    },
    "canvasHostUrl": "https://canvas.example.com",
    "snapshot": {
      "sessionDefaults": {
        "mainSessionKey": "main"
      }
    }
  }
}
```

#### chat.send

发送聊天消息。

**请求参数**：
```json
{
  "sessionKey": "session-key",
  "message": "Hello, AI!",
  "thinking": "low",
  "attachments": [
    {
      "type": "image",
      "url": "https://example.com/image.jpg"
    }
  ]
}
```

**响应**：
```json
{
  "ok": true,
  "payload": {
    "sessionId": "session-id",
    "messageId": "message-id"
  }
}
```

#### node.invoke

调用节点命令。

**请求参数**：
```json
{
  "nodeId": "node-id",
  "command": "camera.capture",
  "params": {
    "quality": "high"
  },
  "timeoutMs": 30000
}
```

**响应**：
```json
{
  "ok": true,
  "payload": {
    "result": "success",
    "data": {
      "url": "https://example.com/photo.jpg"
    }
  }
}
```

---

## 8. 配置说明

### 8.1 配置文件结构

```
~/.openclaw/
├── config.json          # 主配置文件
├── auth.json            # 认证配置（敏感）
├── identity.json        # 设备身份
├── sessions/            # 会话存储
│   └── main/
│       └── transcript.json
├── agents/              # 代理配置
│   └── default/
│       └── config.json
├── skills/              # 技能存储
└── logs/                # 日志文件
```

### 8.2 主配置示例

```json
{
  "version": 1,
  "gateway": {
    "port": 3001,
    "host": "0.0.0.0",
    "tls": {
      "enabled": true,
      "cert": "/path/to/cert.pem",
      "key": "/path/to/key.pem"
    }
  },
  "agents": {
    "default": {
      "model": "deepseek-chat",
      "provider": "deepseek-web",
      "thinking": "low"
    }
  },
  "channels": {
    "slack": {
      "enabled": true,
      "token": "xoxb-..."
    },
    "discord": {
      "enabled": true,
      "token": "..."
    }
  },
  "memory": {
    "enabled": true,
    "provider": "sqlite",
    "embeddingModel": "text-embedding-3-small"
  }
}
```

### 8.3 环境变量

| 变量名 | 描述 | 默认值 |
|--------|------|--------|
| `OPENCLAW_HOME` | 配置目录 | `~/.openclaw` |
| `OPENCLAW_PORT` | Gateway 端口 | `3001` |
| `OPENCLAW_LOG_LEVEL` | 日志级别 | `info` |
| `OPENCLAW_MODEL` | 默认模型 | `deepseek-chat` |
| `DEEPSEEK_TOKEN` | DeepSeek Token | - |
| `ANTHROPIC_API_KEY` | Anthropic API Key | - |
| `OPENAI_API_KEY` | OpenAI API Key | - |

---

## 附录

### A. 文件结构总览

```
openclaw-zero-token/
├── src/                          # 源代码
│   ├── gateway/                  # Gateway 模块
│   ├── agents/                   # Agent 模块
│   ├── channels/                 # 通道模块
│   ├── browser/                  # 浏览器模块
│   ├── memory/                   # 记忆模块
│   ├── plugins/                  # 插件模块
│   ├── hooks/                    # 钩子模块
│   ├── cron/                     # 定时任务模块
│   ├── security/                 # 安全模块
│   ├── slack/                    # Slack 通道
│   ├── discord/                  # Discord 通道
│   ├── telegram/                 # Telegram 通道
│   ├── web/                      # WhatsApp 通道
│   ├── signal/                   # Signal 通道
│   ├── imessage/                 # iMessage 通道
│   ├── line/                     # LINE 通道
│   └── ...
├── apps/
│   ├── android/                  # 安卓应用
│   └── ios/                      # iOS 应用
├── docs/                         # 文档
├── tests/                        # 测试
└── package.json                  # 项目配置
```

### B. 命令行工具

```bash
# 启动 Gateway
openclaw gateway

# 启动 TUI
openclaw tui

# 配置向导
openclaw configure

# 添加通道
openclaw channels add slack --token xoxb-...

# 列出模型
openclaw models list

# 运行代理
openclaw agent --model deepseek-chat

# 安装技能
openclaw skills install ./my-skill
```

### C. 常见问题

**Q: 如何获取 DeepSeek 的认证凭证？**

A: 运行 `openclaw configure`，选择 DeepSeek Web 认证，按照提示在浏览器中登录，系统会自动捕获凭证。

**Q: 如何添加新的通道？**

A: 使用 `openclaw channels add <channel>` 命令，按照提示配置认证信息。

**Q: 如何创建自定义技能？**

A: 在 `~/.openclaw/skills/` 目录下创建技能目录，包含 `SKILL.md` 文件定义技能描述和实现。

---

*文档版本: 1.0.0*
*最后更新: 2026-02-25*
