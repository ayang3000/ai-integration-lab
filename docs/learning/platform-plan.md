---
type: feature-spec
title: AI Platform 转型的十二周实践计划
description: 把后端经验转换为模型网关、业务样例与可复现的工程证据。
owner: NEEDS_OWNER
status: proposed
risk_level: low
tags: [learning, ai-platform, evaluation]
---

# AI Platform 转型的十二周实践计划

## 定位与依据

依据用户提供的《Java后端工程师向AI转型路线图.md》（整理日期 2026-10-30），项目主线调整为 **Java Backend → AI Backend / AI Platform → LLM Infrastructure**。多服务集成经验用于模型适配，服务治理用于路由/限流/故障恢复，运维经验用于推理资源与质量成本分析。职业定位是学习方向，不保证求职结果。

作品由两部分组成：Java AI Gateway 提供平台能力；[Archiver Incident Copilot](../features/incident-copilot/spec.md) 提供合成业务场景和质量验收。保留 Java 主后端，Python 用于数据、评测和小型 FastAPI 对照实验。框架比较以 Spring AI 为主、LangChain4j 为辅，接入前验证与当前 Spring Boot BOM 的兼容性，不直接替换现有薄 adapter。

## 时间盒与里程碑映射

每周约 8–10 小时；十二周交付有限 demo 和证据，不等于完成 [M0–M9](roadmap.md) 的全部生产能力。里程碑编号表示能力和验收项，下表表示可交错推进的练习节奏。超出时间盒时缩小数据/供应商范围，不省略失败验证。

| 时间 | 重点 / 关联里程碑 | 可交付证据与完成条件 |
|---|---|---|
| 1–2 周 | LLM 基础、模型调用、流式，M0/M1/M2 | 先复现现有多别名路由；选一真实模型解释 token、上下文、采样、Attention；补一个云 adapter 或第二本地模型；20 条输入及结构化输出失败样本；SSE 取消和中途故障测试。当前仅同步别名路由已提供 |
| 3–4 周 | Python、embedding、pgvector，M4/M5 | 用合成 Runbook 建入库与问答原型；比较分块、HNSW、关键词与混合检索、reranker；50 条标注检索集和引用检查；小型 FastAPI 对照脚本作为选做 |
| 5–6 周 | Tools / MCP / Agent，M6/M7/M8 | 只读模拟 Job 查询；参数校验与 tenant 权限；MCP 互操作；固定工作流后再加有步数/时间限制的 Agent；越权、注入、工具失败用例 |
| 7–8 周 | 网关治理，M3 | 从精确别名路由扩展为显式任务规则；按 API Key/tenant/model 控制 RPM、TPM；故障分类、有限重试、总 deadline 与 fallback；流开始后不切模型 |
| 9–10 周 | 生产工程实验，M3/M9 | Redis 预算预留/结算与 PostgreSQL 审计练习；输入/最大输出预算，取消与 usage 缺失结算；并发/排队/熔断测试；记录 TTFT、P95、吞吐、错误率、token 和费用，区分估算与账单 |
| 11–12 周 | 评测与作品，M9 | 逐步扩到约 100 条故障样本；固定版本做质量/成本/延迟对比；Docker Compose/CI 回归；5 分钟 demo、架构图、故障报告、取舍说明；指标必须来自实际实验 |

## 路由演进顺序

1. **已实现基础**：服务端精确别名 → 单个 provider/model；无自动择优。
2. **待实现规则**：显式任务、能力需求、上下文预算筛选候选；规则可解释、可复现。
3. **待实现统计策略**：先限定合格质量，再比较价格、成功率、P95；固定评测集约束便宜模型替换。不能凭模型名称推断质量。
4. **选修动态策略**：可靠反馈和离线基线具备后再研究 Contextual Bandit；PPO、训练 Transformer 和微调不作为近期前置条件。

对 LiteLLM 的学习重点是路由、限额、fallback 和计量方案的对照实验。个人网关的价值在于证明边界正确和理解取舍，而不是复制全部产品功能。

## 再用三至六个月深入 Infra

按目标岗位逐步研究 vLLM、KV Cache、prefill/decode、continuous batching、量化和推理资源调度。在固定模型/数据/硬件上记录吞吐与延迟，并解释排队、并发和显存的约束。Docker/Kubernetes 扩缩容以测量为依据；本机资源不足时先做机制阅读与小规模 CPU/本地模型实验，不把购买 GPU 作为本项目要求。

## 自测与证据模板

每个阶段记录：代码版本、模型/参数、数据集版本、机器环境、运行命令、样本量、成功/拒答/失败数量、指标和限制。mock 和 stub 证明工程契约，不能进入真实模型质量排名。

- 为什么 Tomcat 线程上限不能代表 LLM 并发容量？解释排队、上下文、KV Cache 和推理瓶颈。
- 预计 20K token 的请求如何预留预算？分别处理输入估算、最大输出、缺失 usage 与并发扣减。
- 429/超时是否允许重试？描述 deadline、潜在重复费用和流是否已经输出。
- SSE 已输出部分内容后如何报告失败？不得把另一个模型的回答拼接成同一次成功。
- 便宜模型替换后如何证明质量可接受？使用相同样本、人工复核和明确准入阈值。
- 工具如何阻止跨 tenant 访问？权限依据来自服务端身份，不是模型参数。
- P95 增长发生在哪一段？分别测量检索、排队、推理及工具执行。

## Verification

当前落地能力见[路由规格](../features/model-routing/spec.md)及其自动化测试。其余计划与自测需要学习者提交实验记录，Verification: Missing。此文不声称已完成十二周实践，也不将生成代码视作个人技能验收。

# Citations

- 用户提供的《Java后端工程师向AI转型路线图.md》：转型定位、十二周节奏、平台能力、Copilot 场景与自测问题；原文件未复制入仓库。
