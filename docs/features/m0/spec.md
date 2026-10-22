---
type: feature-spec
title: M0：本地模型网关最小闭环
description: 明确当前已实现的行为和学习用边界。
owner: NEEDS_OWNER
status: proposed
risk_level: low
tags: [ai-integration, learning]
---

# M0：本地模型网关最小闭环

## 目标与范围

建立一个无需密钥可运行、能替换真实本地模型的 Java 工程，保留未来流式、治理和应用编排的扩展点。M0 不实现鉴权、持久化、重试、RAG 或工具执行。

## 行为与契约

接口由 `ChatController` DTO 定义，领域约束由 `ChatRequest` / `Message` 定义。只接受逻辑别名 `assistant`；当前消息仅支持 user 角色。每次入口创建服务端 requestId，成功与受控错误同时返回 body 与 `X-Request-Id`。非法 JSON/参数返回 400，未知模型返回 404，上游失败返回 502。

M0 使用同步有限超时的单次调用，无自动重试。mock 明确声明 simulated；Ollama 显式关闭 streaming 并映射其 usage。未知/缺失 usage 保留 null，不能编造 token 或费用。

启动配置决定 provider 与上游模型；无管理接口、热更新或外部路由控制。回环地址绑定只适用于本地学习，扩大到非本机前需要完成 M3 的认证与授权。

## 故障与观测

参数拒绝不得调用 provider。上游响应格式异常、超时、网络故障返回固定安全错误；模型回答与异常正文不得进入日志。`ObservationFilter` 记录 requestId、耗时与结果；Actuator 提供进程探针和聚合 HTTP 指标。readiness 不校验 Ollama 模型是否加载，因此不能推断真实模型健康。

## 发布与回滚

本地从 mock 开始，手动切换 Ollama；失败时改回 mock 并重启。不存在数据迁移或线上发布。M0 上游使用可信本地进程；大响应上限、恶意上游及更严格的请求字节限制是 M2/M3 练习。

## Verification

- 参数/角色/逻辑模型边界、requestId、mock usage：`ChatHttpTest` / `ProviderFailureHttpTest`。
- 过滤器升序进入、逆序退出、异常清理与短路：`PipelineTest`。
- Ollama 请求协议、usage、格式错误和错误正文隔离：`OllamaChatModelTest`。
- 真实模型手动验证：第一课第 5–6 步，尚未执行。
- Actuator 探针：`scripts/smoke.sh`。

## Reviewer focus

模型 URL 只能来自启动配置；新增云 adapter 必须考虑数据目的地、密钥与费用；新增消息角色需要重新设计信任边界。本文尚未由项目负责人评审。
