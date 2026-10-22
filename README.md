---
type: repo-overview
title: AI Integration Lab
description: 面向 Java 后端开发者的 AI 集成工程学习项目。
owner: NEEDS_OWNER
status: proposed
tags: [ai-integration, learning]
---

# AI Integration Lab

以「开发者知识助手」为业务主线，构建一个可治理的模型网关，再接入知识库与受控工具。学习目标是能把不稳定的模型能力集成为可测试、可观测、可演进的应用。主线使用 Java；Python 用于数据处理与评测。无需先学训练大模型。

## 当前进度

**M0：可运行的起点。** 三模块 Maven 工程、同步聊天接口、过滤器链、逻辑模型别名、mock / Ollama 适配器、统一错误和基础指标。mock 仅用于验证工程链路，不具备模型智能。Ollama 适配器通过本地 HTTP 契约测试验证，真实模型需要你自行安装并选定。

**尚未实现：** 云模型适配器、SSE、鉴权、多租户、配额、持久化、RAG、工具调用、MCP、Agent、评测平台。它们是后续学习任务，详见[路线与验收](docs/learning/roadmap.md)。当前服务仅绑定本机回环地址。

## 快速开始

需要 JDK 17+、Maven 3.6.3+；项目固定 Spring Boot 4.0.8。`scripts/mvn.sh` 从当前 `java` 推导 `JAVA_HOME`，避免本机 Maven 与命令行 Java 使用不同 JDK；已显式设置的 `JAVA_HOME` 优先。

```bash
./scripts/mvn.sh clean verify
./scripts/mvn.sh -pl ai-launcher -am package -DskipTests
java -jar ai-launcher/target/ai-launcher-0.1.0-SNAPSHOT.jar
```

另一个终端：

```bash
./scripts/smoke.sh
curl -sS http://127.0.0.1:8080/api/chat   -H 'Content-Type: application/json'   -d '{"model":"assistant","messages":[{"role":"user","content":"如何从 Java 后端转向 AI 集成？"}]}'
```

接口是本项目的自有契约，不宣称兼容任何厂商的完整 API。逻辑别名 `assistant` 映射到启动时选定的适配器，客户端不能传任意上游模型名或 URL。当前不接收历史 `assistant` / `system` 消息，后续通过信任边界练习再扩展。

使用已运行的本地 Ollama（模型名替换成你实际安装的模型，不会自动下载）：

```bash
AI_PROVIDER=ollama OLLAMA_MODEL=your-installed-model   java -jar ai-launcher/target/ai-launcher-0.1.0-SNAPSHOT.jar
```

## 从哪里学

1. [第一课](docs/learning/first-session.md)：跑通、追踪、替换、制造故障。
2. [路线与验收](docs/learning/roadmap.md)：M0–M9 的知识点、产物和完成标准。
3. [架构与 AGW 借鉴](docs/architecture/overview.md)：为何分层、哪些思想沿用、AI 场景为何要调整。
4. [运行时模块](docs/architecture/runtime-modules.md)、[M0 规格](docs/features/m0/spec.md)、[测试策略](docs/testing/strategy.md)。
5. [决策记录](docs/adr/0001-learning-baseline.md)、[本地运行](docs/operations/local.md)、[文档导航](docs/index.md)。

每个里程碑尽量按「先画流程 → 手写最小实现 → 写故障用例 → 测量 → 与框架实现对比 → 写复盘」完成。不要让编码助手一次实现整条路线；先由自己解释边界与失败策略。

## Verification

`./scripts/mvn.sh clean verify` 检查核心行为、适配器契约及真实 HTTP 入口；启动后执行 `./scripts/smoke.sh`。尚未完成的学习目标均在路线中标记为待实现。
