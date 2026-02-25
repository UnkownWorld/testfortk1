# 更新日志

所有重要的更改都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

---

## [1.0.0] - 2025-01-XX

### 新增

#### 核心功能
- ✅ WebView 登录认证，自动捕获 Cookie 和 Token
- ✅ 流式聊天响应，支持实时显示
- ✅ 多轮对话支持，完整的会话管理
- ✅ 会话历史记录，支持搜索和删除

#### AI 平台支持
- ✅ DeepSeek 支持
  - deepseek-chat 通用对话
  - deepseek-reasoner (R1) 深度思考
  - deepseek-search 联网搜索
  - deepseek-r1-search 深度思考 + 联网搜索
- ✅ Claude 支持
  - claude-3-5-sonnet 最新模型
  - claude-3-opus 最强模型
  - claude-3-haiku 快速响应
- ✅ 豆包支持
  - doubao-pro-32k 主力模型

#### 技术特性
- ✅ PoW 挑战求解器 (SHA3-256)
- ✅ Cloudflare 检测和绕过
- ✅ 完整的错误处理和重试机制
- ✅ 凭证自动刷新

#### UI 功能
- ✅ Material 3 设计风格
- ✅ 深色模式支持
- ✅ 设置页面
  - 温度调节
  - 深度思考开关
  - 联网搜索开关
  - 流式输出开关
- ✅ 模型选择对话框
- ✅ 会话历史页面

#### 架构
- ✅ Clean Architecture 分层设计
- ✅ MVVM 架构模式
- ✅ Hilt 依赖注入
- ✅ Kotlin Coroutines + Flow
- ✅ Jetpack Compose UI

---

## 技术细节

### 认证流程

```
用户点击登录
    ↓
打开 WebView
    ↓
用户在 WebView 中登录
    ↓
拦截网络请求，捕获凭证
    ↓
保存凭证到 DataStore
    ↓
返回主界面，开始对话
```

### 聊天流程

```
用户输入消息
    ↓
创建用户消息，添加到会话
    ↓
构建请求（包含 PoW 答案）
    ↓
发送 SSE 请求
    ↓
解析响应块流
    ↓
更新 UI
    ↓
保存助手消息到会话
```

---

## 已知问题

### 待解决

- [ ] Claude 认证流程需要进一步测试
- [ ] 豆包平台 API 需要验证
- [ ] 部分设备上 WebView 可能需要手动刷新 Cookie

### 限制

- 凭证默认有效期 7 天
- PoW 计算可能需要几秒钟
- 需要网络连接才能使用

---

## 路线图

### v1.1.0 (计划中)

- [ ] 图片上传支持
- [ ] 语音输入
- [ ] 导出对话
- [ ] 多语言支持

### v1.2.0 (计划中)

- [ ] 工具调用支持
- [ ] 代码高亮
- [ ] Markdown 渲染
- [ ] 自定义主题

---

## 贡献

欢迎提交 Issue 和 Pull Request！

---

## 版本说明

- **主版本号**: 重大架构变更或不兼容更新
- **次版本号**: 新功能添加
- **修订号**: Bug 修复和小改进
