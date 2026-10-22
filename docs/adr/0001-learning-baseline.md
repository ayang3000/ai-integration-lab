---
type: adr
title: ADR-0001：先建立可测量的集成骨架
description: 记录学习优先的技术选择与替代方案。
owner: NEEDS_OWNER
status: proposed
risk_level: low
tags: [ai-integration, learning]
---

# ADR-0001：先建立可测量的集成骨架

## 背景与选择

用户希望以现有 Java 后端经验转向 AI 集成，并参考 AGW 的设计哲学。选择 Maven 多模块、JDK 17 基线、Spring Boot 4.0.8、同步 MVC、显式端口与适配器。JDK 17 匹配当前本机环境，后续用 JDK 21+ 对比虚拟线程、结构化并发的适用范围；不把语言升级作为第一课前置任务。

Spring Boot 版本来自官方 4.0 文档的稳定维护版本，固定版本便于复现；这不是持续升级或安全维护承诺。集成 Spring AI 前需重新核对当时的 Spring Boot / Spring AI BOM 兼容矩阵，不能直接拼接示例版本。

先手写一层薄 Ollama adapter，理解 message、timeout、usage 和错误边界。M5 再与 Spring AI 的 ChatClient / Advisor / VectorStore 对比。避免第一步就用框架掩盖协议，或重造完整 Netty 服务器。默认 mock 无模型下载、密钥或推理费用。

## 备选与取舍

- 原生 Netty：有助于网络原理，但会拖慢 AI 集成主线；放到 M2 对比实验。
- 从零做 Python 全栈：有生态优势，但不能充分利用已有 Java 技能；Python 聚焦评测和数据。
- 一开始部署微服务 / Kubernetes：运维成本高；M9 有质量与负载证据后再实践。
- 一次实现所有 Agent / RAG：减少主动学习机会；保留清晰的里程碑练习。

## 兼容、迁移与回滚

当前没有用户数据或线上兼容负担。将同步接口保留到 M2，通过新端点引入流式协议；新增 provider 不改变 core 端口的厂商中立语义。切回 `AI_PROVIDER=mock` 可隔离本地模型故障，mock 必须显式标记模拟结果。

## Verification

当前模块 POM、集成测试和第一课实验。后续框架选型需要新的兼容性实验；Verification: Missing，M5 补齐。负责人和设计评审尚待确认，本文记录项目初始方案。

# Citations

- [Spring Boot 4.0 系统要求](https://docs.spring.io/spring-boot/4.0/system-requirements.html)
- [Ollama Chat API](https://docs.ollama.com/api/chat)
- [Spring AI ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [Spring AI RAG](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
