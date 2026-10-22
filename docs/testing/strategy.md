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
./scripts/mvn.sh clean verify
```

- `PipelineTest`：顺序、异常退栈、短路，是框架行为契约。
- `OllamaChatModelTest`：本机 JDK HTTP stub 校验真实序列化请求与响应解析，不依赖模型下载。
- `ChatHttpTest`：随机本机端口完整启动，验证 HTTP DTO、错误、requestId、mock 和 provider 失败的安全边界。
- 启动后的 `scripts/smoke.sh`：默认 mock 的聊天与探针；不是负载测试。

这些测试证明软件边界，不能证明真实模型效果、实际 Ollama 版本兼容性、token 正确性或生产安全。真实模型验证在第一课执行；本次不会下载安装模型或调用付费 API。

## 逐步增加

M2 加取消、慢客户端与持续输出测试；M3 加隔离、并发与故障注入；M4/M5 加版本化检索和生成集；M6/M8 加副作用与工作流恢复。Python 评测需把 deterministic 断言、人工标注与 LLM judge 分开，不把 judge 分数当唯一真值。

## Verification

测试报告在各模块 `target/surefire-reports/`，Git 不收录构建输出。未来评测和性能指标：Verification: Missing，里程碑实现时添加。
