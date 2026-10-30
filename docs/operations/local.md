---
type: runbook
title: 本地运行与排障
description: 本地环境、常见故障、停止与回滚。
owner: NEEDS_OWNER
status: proposed
tags: [ai-integration, learning]
---

# 本地运行与排障

## 构建与启动

构建与开发启动命令见[根 README](../../README.md)。服务默认只绑定 127.0.0.1；不要直接把此学习版本暴露到公网。没有默认密码或凭据。启动参数见 `ai-launcher/src/main/resources/application.properties`。

根目录 `application.properties.example` 是 Ollama 本地配置模板；复制为 `application.properties` 后，从项目根目录运行 JAR 会自动加载，`bootRun` 也固定使用此工作目录。本地文件覆盖 JAR 内的默认配置，不需要重新编译，修改后重启生效。模板用 `${环境变量:默认值}` 保留临时环境变量覆盖。未创建本地文件时仍默认 mock；运行 `scripts/smoke.sh` 前应保证 `assistant` 指向 mock；没有显式路由列表时可用 `AI_PROVIDER=mock`，因为该脚本专门断言 mock 响应。

- Gradle 与 `java -version` 不一致：检查 `./gradlew --version`，给当前命令显式设置合适的 `JAVA_HOME`；无需修改全局 JDK。
- 首次构建需要下载 Wrapper 指定的 Gradle 发行包，并访问 Gradle Plugin Portal 和 Maven Central；网络失败先解决下载，不跳过测试。
- 端口冲突：用 `SERVER_PORT=18080` 启动，smoke 使用 `BASE_URL=http://127.0.0.1:18080 ./scripts/smoke.sh`。
- Ollama 502：检查本机进程、实际模型名和启动配置；不要通过开启原始正文日志排障。直接在自己的终端检查本地 Ollama 状态。
- provider 配置不认识或缺少 Ollama 模型：启动失败，修正环境变量。不会自动降级并伪装成功。
- 停止：前台终端 Ctrl-C，等待优雅退出；若配置过 `ai.routing.routes`，移除整个列表后再切回 mock 并重启。

访问 `/` 打开 Admin Portal。旧进程不会自动加载新构建的页面，修改后需重新启动 JAR。连接档案按浏览器来源保存，`localhost`、`127.0.0.1` 和不同端口各自独立。外部网关连接失败时检查服务地址、CORS 与浏览器的混合内容限制；控制台不会代理请求或修改服务端 CORS。

## 配置多个模型

参照根目录模板配置 `ai.routing.routes`，重启后请求 `/api/models` 确认别名。该列表完整替换默认 `assistant` 映射，不会与旧 provider 配置合并；Ollama 未指定的模型/地址/超时字段仍继承 `ai.ollama.*`。列表索引必须连续且位于同一配置来源。完整示例、指标解释及回滚见[路由规格](../features/model-routing/spec.md)。

目录可读取不表示上游已下载或加载模型；真实可用性需要主动发起聊天检查。Portal 的“模型目录”预设可查询，聊天 JSON 的 `model` 填对应 alias。

## JAR 部署

```bash
./gradlew clean build
java -jar ai-launcher/build/libs/ai-launcher-0.1.0-SNAPSHOT.jar
```

部署产物是 `ai-launcher/build/libs/` 下的可执行 JAR，已包含运行依赖；运行机器只需 Java 17+。启动后执行 `./scripts/smoke.sh` 验证 mock 链路。变更前保留上一版 JAR；回滚时停止当前进程，使用上一版 JAR 和对应环境变量重启。当前仓库未配置远程部署或容器发布流水线。

## 数据与密钥

M0 不持久化对话；adapter 只在你主动发起请求时发送该请求消息。mock 不出站，Ollama 默认出站到本机。后续云模型接入须明确数据目的地。根目录 `application.properties` 不提交 Git，避免收录个人配置；`.env` 不会自动加载，`.env.example` 仅供参考。不要提交真实密钥、私人对话或公司内部数据。

## 观测范围

`/actuator/health/liveness`、`/actuator/health/readiness` 是进程级探针，不包含模型健康。`/actuator/metrics` 可查看 HTTP 聚合指标；provider 的 requestId 与耗时目前通过结构化字段日志关联。`ai.gateway.calls` 提供按路由成功/错误/拒绝的次数与耗时，`ai.gateway.tokens` 只累计已返回的真实供应商 usage。指标随进程重启清零。OTel、TTFT、P95 分布配置、token 成本与仪表板均待 M2/M3 实现。

## Verification

本地启动后执行 `scripts/smoke.sh`；真实模型诊断按[第一课](../learning/first-session.md)手动执行。
