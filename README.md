---
type: repo-overview
title: AI Integration Lab
description: 面向 Java 后端开发者的 AI 集成工程学习项目。
owner: NEEDS_OWNER
status: proposed
tags: [ai-integration, learning]
---

# AI Integration Lab

以 **Java AI Gateway + 模型路由平台** 为工程主线，面向 AI Backend / AI Platform，再逐步深入 LLM Infrastructure。以「Archiver Incident Copilot」作为知识库与受控工具的业务验收场景。学习目标是能把不稳定的模型能力集成为可测试、可观测、可演进的应用。主线使用 Java；Python 用于数据处理与评测。无需先学训练大模型。

## 当前进度

**M0：可运行的起点。** 三模块 Gradle 工程、Admin Portal、同步聊天接口、过滤器链、配置驱动的多模型别名路由、模型目录、mock / Ollama 适配器、统一错误、按路由调用耗时及供应商 token 计数。mock 仅用于验证工程链路，不具备模型智能。Ollama 适配器通过本地 HTTP 契约测试验证，真实模型需要你自行安装并选定。

**尚未实现：** 云模型适配器、SSE、鉴权、多租户、RPM/TPM 配额、重试/fallback、成本/延迟自动路由、计费、持久化、RAG、工具调用、MCP、Agent、评测平台。它们是后续学习任务，详见[路线与验收](docs/learning/roadmap.md)。当前服务仅绑定本机回环地址。

## 快速开始

需要 JDK 17+；项目固定 Spring Boot 4.0.8，使用 Gradle Wrapper 固定构建版本，无需全局安装 Gradle。`gradlew` 优先使用 `JAVA_HOME`，未设置时使用 PATH 中的 `java`；编译目标为 Java 17。

```bash
./gradlew clean build
java -jar ai-launcher/build/libs/ai-launcher-0.1.0-SNAPSHOT.jar
```

`build` 会运行三个模块的测试并生成可部署的 Spring Boot JAR。只打包可用 `./gradlew :ai-launcher:bootJar`（不运行测试）；开发时可用 `./gradlew :ai-launcher:bootRun` 直接启动。Windows 使用 `gradlew.bat`。

启动后访问 [Admin Portal](http://localhost:8080/)：新增、编辑、删除和切换网关连接档案，检查健康状态，测试聊天、模型目录、探针、服务信息与指标目录接口。连接档案保存在当前浏览器；请求记录仅保留在当前页面会话。模型别名及 provider 仍由服务端启动配置决定。跨域连接需要目标网关允许当前页面来源的 CORS。详情见[管理页面规格](docs/features/admin-portal/spec.md)。

另一个终端：

```bash
./scripts/smoke.sh
curl -sS http://127.0.0.1:8080/api/chat   -H 'Content-Type: application/json'   -d '{"model":"assistant","messages":[{"role":"user","content":"如何从 Java 后端转向 AI 集成？"}]}'
```

接口是本项目的自有契约，不宣称兼容任何厂商的完整 API。未配置路由表时，逻辑别名 `assistant` 映射到启动时选定的适配器；可配置多个别名映射不同模型，客户端不能传任意上游模型名或 URL。当前不接收历史 `assistant` / `system` 消息，后续通过信任边界练习再扩展。

使用已运行的本地 Ollama，可以将根目录的配置模板复制为本地配置，只需设置一次：

```bash
cp application.properties.example application.properties
java -jar ai-launcher/build/libs/ai-launcher-0.1.0-SNAPSHOT.jar
```

模板默认使用 `qwen2.5:7b` 和 120 秒后端超时；先用 `ollama pull qwen2.5:7b` 下载，或修改 `ai.ollama.model` 为已安装的模型。配置格式 `${环境变量:默认值}` 中，冒号后的内容是保存的默认值；无需设置环境变量，也可直接填写固定值。

从**项目根目录**运行时，Spring Boot 会自动读取外部 `application.properties`，覆盖 JAR 内的默认配置；`bootRun` 也使用项目根目录。修改本地配置后重启即可，无需重新打包。该文件已加入 Git 忽略，模板保留在仓库。没有本地配置时仍使用 mock；未配置显式路由列表时，临时切回 mock 可用 `AI_PROVIDER=mock java -jar ai-launcher/build/libs/ai-launcher-0.1.0-SNAPSHOT.jar`。

Portal 的浏览器请求仍为 30 秒超时，独立于此处的 Ollama 后端超时。

## 多模型路由与观测

在本地 `application.properties` 中按模板添加完整的 `ai.routing.routes` 列表并重启。同一进程可用不同别名指向多个 Ollama 模型，也可保留 mock 路由。显式列表会替换默认 `assistant` 路由；不会自动选择更便宜的模型或在失败时降级。完整规则与回滚方法见[路由规格](docs/features/model-routing/spec.md)。

```bash
curl -sS http://127.0.0.1:8080/api/models
curl -sS http://127.0.0.1:8080/actuator/metrics/ai.gateway.calls
```

先从模型目录取得别名，再填入聊天请求的 `model`。指标在调用后产生；`ai.gateway.tokens` 仅累计真实 provider 返回的 usage，mock、缺失用量与失败时未知消耗不伪造计数，也不代表费用结算。

## 从哪里学

1. [第一课](docs/learning/first-session.md)：跑通、追踪、替换、制造故障。
2. [十二周 AI Platform 计划](docs/learning/platform-plan.md)与[路线与验收](docs/learning/roadmap.md)：时间盒、M0–M9 能力和完成标准。
3. [架构与 AGW 借鉴](docs/architecture/overview.md)：为何分层、哪些思想沿用、AI 场景为何要调整。
4. [运行时模块](docs/architecture/runtime-modules.md)、[M0 规格](docs/features/m0/spec.md)、[测试策略](docs/testing/strategy.md)。
5. [Copilot 实践规格](docs/features/incident-copilot/spec.md)：合成归档故障、RAG/工具边界与评测覆盖。
6. [决策记录](docs/adr/0001-learning-baseline.md)、[本地运行](docs/operations/local.md)、[文档导航](docs/index.md)。

每个里程碑尽量按「先画流程 → 手写最小实现 → 写故障用例 → 测量 → 与框架实现对比 → 写复盘」完成。不要让编码助手一次实现整条路线；先由自己解释边界与失败策略。

## Verification

`./gradlew clean build` 检查核心行为、适配器契约及真实 HTTP 入口；启动后执行 `./scripts/smoke.sh`。尚未完成的学习目标均在路线中标记为待实现。

管理页面无需 Node.js 构建。可选使用 Node.js 18+ 运行前端逻辑测试：`node --test ai-launcher/src/test/js/portal.test.mjs`。
