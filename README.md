

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

English | [简体中文](README_zh-CN.md)

---

## Overview

OpenClaw Zero Token is a fork of [OpenClaw](https://github.com/openclaw/openclaw) with a core mission: **eliminate API token costs** by capturing session credentials through browser automation, enabling free access to major AI platforms.

### Why Zero Token?

| Traditional Approach | Zero Token Approach |
|---------------------|---------------------|
| Requires purchasing API tokens | **Completely free** |
| Pay per API call | No usage limits |
| Credit card binding required | Only web login needed |
| Potential token leakage | Credentials stored locally |

### Supported Platforms

| Platform | Status | Models |
|----------|--------|--------|
| DeepSeek | ✅ **Currently Supported** | deepseek-chat, deepseek-reasoner |
| Doubao (豆包) | ✅ **Currently Supported** | doubao (via doubao-free-api) |
| Claude Web | ✅ **Currently Supported** | claude-3-5-sonnet-20241022, claude-3-opus-20240229, claude-3-haiku-20240307 |
| ChatGPT Web | 🔜 Coming Soon | - |

> **Note:** Doubao requires [doubao-free-api](https://github.com/linuxhsj/doubao-free-api) proxy. See "Doubao Implementation & Deployment" below for details.

---

## System Architecture

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

---

## How It Works

### Zero Token Authentication Flow

```
┌────────────────────────────────────────────────────────────────────────────┐
│                     DeepSeek Web Authentication Flow                        │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. Launch Browser                                                          │
│     ┌─────────────┐                                                        │
│     │ openclaw    │ ──start──▶ Chrome (CDP Port: 18892)                    │
│     │ gateway     │             with user data directory                   │
│     └─────────────┘                                                        │
│                                                                             │
│  2. User Login                                                              │
│     ┌─────────────┐                                                        │
│     │ User logs in│ ──visit──▶ https://chat.deepseek.com                   │
│     │  browser    │             scan QR / password login                    │
│     └─────────────┘                                                        │
│                                                                             │
│  3. Capture Credentials                                                     │
│     ┌─────────────┐                                                        │
│     │ Playwright  │ ──listen──▶ Network requests                           │
│     │ CDP Connect │              Intercept Authorization Header            │
│     └─────────────┘              Extract Cookies                            │
│                                                                             │
│  4. Store Credentials                                                       │
│     ┌─────────────┐                                                        │
│     │ auth.json   │ ◀──save── { cookie, bearer, userAgent }               │
│     └─────────────┘                                                        │
│                                                                             │
│  5. API Calls                                                               │
│     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐               │
│     │ DeepSeek    │ ──▶ │ DeepSeek    │ ──▶ │ chat.deep-  │               │
│     │ WebClient   │     │ Web API     │     │ seek.com    │               │
│     └─────────────┘     └─────────────┘     └─────────────┘               │
│         Using stored Cookie + Bearer Token                                  │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

### Key Technical Components

| Component | Implementation |
|-----------|----------------|
| **Browser Automation** | Playwright CDP connection to Chrome |
| **Credential Capture** | Network request interception, Authorization Header extraction |
| **PoW Challenge** | WASM SHA3 computation for anti-bot bypass |
| **Streaming Response** | SSE parsing + custom tag parser |

---

## Doubao Implementation & Deployment

### Overview

Doubao integration uses **web Cookie authentication** (no official API key required):

```
Browser login → Get sessionid (F12 → Application → Cookies) →
  doubao-proxy: Pass sessionid to local proxy, proxy calls Doubao API internally
  doubao-web: Direct Cookie-based requests to Doubao internal API (fallback, SSE format may change)
```

**Recommended: doubao-proxy** — Use [doubao-free-api](https://github.com/linuxhsj/doubao-free-api) for an OpenAI-compatible interface; more stable and easier to debug.

### Two Approaches Compared

| Approach | Recommended | API Endpoint | Auth | Request/Response |
|----------|-------------|--------------|------|-------------------|
| **doubao-proxy** | ★ Yes | Local `http://127.0.0.1:8000/v1/chat/completions` | Bearer Token (sessionid) | Standard OpenAI format |
| **doubao-web** | Fallback | `https://www.doubao.com/...` direct | Cookie (sessionid, ttwid, etc.) | Doubao custom SSE |

### Code Structure

```
src/
├── providers/
│   ├── doubao-web-auth.ts      # Browser login & credential capture
│   └── doubao-web-client.ts    # Doubao web API client (for doubao-web)
├── agents/
│   ├── doubao-web-stream.ts    # doubao-web streaming response parser
│   └── models-config.providers.ts  # doubao-proxy registration (api: openai-completions)
└── commands/
    ├── auth-choice.apply.doubao-proxy.ts   # doubao-proxy setup flow
    ├── auth-choice.apply.doubao-web.ts     # doubao-web setup flow
    └── onboard-auth.config-core.ts         # applyDoubaoProxyConfig etc.
```

### doubao-free-api Deployment

Use [linuxhsj/doubao-free-api](https://github.com/linuxhsj/doubao-free-api). Supports text-to-image, image-to-image, image understanding, etc.

#### Get sessionid

1. Open [https://www.doubao.com](https://www.doubao.com) and log in
2. Press F12 → Application → Cookies
3. Copy the `sessionid` value

#### Native Deployment (Recommended)

```bash
git clone https://github.com/linuxhsj/doubao-free-api.git
cd doubao-free-api
npm i
npm run build
npm start   # or: pm2 start dist/index.js --name doubao-free-api
```

#### Docker Deployment

```bash
docker run -it -d --init --name doubao-free-api -p 8000:8000 \
  -e TZ=Asia/Shanghai linuxhsj/doubao-free-api:latest

docker logs -f doubao-free-api
```

#### Docker Compose

```yaml
version: '3'
services:
  doubao-free-api:
    container_name: doubao-free-api
    image: linuxhsj/doubao-free-api:latest
    restart: always
    ports:
      - "8000:8000"
    environment:
      - TZ=Asia/Shanghai
```

#### OpenClaw Configuration

1. Run `node openclaw.mjs onboard`, select **Doubao** → **doubao-proxy**
2. Default baseUrl: `http://127.0.0.1:8000/v1` (change if proxy runs elsewhere)
3. Paste sessionid to finish setup

#### Verification

```bash
curl -N -X POST "http://127.0.0.1:8000/v1/chat/completions" \
  -H "Authorization: Bearer <sessionid>" \
  -H "Content-Type: application/json" \
  -d '{"model":"doubao","messages":[{"role":"user","content":"Hello"}],"stream":true}'
```

If SSE stream is returned, the proxy is working.

### Auth & Config Storage

| Location | Description |
|----------|-------------|
| `auth-profiles.json` | `doubao-proxy:default` → `key` is sessionid |
| `openclaw.json` | `models.providers["doubao-proxy"].baseUrl`, `agents.defaults.model.primary` |
| Env var | Optional `DOUBAO_PROXY_SESSIONID` |

### Notes

- **sessionid expiry**: Doubao sessions expire; re-login and update sessionid when needed
- **Multi-account**: doubao-free-api supports `Authorization: Bearer sessionid1,sessionid2`
- **Port**: Default 8000; ensure firewall/security group allows it
- **Compliance**: Reverse API for personal use only; use [Volcengine official API](https://www.volcengine.com/product/doubao) for commercial use

---

## Quick Start

### Requirements

- Node.js >= 22.12.0
- pnpm >= 9.0.0
- Chrome Browser

### Script Overview

This project provides several helper scripts for different use cases:

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Script Relationships                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  First Time Setup:                                                  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ run.sh (One-click setup: build + configure + start)          │  │
│  │    │                                                          │  │
│  │    ├─→ pnpm build              # Compile project             │  │
│  │    │                                                          │  │
│  │    ├─→ onboard.sh               # Configuration wizard       │  │
│  │    │       └─→ Select AI provider (DeepSeek/Doubao/Claude)  │  │
│  │    │           Configure authentication                      │  │
│  │    │           Save to .openclaw-state/openclaw.json         │  │
│  │    │                                                          │  │
│  │    └─→ server.sh start          # Start Gateway (port 3001)  │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  Testing Claude Web:                                                │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ test-all.sh (Test Claude Web functionality)                  │  │
│  │    │                                                          │  │
│  │    ├─→ start-chrome-debug.sh   # Start Chrome debug mode    │  │
│  │    ├─→ test-chrome-connection.sh # Verify connection        │  │
│  │    ├─→ server.sh stop           # Stop Gateway              │  │
│  │    ├─→ Start Gateway            # Restart Gateway           │  │
│  │    └─→ test-claude.sh           # Test Claude API           │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  Daily Usage:                                                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ server.sh [start|stop|restart|status]                        │  │
│  │    └─→ Manage Gateway service                                │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**Script Comparison:**

| Script | Purpose | When to Use | Requires Build |
|--------|---------|-------------|----------------|
| `run.sh` | Build + Configure + Start | First time setup | ✅ Auto-builds |
| `onboard.sh` | Configuration wizard | Initial config or reconfigure | ❌ Build first |
| `test-all.sh` | Test Claude Web | Verify Claude functionality | ❌ Build & configure first |
| `server.sh` | Manage Gateway service | Daily start/stop/restart | ❌ Build & configure first |

### Installation

```bash
# Clone the repository
git clone https://github.com/linuxhsj/openclaw-zero-token.git
cd openclaw-zero-token

# Install dependencies
pnpm install
```

### Option 1: One-Click Setup (Recommended for First Time)

```bash
# This will: build + configure + start
./run.sh
```

The script will:
1. Compile the project (`pnpm build`)
2. Run configuration wizard (`onboard.sh`)
3. Start Gateway service (`server.sh start`)

### Option 2: Step-by-Step Setup

#### Step 1: Build

```bash
pnpm build
```

#### Step 2: Configure Authentication

```bash
# Run setup wizard
./onboard.sh

# Or use the compiled version
node openclaw.mjs onboard

# Select authentication method
? Auth provider: DeepSeek (Browser Login)

# Choose login mode
? DeepSeek Auth Mode: 
  > Automated Login (Recommended)  # Auto-capture credentials
    Manual Paste                   # Manually paste credentials
```

#### Step 3: Start Gateway

```bash
# Using helper script (recommended)
./server.sh start

# Or directly
node openclaw.mjs gateway

# Access Web UI
open http://127.0.0.1:3001
```

---

## Usage

### Web UI

Visit `http://127.0.0.1:3001` and start chatting with DeepSeek models directly.

### API Calls

```bash
# Call via Gateway Token
curl http://127.0.0.1:3001/v1/chat/completions \
  -H "Authorization: Bearer YOUR_GATEWAY_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "deepseek-web/deepseek-chat",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'
```

### CLI Mode

```bash
# Interactive terminal
node openclaw.mjs tui
```

---

## Claude Web Usage

> **Note:** Before testing Claude Web, make sure you have completed the initial setup using `run.sh` or `onboard.sh` to configure Claude Web authentication. See [Script Overview](#script-overview) for the relationship between different scripts.

### Quick Start (One-Click Test)

```bash
# One-click test script (recommended)
./test-all.sh

# Features:
# - Automatically starts Chrome in debug mode
# - Opens Claude.ai and waits for login
# - Tests connection and API
# - Opens Web UI automatically
```

**What test-all.sh does:** See the [Script Flow](#testing-scripts) in the Testing Scripts section below.

### Manual Setup

#### Step 1: Start Chrome Debug Mode

```bash
# Start Chrome with remote debugging
./start-chrome-debug.sh

# Chrome will open with:
# - Debug port: 9222
# - Separate user profile (won't affect your daily Chrome)
# - Auto-navigate to https://claude.ai/new
```

#### Step 2: Login to Claude

1. Wait for Claude.ai to load in the opened Chrome window
2. Login with your Claude account (should auto-login if previously logged in)
3. Keep this Chrome window open

#### Step 3: Start Gateway

```bash
# Start the gateway server
./server.sh start

# Or manually:
node dist/index.mjs gateway
```

#### Step 4: Test

```bash
# Test via CLI
./test-claude.sh "Hello, Claude!"

# Or open Web UI
# Browser: http://127.0.0.1:3001/#token=62b791625fa441be036acd3c206b7e14e2bb13c803355823
```

### How It Works

**Architecture:**
```
User Request
    ↓
OpenClaw Gateway (Port 3001)
    ↓
ClaudeWebClientBrowser (Playwright)
    ↓
Chrome Debug Mode (Port 9222)
    ↓
Claude.ai API (Browser Context)
    ↓
Response (SSE Stream)
```

**Key Features:**
- ✅ **Cloudflare Bypass**: Requests sent in real browser context
- ✅ **Cookie Authentication**: Uses browser's session cookies
- ✅ **No API Token**: Completely free, no credit card required
- ✅ **Streaming Support**: Real-time response streaming
- ✅ **Separate Instance**: Independent Chrome profile, won't affect daily usage

### Configuration

The configuration is stored in `.openclaw-state/openclaw.json`:

```json
{
  "browser": {
    "attachOnly": true,
    "defaultProfile": "my-chrome",
    "profiles": {
      "my-chrome": {
        "cdpUrl": "http://127.0.0.1:9222",
        "color": "#4285F4"
      }
    }
  },
  "models": {
    "providers": {
      "claude-web": {
        "baseUrl": "https://claude.ai",
        "api": "claude-web",
        "models": [
          {
            "id": "claude-3-5-sonnet-20241022",
            "name": "Claude 3.5 Sonnet (Web)"
          }
        ]
      }
    }
  }
}
```

### API Calls

```bash
# Call via Gateway Token
curl http://127.0.0.1:3001/v1/chat/completions \
  -H "Authorization: Bearer YOUR_GATEWAY_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "claude-web/claude-3-5-sonnet-20241022",
    "messages": [{"role": "user", "content": "Hello, Claude!"}]
  }'
```

### Available Models

Claude Web supports the following models with automatic ID mapping:

| Configuration ID | Claude Web API ID | Model Name | Recommended |
|------------------|-------------------|------------|-------------|
| `claude-3-5-sonnet-20241022` | `claude-sonnet-4-6` | Claude 3.5 Sonnet | ✅ Yes |
| `claude-3-opus-20240229` | `claude-opus-4-6` | Claude 3 Opus | - |
| `claude-3-haiku-20240307` | `claude-haiku-4-6` | Claude 3 Haiku | - |

**How it works:**
- You use the standard Anthropic model ID (e.g., `claude-3-5-sonnet-20241022`) in your configuration and API calls
- The system automatically converts it to Claude Web's internal format (e.g., `claude-sonnet-4-6`)
- This ensures compatibility with standard Anthropic API naming conventions

**Example:**
```bash
# You call with standard ID
curl http://127.0.0.1:3001/v1/chat/completions \
  -H "Authorization: Bearer YOUR_GATEWAY_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "claude-web/claude-3-5-sonnet-20241022",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'

# System automatically converts to: claude-sonnet-4-6
```

### Testing Scripts

```bash
# One-click test (recommended)
./test-all.sh

# Test Chrome connection
./test-chrome-connection.sh

# Test Claude API with custom message
./test-claude.sh "Your question here"

# Test with random message (avoid detection)
./test-claude.sh "$(shuf -n 1 test-messages.txt)"
```

**test-all.sh Script Flow:**

```
test-all.sh
    │
    ├─→ start-chrome-debug.sh      # Start Chrome in debug mode (port 9222)
    │
    ├─→ test-chrome-connection.sh  # Verify Chrome debug connection
    │
    ├─→ server.sh stop              # Stop existing Gateway
    │
    ├─→ Start Gateway               # Launch Gateway service
    │
    └─→ test-claude.sh              # Test Claude API with message
```

The `test-all.sh` script automates the entire testing process, making it easy to verify your Claude Web setup with a single command.

### Troubleshooting

**Chrome connection failed:**
```bash
# Check if Chrome is running
ps aux | grep "chrome.*9222"

# Restart Chrome
pkill -f "chrome.*9222"
./start-chrome-debug.sh
```

**No response from Claude:**
- Ensure Chrome window is open with Claude.ai loaded
- Check Gateway logs: `tail -50 /tmp/openclaw-gateway.log`
- Restart Gateway: `./server.sh restart`

**Model not available (403):**
- Model IDs are automatically mapped, no action needed
- If issue persists, check your Claude account subscription

### Technical Details

For detailed technical documentation, see [CLAUDE_WEB_IMPLEMENTATION.md](CLAUDE_WEB_IMPLEMENTATION.md):
- System architecture
- Cloudflare bypass principles
- SSE streaming response parsing
- Code structure and modifications

### CLI Mode

```bash
# Interactive terminal with Claude
node openclaw.mjs tui
```

---

## Configuration

### openclaw.json

```json
{
  "auth": {
    "profiles": {
      "deepseek-web:default": {
        "provider": "deepseek-web",
        "mode": "api_key"
      }
    }
  },
  "models": {
    "providers": {
      "deepseek-web": {
        "baseUrl": "https://chat.deepseek.com",
        "api": "deepseek-web",
        "models": [
          {
            "id": "deepseek-chat",
            "name": "DeepSeek Chat",
            "contextWindow": 64000,
            "maxTokens": 4096
          },
          {
            "id": "deepseek-reasoner",
            "name": "DeepSeek Reasoner",
            "reasoning": true,
            "contextWindow": 64000,
            "maxTokens": 8192
          }
        ]
      }
    }
  },
  "gateway": {
    "port": 3001,
    "auth": {
      "mode": "token",
      "token": "your-gateway-token"
    }
  }
}
```

---

## Roadmap

### Current Focus
- ✅ DeepSeek Web authentication (stable)
- ✅ Doubao via doubao-free-api
- ✅ Claude Web authentication (stable)
- 🔧 Improving credential capture reliability
- 📝 Documentation improvements

### Planned Features
- 🔜 ChatGPT Web authentication support
- 🔜 Auto-refresh for expired sessions

---

## Adding New Platforms

To add support for a new platform, create the following files:

### 1. Authentication Module (`src/providers/{platform}-web-auth.ts`)

```typescript
export async function loginPlatformWeb(params: {
  onProgress: (msg: string) => void;
  openUrl: (url: string) => Promise<boolean>;
}): Promise<{ cookie: string; bearer: string; userAgent: string }> {
  // Browser automation login, capture credentials
}
```

### 2. API Client (`src/providers/{platform}-web-client.ts`)

```typescript
export class PlatformWebClient {
  constructor(options: { cookie: string; bearer?: string }) {}
  
  async chatCompletions(params: ChatParams): Promise<ReadableStream> {
    // Call platform Web API
  }
}
```

### 3. Stream Handler (`src/agents/{platform}-web-stream.ts`)

```typescript
export function createPlatformWebStreamFn(credentials: string): StreamFn {
  // Handle platform-specific response format
}
```

---

## Project Structure

```
openclaw-zero-token/
├── src/
│   ├── providers/
│   │   ├── deepseek-web-auth.ts      # DeepSeek login capture
│   │   └── deepseek-web-client.ts    # DeepSeek API client
│   ├── agents/
│   │   └── deepseek-web-stream.ts    # Streaming response handler
│   ├── commands/
│   │   └── auth-choice.apply.deepseek-web.ts  # Authentication flow
│   └── browser/
│       └── chrome.ts                 # Chrome automation
├── ui/                               # Web UI (Lit 3.x)
├── .openclaw-state/                  # Local state (not committed)
│   ├── openclaw.json                 # Configuration
│   └── agents/main/agent/
│       └── auth.json                 # Credentials (sensitive)
└── .gitignore                        # Includes .openclaw-state/
```

---

## Security Notes

1. **Credential Storage**: Cookies and Bearer tokens are stored locally in `auth.json`, **never committed to Git**
2. **Session Expiry**: Web sessions may expire and require periodic re-login
3. **Rate Limits**: Web APIs may have rate limits, not suitable for high-frequency calls
4. **Compliance**: For personal learning and research only, please comply with platform terms of service

---

## Syncing with Upstream

This project is based on OpenClaw. Sync upstream updates with:

```bash
# Add upstream repository
git remote add upstream https://github.com/openclaw/openclaw.git

# Sync upstream updates
git fetch upstream
git merge upstream/main
```

---

## Contributing

Contributions are welcome, especially:
- New platform Web authentication support (Doubao, Claude, ChatGPT, etc.)
- Bug fixes
- Documentation improvements

---

## License

[MIT License](LICENSE)

---

## Acknowledgments

- [OpenClaw](https://github.com/openclaw/openclaw) - The original project
- [DeepSeek](https://deepseek.com) - Excellent AI models

---

## Disclaimer

This project is for learning and research purposes only. When using this project to access any third-party services, please ensure compliance with that service's terms of use. The developers are not responsible for any issues arising from the use of this project.
