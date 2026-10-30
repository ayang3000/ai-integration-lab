---
type: runbook
title: 第一课：理解一个 AI 请求
description: 通过一条同步聊天链路建立正确的模型集成认识。
owner: NEEDS_OWNER
status: proposed
tags: [ai-integration, learning]
---

# 第一课：理解一个 AI 请求

建议用 60–90 分钟完成，不急于添加依赖。

1. 按根 README 构建、启动和 smoke。保存响应，确认 `simulated=true` 与 unavailable usage；回答为什么 mock 成功不能说明模型集成质量达标。
2. 从 `ChatController` 顺着 `GatewayPipeline`、`ModelRouter` 到 `MockChatModel`，手绘请求与异常返回路径。解释为什么 provider 不能依赖 controller。
3. 请求不存在的模型别名、空消息和非法角色；观察 requestId 与状态。确认没有调用上游的必要性。
4. 阅读 `PipelineTest`，解释 around 过滤器为什么需要 `finally`、短路后哪些过滤器不该清理。
5. 可选：自己启动 Ollama，并选一个适合本机资源的模型；按 README 指定模型。比较真实结果、usage、延迟和不确定性。无需为此安装软件即可完成前四步。
6. Ollama 已配置时停止它，再请求；确认得到受控 502，响应和日志没有泄漏 prompt 或上游响应正文。
7. 自己新增一个“拒绝特定逻辑模型”的过滤器，先写短路测试，再实现；完成后提交 Git。

## 路由扩展练习

按[路由规格](../features/model-routing/spec.md)先配置两个 mock 别名，验证目录、聊天与未知别名 404；再任选一个别名切到已安装的 Ollama 模型。对比两条路由的 `simulated`、usage 和调用指标。解释为什么两个 mock 别名不等于验证了两个真实模型的能力，以及为什么路由失败不能悄悄替换成 mock 成功。

## 理解检查

- 为什么不能把字符数当 token？为何 usage 缺失时保留 unknown？
- 为什么不能像普通 GET 请求那样随意重试有费用的生成或写工具？
- 流开始输出后还能换模型吗？客户端断开为什么需要取消上游？
- RAG、工具调用与模型训练分别解决什么问题？

## Verification

`./gradlew clean build`、`./scripts/smoke.sh`，以及你能在不看源码时解释上面四个问题。此次仅创建起点，个人学习验收尚未执行。
