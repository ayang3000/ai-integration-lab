---
type: test-plan
title: 测试与评测策略
description: 区分工程正确性、模型效果和真实供应商验证。
owner: NEEDS_OWNER
status: proposed
tags: [ai-integration, learning]
---

# 测试与评测策略

## 当前检查

```bash
./gradlew clean build
```

- `ModelRouterTest` / `RoutingConfigurationTest`：路由选择、未知别名无出站、快照与非法配置拒绝。
- `RoutingHttpTest`：同进程 mock 与 Ollama HTTP stub，目录、显式路由替换与真实协议分发。
- `ObservationFilterTest`：失败计时、有限标签与供应商 usage 计量边界。
- `PipelineTest`：顺序、异常退栈、短路，是框架行为契约。
- `OllamaChatModelTest`：本机 JDK HTTP stub 校验真实序列化请求与响应解析，不依赖模型下载。
- `ChatHttpTest`：随机本机端口完整启动，验证 HTTP DTO、错误、requestId、mock 的安全边界；`ProviderFailureHttpTest` 单独验证 provider 失败。
- 启动后的 `scripts/smoke.sh`：默认 mock 的聊天与探针；不是负载测试。
- `ChatHttpTest.adminPortalAndAssetsAreServedAlongsideApi`：根路径管理页面与静态资源内容类型。
- `node --test ai-launcher/src/test/js/portal.test.mjs`：可选 Node.js 18+ 前端逻辑检查，无第三方依赖；浏览器交互验收见 [Portal 规格](../features/admin-portal/spec.md)。

这些测试证明软件边界，不能证明真实模型效果、实际 Ollama 版本兼容性、token 正确性或生产安全。真实模型验证在第一课执行；本次不会下载安装模型或调用付费 API。

## 逐步增加

M2 加取消、慢客户端与持续输出测试；M3 加隔离、并发与故障注入；M4/M5 加版本化检索和生成集；M6/M8 加副作用与工作流恢复。Python 评测需把 deterministic 断言、人工标注与 LLM judge 分开，不把 judge 分数当唯一真值。

未来质量集采用[Copilot 评测设计](../features/incident-copilot/spec.md)，覆盖约 100 条合成故障、无答案、冲突、注入与工具越权。网关契约测试与模型质量评测分开记录。

## Verification

测试报告在各模块 `build/reports/tests/test/`（HTML）和 `build/test-results/test/`（JUnit XML），Git 不收录构建输出。未来评测和性能指标：Verification: Missing，里程碑实现时添加。
