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

命令见根 README。服务默认只绑定 127.0.0.1；不要直接把此学习版本暴露到公网。没有默认密码或凭据。启动参数见 `ai-launcher/src/main/resources/application.properties`。

- Maven 与 `java -version` 不一致：使用 `scripts/mvn.sh`，或给当前命令显式设置合适的 `JAVA_HOME`；无需修改全局 JDK。
- 首次构建需要访问 Maven Central；网络失败先解决下载，不跳过测试。
- 端口冲突：用 `SERVER_PORT=18080` 启动，smoke 使用 `BASE_URL=http://127.0.0.1:18080 ./scripts/smoke.sh`。
- Ollama 502：检查本机进程、实际模型名和启动配置；不要通过开启原始正文日志排障。直接在自己的终端检查本地 Ollama 状态。
- provider 配置不认识或缺少 Ollama 模型：启动失败，修正环境变量。不会自动降级并伪装成功。
- 停止：前台终端 Ctrl-C，等待优雅退出；回滚到 mock 后重启。

## 数据与密钥

M0 不持久化对话；adapter 只在你主动发起请求时发送该请求消息。mock 不出站，Ollama 默认出站到本机。后续云模型接入须明确数据目的地。`.env` 不会自动加载；`.env.example` 仅供参考，显式 export 或使用终端环境注入。不要提交真实密钥、私人对话或公司内部数据。

## 观测范围

`/actuator/health/liveness`、`/actuator/health/readiness` 是进程级探针，不包含模型健康。`/actuator/metrics` 可查看 HTTP 聚合指标；provider 的 requestId 与耗时目前通过结构化字段日志关联。OTel、TTFT、token 成本、仪表板均待 M2/M3 实现。

## Verification

本地启动后执行 `scripts/smoke.sh`；真实模型诊断按[第一课](../learning/first-session.md)手动执行。
