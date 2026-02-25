# OpenClaw Android - 免 Token AI 客户端

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.01.00-blue.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

**一个功能完整的 Android AI 聊天客户端，支持免 Token 访问 DeepSeek、Claude、豆包等 AI 平台**

[功能特性](#功能特性) • [快速开始](#快速开始) • [架构设计](#架构设计) • [使用指南](#使用指南)

</div>

---

## 📖 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [架构设计](#架构设计)
- [使用指南](#使用指南)
- [支持的模型](#支持的模型)
- [项目结构](#项目结构)
- [开发指南](#开发指南)
- [常见问题](#常见问题)
- [致谢](#致谢)
- [许可证](#许可证)

---

## ✨ 功能特性

### 核心功能

| 功能 | 描述 | 状态 |
|------|------|------|
| 🔐 **WebView 登录** | 在 App 内直接登录，自动捕获凭证 | ✅ |
| 💬 **流式聊天** | 实时显示 AI 回复，支持中断 | ✅ |
| 🔄 **多轮对话** | 完整的会话管理和历史记录 | ✅ |
| 🤖 **多模型支持** | DeepSeek R1、Claude 3.5、豆包等 | ✅ |
| 🧠 **深度思考** | 支持 DeepSeek R1 推理模式 | ✅ |
| 🌐 **联网搜索** | 获取最新信息的搜索模式 | ✅ |
| ⚙️ **丰富设置** | 温度调节、功能开关等 | ✅ |
| 📱 **Material 3** | 现代化 UI 设计 | ✅ |

### 支持的 AI 平台

| 平台 | 模型数量 | 特殊功能 |
|------|----------|----------|
| **DeepSeek** | 4+ | R1 深度思考、联网搜索 |
| **Claude** | 3+ | 视觉理解、长上下文 |
| **豆包** | 1+ | 中文优化 |

---

## 🛠 技术栈

### 核心技术

| 技术 | 用途 |
|------|------|
| **Kotlin** | 主要开发语言 |
| **Jetpack Compose** | UI 框架 |
| **Coroutines + Flow** | 异步处理 |
| **Hilt** | 依赖注入 |
| **OkHttp + SSE** | 网络请求 |
| **DataStore** | 数据存储 |
| **Kotlinx Serialization** | JSON 序列化 |

### 架构模式

```
┌─────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │  Compose UI │  │  ViewModel  │  │  ViewModel  │             │
│  │   Screens   │──│   State     │──│   Events    │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                          Domain Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   UseCase   │  │  Repository │  │    Model    │             │
│  │  Business   │──│  Interface  │──│   Entity    │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                           Data Layer                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │  Repository │  │   Remote    │  │    Local    │             │
│  │    Impl     │──│  DataSource │──│  DataSource │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.0+

### 安装步骤

1. **克隆项目**

```bash
git clone https://github.com/UnkownWorld/testfortk1.git
cd testfortk1/apps/android
```

2. **打开项目**

使用 Android Studio 打开 `apps/android` 目录

3. **同步依赖**

点击 "Sync Project with Gradle Files"

4. **运行应用**

点击 Run 按钮或使用快捷键 `Shift + F10`

### 首次使用

1. 打开应用后，选择要登录的平台
2. 在 WebView 中完成登录
3. 登录成功后自动返回，开始对话

---

## 🏗 架构设计

### 分层架构

项目采用 **Clean Architecture** 分层设计：

```
domain/                          # 领域层（纯 Kotlin）
├── model/                       # 实体类
│   ├── AIModel.kt              # AI 模型定义
│   ├── AuthConfig.kt           # 认证配置
│   ├── ChatChunk.kt            # 响应块
│   ├── ChatException.kt        # 异常定义
│   ├── ChatMessage.kt          # 聊天消息
│   ├── Provider.kt             # 提供商枚举
│   └── Session.kt              # 会话
├── repository/                  # 仓库接口
│   ├── IAuthProvider.kt        # 认证接口
│   ├── IChatProvider.kt        # 聊天接口
│   └── ISessionRepository.kt   # 会话接口
└── usecase/                     # 用例
    ├── AuthUseCase.kt          # 认证用例
    ├── ChatUseCase.kt          # 聊天用例
    └── SessionUseCase.kt       # 会话用例

data/                            # 数据层
├── local/                       # 本地数据源
│   ├── AuthLocalDataSource.kt  # 认证存储
│   └── SessionLocalDataSource.kt
├── remote/                      # 远程数据源
│   ├── AuthRemoteDataSource.kt # 认证远程
│   ├── chat/                    # 聊天提供者
│   │   ├── DeepSeekChatProvider.kt
│   │   ├── ClaudeChatProvider.kt
│   │   └── DoubaoChatProvider.kt
│   ├── cloudflare/              # Cloudflare 绕过
│   └── pow/                     # PoW 挑战
└── repository/                  # 仓库实现
    ├── AuthProviderImpl.kt
    └── SessionRepositoryImpl.kt

ui/                              # 表现层
├── screens/                     # 页面
│   ├── auth/                    # 认证页面
│   ├── chat/                    # 聊天页面
│   ├── sessions/                # 会话历史
│   └── settings/                # 设置页面
└── viewmodel/                   # ViewModel
    ├── AuthViewModel.kt
    ├── ChatViewModel.kt
    ├── SessionsViewModel.kt
    └── SettingsViewModel.kt
```

### 数据流

```
用户操作 → ViewModel → UseCase → Repository → DataSource
                                                    │
                                                    ▼
                                              API/本地存储
                                                    │
                                                    ▼
UI 更新 ← ViewModel ← UseCase ← Repository ←─────┘
```

---

## 📚 使用指南

### 登录认证

#### 方式一：WebView 登录（推荐）

1. 打开应用，进入登录页面
2. 选择要登录的平台（DeepSeek/Claude/豆包）
3. 在 WebView 中完成登录（扫码或密码）
4. 登录成功后自动捕获凭证并返回

#### 凭证存储

- 凭证存储在本地 DataStore 中
- 默认有效期：7 天
- 支持自动刷新

### 聊天功能

#### 基本对话

```
┌─────────────────────────────────────┐
│  你: 什么是人工智能？                │
│                                     │
│  AI: 人工智能（Artificial           │
│  Intelligence，简称AI）是...        │
└─────────────────────────────────────┘
```

#### 深度思考模式（R1）

启用后，AI 会展示思考过程：

```
┌─────────────────────────────────────┐
│  💭 思考中...                        │
│  首先，我需要分析问题的核心...       │
│  然后考虑可能的解决方案...           │
│                                     │
│  AI: 经过深思熟虑，我认为...         │
└─────────────────────────────────────┘
```

#### 联网搜索模式

启用后，AI 会搜索最新信息：

```
┌─────────────────────────────────────┐
│  🔍 搜索中...                        │
│  正在搜索: 2024年最新AI发展          │
│                                     │
│  AI: 根据最新搜索结果...             │
└─────────────────────────────────────┘
```

### 设置选项

| 设置项 | 说明 | 默认值 |
|--------|------|--------|
| 温度 | 控制回复随机性 (0-2) | 0.7 |
| 深度思考 | 启用 R1 推理模式 | 关闭 |
| 联网搜索 | 启用联网搜索 | 关闭 |
| 流式输出 | 实时显示回复 | 开启 |

---

## 🤖 支持的模型

### DeepSeek

| 模型 ID | 显示名称 | 特性 |
|---------|----------|------|
| `deepseek-chat` | DeepSeek Chat | 通用对话，64K 上下文 |
| `deepseek-reasoner` | DeepSeek R1 | 深度思考，复杂推理 |
| `deepseek-search` | DeepSeek 搜索 | 联网搜索，获取最新信息 |
| `deepseek-r1-search` | DeepSeek R1 搜索 | 深度思考 + 联网搜索 |

### Claude

| 模型 ID | 显示名称 | 特性 |
|---------|----------|------|
| `claude-3-5-sonnet-20241022` | Claude 3.5 Sonnet | 最新模型，200K 上下文 |
| `claude-3-opus-20240229` | Claude 3 Opus | 最强性能 |
| `claude-3-haiku-20240307` | Claude 3 Haiku | 快速响应 |

### 豆包

| 模型 ID | 显示名称 | 特性 |
|---------|----------|------|
| `doubao-pro-32k` | 豆包 Pro | 主力模型，32K 上下文 |

---

## 📁 项目结构

```
apps/android/app/src/main/java/ai/openclaw/android/
│
├── MainActivity.kt              # 主 Activity
├── OpenClawApplication.kt       # Application 类
│
├── data/                        # 数据层
│   ├── local/                   # 本地存储
│   ├── remote/                  # 网络请求
│   └── repository/              # 仓库实现
│
├── di/                          # 依赖注入
│   ├── DataModule.kt
│   ├── NetworkModule.kt
│   ├── RepositoryModule.kt
│   └── ViewModelModule.kt
│
├── domain/                      # 领域层
│   ├── model/                   # 实体类
│   ├── repository/              # 仓库接口
│   └── usecase/                 # 用例
│
└── ui/                          # 表现层
    ├── screens/                 # 页面
    └── viewmodel/               # ViewModel
```

---

## 🔧 开发指南

### 添加新的 AI 提供商

1. **创建模型定义**

在 `domain/model/Provider.kt` 中添加：

```kotlin
NEW_PROVIDER(
    id = "new_provider",
    displayName = "新提供商",
    loginUrl = "https://example.com/login",
    apiBaseUrl = "https://example.com/api"
)
```

2. **实现聊天提供者**

创建 `data/remote/chat/NewProviderChatProvider.kt`：

```kotlin
class NewProviderChatProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) : IChatProvider {
    override val provider = Provider.NEW_PROVIDER
    
    override fun chat(...): Flow<ChatChunk> {
        // 实现聊天逻辑
    }
}
```

3. **注册到工厂**

在 `ChatProviderFactory.kt` 中注册：

```kotlin
init {
    registerProvider(newProviderChatProvider)
}
```

### 添加新的功能特性

1. 在 `domain/model/AIModel.kt` 中定义特性：

```kotlin
enum class ModelFeature {
    // 添加新特性
    NEW_FEATURE
}
```

2. 在 `ChatOptions` 中添加配置：

```kotlin
data class ChatOptions(
    // 添加新配置
    val enableNewFeature: Boolean = false
)
```

3. 在 `ChatViewModel` 中添加状态管理

4. 在 UI 中添加开关

---

## ❓ 常见问题

### Q: 登录后凭证多久过期？

A: 默认 7 天。可以在 `AuthConfig.kt` 中修改 `DEFAULT_EXPIRATION_MS`。

### Q: 为什么 DeepSeek 需要 PoW？

A: DeepSeek 使用 Proof of Work 防止滥用，需要计算 SHA3-256 哈希。

### Q: 如何处理 Cloudflare 保护？

A: 使用 WebView 登录会自动获取有效的 Cookie，绕过 Cloudflare 检测。

### Q: 支持离线使用吗？

A: 不支持，所有 AI 功能需要网络连接。

### Q: 数据存储在哪里？

A: 所有数据存储在本地 DataStore 中，不会上传到服务器。

---

## 🙏 致谢

本项目参考了以下开源项目：

- [LLM-Red-Team/deepseek-free-api](https://github.com/LLM-Red-Team/deepseek-free-api) - DeepSeek API 逆向
- [xtekky/deepseek4free](https://github.com/xtekky/deepseek4free) - PoW 挑战实现
- [Amm1rr/WebAI-to-API](https://github.com/Amm1rr/WebAI-to-API) - 多平台支持

---

## 📄 许可证

```
MIT License

Copyright (c) 2025 OpenClaw

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## ⚠️ 免责声明

本项目仅供学习和研究使用，请勿用于商业用途。

使用本项目时，请遵守相关 AI 平台的用户协议和服务条款。

---

<div align="center">

**如果这个项目对你有帮助，请给一个 ⭐ Star！**

Made with ❤️ by OpenClaw Team

</div>
