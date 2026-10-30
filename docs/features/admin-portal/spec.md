---
type: feature-spec
title: 本地 Admin Portal
description: 浏览器中的网关连接档案、健康检查与 API 调试工作台。
owner: NEEDS_OWNER
status: proposed
risk_level: low
tags: [ai-integration, admin-portal]
---

# 本地 Admin Portal

## 目标与边界

为本地学习网关提供可直接操作的管理页面。静态 HTML、CSS 与原生 JavaScript ES module 随 launcher 的 JAR 打包，根路径 `/` 展示控制台，无 CDN、前端构建或独立 Node.js 服务依赖。

连接管理按用户选定的范围管理**网关地址与连接档案**；不配置上游 Ollama、不变更 provider、不增加服务端管理 API。模型路由由服务端配置决定，见[路由规格](../model-routing/spec.md)；回环绑定维持本地学习范围。

## 行为与数据

- 默认连接使用页面实际来源；可新增、编辑、删除和切换连接。至少保留一个连接，最多保存 20 个；删除前展示确认框。
- 连接名称、URL、当前连接由浏览器 localStorage 保存。载入时重新校验；损坏或不可读取时回到当前服务，存储失败时提示仅在本页面保留。URL 仅允许 HTTP / HTTPS，可包含路径前缀，拒绝内嵌凭据、查询参数和片段。
- 健康检查调用选定网关的 `/actuator/health`，仅在 HTTP 成功且状态为 UP 时标记正常。单次超时 5 秒；探针只证明进程健康，不证明模型可用。
- 测试台提供聊天、只读模型目录、健康检查、存活/就绪探针、服务信息和指标目录预设。聊天请求体可编辑，`model` 填模型目录中的别名；GET 不带请求体。展示真实 HTTP 状态、耗时、响应体、可读取的响应头与 requestId，支持复制响应和 POSIX shell cURL 命令。
- API 测试 30 秒超时，支持取消；非法 JSON 在发送前提示。HTTP 失败仍展示原响应；网络、CORS、取消、超时均显示明确状态。切换连接不会把进行中请求的结果标记为新连接。
- 最近 20 次 API 测试记录只在页面内存保存，刷新即清空；请求体和响应不写入 localStorage。动态名称和响应使用文本节点渲染。

## 跨域与发布

请求由浏览器直接发出，不经过服务端代理。不携带 Cookie 或认证信息。相同来源无需 CORS；跨域网关需要自行允许页面来源，响应头可见性也受 CORS 约束。HTTPS 页面中的 HTTP 网关连接可能被浏览器阻止。此版本没有新增认证与远程部署能力，仍用于本地学习。

使用 `./gradlew clean build` 打包，重启 JAR 后访问 `/`。回滚至前一版 JAR；浏览器档案不影响服务端状态。前端源码位于 `ai-launcher/src/main/resources/static/`，协议与存储逻辑由 `admin/core.mjs` 集中维护。

## Verification

- `ChatHttpTest.adminPortalAndAssetsAreServedAlongsideApi`：首页及 CSS / ES module 返回成功和正确内容类型；原 HTTP API 测试继续覆盖既有契约。
- `node --test ai-launcher/src/test/js/portal.test.mjs`：地址和档案校验、持久化恢复、损坏存储、响应格式化、cURL 引号边界。
- 手动浏览器验收：新增并编辑连接、切换和刷新保留、检查失效连接、删除确认；聊天成功/非法 JSON/服务端 404、GET 预设、响应头与请求记录；窄屏布局。
