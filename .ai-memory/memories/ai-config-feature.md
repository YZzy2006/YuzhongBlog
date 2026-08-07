# AI 配置功能完整知识库

> 记录时间: 2026-06-30
> 状态: 已完成，可用于其他项目参考

---

## 一、功能概述

管理后台的 AI 模型配置页面，支持多供应商预设、API Key 加密存储、余额查询、连接测试。
复刻自 TicketingSystem 项目的 `AiModelConfig.vue`，采用原生 HTML + CSS（非 Element Plus）。

---

## 二、UI 布局结构（从上到下）

### 2.1 当前激活模型信息栏

```
┌─────────────────────────────────────────────────────────────────┐
│ 当前模型  [DeepSeek — deepseek-chat]  OpenAI · Bearer           │
│           https://api.deepseek.com            官网 ↗            │
└─────────────────────────────────────────────────────────────────┘
```

- 渐变背景: `linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%)`
- 左侧: "当前模型" 标签 + 模型名标签（绿色/灰色）
- 中间: API 格式 · 认证方式（蓝色小字，动态显示）
- 右侧: API 地址（灰色，溢出省略） + 官网链接
- 未配置时显示灰色 "未配置" 标签

### 2.2 操作栏

```
[+ 配置模型]  [刷新]
```

- "配置模型" 蓝色主按钮，打开供应商选择弹窗
- "刷新" 幽灵按钮，重新加载配置

### 2.3 配置表格

```
┌──────────┬─────────┬─────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┐
│ 配置名称  │ 模型    │ API格式  │ 认证方式  │ API地址   │ API Key  │ 余额     │ 状态     │ 操作     │
├──────────┼─────────┼─────────┼──────────┼──────────┼──────────┼──────────┼──────────┼──────────┤
│ DeepSeek │deepseek │ OpenAI  │ Bearer   │https://..│ ****a8f2 │ ¥128.50  │ 已启用   │ 编辑 测试│
│          │ -chat   │         │          │          │          │          │          │ 查余额 停│
└──────────┴─────────┴─────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────┘
```

- 9 列表格，响应式横向滚动
- API Key 列显示脱敏值（`****` + 最后4位）
- 余额列：订阅制显示蓝色计划名，预付费显示绿色金额，失败显示红色
- 操作列：编辑、测试、查余额、停用 四个链接按钮
- 空状态：居中文字 "暂无模型配置，点击「配置模型」添加"

### 2.4 供应商选择弹窗

```
┌─────────────────────────────────────────┐
│ 选择供应商                               │
│                                          │
│ ┌──────────────┐  ┌──────────────┐      │
│ │ [D] DeepSeek  │  │ [M] Kimi     │      │
│ │ 高性价比国产..│  │ 月之暗面..   │      │
│ └──────────────┘  └──────────────┘      │
│ ┌──────────────┐  ┌──────────────┐      │
│ │ [Q] 通义千问  │  │ [O] OpenAI   │      │
│ │ 阿里旗舰..   │  │ GPT系列..    │      │
│ └──────────────┘  └──────────────┘      │
│ ┌──────────────┐  ┌──────────────┐      │
│ │ [A] Anthropic │  │ [Z] 智谱AI   │      │
│ │ Claude系列..  │  │ GLM系列..    │      │
│ └──────────────┘  └──────────────┘      │
│ ┌──────────────┐                        │
│ │ [+] 自定义    │                        │
│ │ 手动配置..   │                        │
│ └──────────────┘                        │
└─────────────────────────────────────────┘
```

- 2 列网格布局，7 个供应商（含自定义）
- 每个卡片：彩色圆角图标（40x40）+ 名称 + 描述
- 点击卡片自动填充预设值并进入表单

### 2.5 配置表单弹窗

```
┌─────────────────────────────────────────┐
│ ← 返回供应商选择                          │
│ 新增配置                                 │
│                                          │
│ 配置名称                                 │
│ [DeepSeek                            ]  │
│                                          │
│ API Key                                  │
│ [************************************] 👁│
│ 留空则不修改原密钥                        │
│                                          │
│ API 地址                                 │
│ [https://api.deepseek.com            ]  │
│                                          │
│ 模型名称                                 │
│ [deepseek-chat                        ]  │
│                                          │
│ ──── 高级选项 ▼ ─────────────────────── │
│                                          │
│ API 格式                                 │
│ [OpenAI Chat Completions             ▼] │
│ 使用 /chat/completions 端点（OpenAI兼容） │
│                                          │
│ 认证字段                                 │
│ [ANTHROPIC_AUTH_TOKEN (默认)         ▼] │
│ Authorization: Bearer <key> — 多数兼容API │
│                                          │
│ Max Tokens                               │
│ [4096]                                   │
│                                          │
│ Temperature                              │
│ [━━━━━━━━━━━━━━━━●━━━━━] 0.7            │
│                                          │
│ 官网地址                                 │
│ [https://platform.deepseek.com        ] │
│                                          │
│ 余额查询                                 │
│ [留空自动识别，或手动填入余额查询地址    ] │
│ DeepSeek/Kimi/通义千问可自动识别          │
│                                          │
│ 解析脚本                                 │
│ ┌──────────────────────────────────────┐│
│ │ 可选。填写后优先使用此脚本解析余额响应。││
│ │                                      ││
│ │ 示例：                               ││
│ │ function parse(response) {           ││
│ │   return {                           ││
│ │     balance: response.balance,       ││
│ │     currency: 'CNY',                 ││
│ │     isAvailable: true                ││
│ │   }                                  ││
│ │ }                                    ││
│ └──────────────────────────────────────┘│
│ JavaScript 函数，接收 response...         │
│                                          │
│ 备注                                     │
│ [可选备注信息...                        ] │
│                                          │
│              [取消]  [创建]              │
└─────────────────────────────────────────┘
```

---

## 三、全部参数字段

| 字段 | 表单键名 | 后端存储键 | 类型 | 默认值 | 说明 |
|------|---------|-----------|------|--------|------|
| 配置名称 | `name` | `ai_name` | string | - | 用于显示的友好名称 |
| API Key | `ai_api_key` | `ai_api_key` | string(加密) | - | AES-256-GCM 加密存储 |
| API 地址 | `ai_base_url` | `ai_base_url` | string | - | 如 `https://api.deepseek.com` |
| 模型名称 | `ai_model` | `ai_model` | string | - | 如 `deepseek-chat` |
| API 格式 | `apiFormat` | `ai_api_format` | enum | `OPENAI` | `OPENAI` / `ANTHROPIC` |
| 认证方式 | `authType` | `ai_auth_type` | enum | `BEARER` | `BEARER` / `X_API_KEY` / `RAW_TOKEN` / `API_KEY` |
| Max Tokens | `ai_max_tokens` | `ai_max_tokens` | string→int | `4096` | 最大生成 token 数 |
| Temperature | `ai_temperature` | `ai_temperature` | string→float | `0.7` | 生成随机性 0-2 |
| 官网地址 | `ai_website_url` | `ai_website_url` | string | - | 供应商官网链接 |
| 余额查询 | `ai_balance_url` | `ai_balance_url` | string | - | 留空自动识别 |
| 解析脚本 | `ai_balance_script` | `ai_balance_script` | string | - | JavaScript 函数 |
| 备注 | `description` | `ai_description` | string | - | 可选备注 |
| 启用状态 | `ai_enabled` | `ai_enabled` | string | `false` | `true` / `false` |

---

## 四、供应商预设配置

| ID | 名称 | 图标 | 颜色 | Base URL | 默认模型 | API格式 | 认证方式 |
|----|------|------|------|----------|---------|---------|---------|
| deepseek | DeepSeek | D | #4D6BFE | `https://api.deepseek.com` | deepseek-chat | OPENAI | BEARER |
| kimi | Kimi / Moonshot | M | #000000 | `https://api.moonshot.cn` | moonshot-v1-8k | OPENAI | BEARER |
| qwen | 通义千问 | Q | #615CED | `https://dashscope.aliyuncs.com/compatible-mode` | qwen-plus | OPENAI | BEARER |
| openai | OpenAI | O | #10A37F | `https://api.openai.com` | gpt-4o | OPENAI | BEARER |
| anthropic | Anthropic | A | #D97706 | `https://api.anthropic.com` | claude-sonnet-4-20250514 | ANTHROPIC | X_API_KEY |
| zhipu | 智谱 AI | Z | #3265FF | `https://open.bigmodel.cn/api/paas` | glm-4-flash | OPENAI | BEARER |
| custom | 自定义 | + | #64748b | - | - | OPENAI | BEARER |

---

## 五、API 格式与认证方式选项

### API 格式 (apiFormatOptions)

| 值 | 标签 | 提示文字 |
|----|------|---------|
| `OPENAI` | OpenAI Chat Completions | 使用 /chat/completions 端点（OpenAI 兼容格式） |
| `ANTHROPIC` | Anthropic Messages (native) | 使用 /messages 端点，自动添加 anthropic-version 头 |

### 认证方式 (authTypeOptions)

| 值 | 标签 | 提示文字 |
|----|------|---------|
| `BEARER` | ANTHROPIC_AUTH_TOKEN (默认) | Authorization: Bearer \<key\> — 多数 OpenAI 兼容 API |
| `X_API_KEY` | ANTHROPIC_API_KEY | x-api-key: \<key\> — Anthropic 原生格式 |
| `RAW_TOKEN` | Authorization (无Bearer前缀) | Authorization: \<key\> — 无 Bearer 前缀 |
| `API_KEY` | AZURE_API_KEY | api-key: \<key\> — Azure OpenAI 格式 |

---

## 六、后端 API 端点

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/admin/ai/settings` | 获取所有 AI 配置（Key 脱敏） | JWT |
| PUT | `/admin/ai/settings` | 保存 AI 配置 | JWT |
| POST | `/admin/ai/test` | 测试连接 | JWT |
| GET | `/admin/ai/balance` | 查询余额 | JWT |

### GET /admin/ai/settings 响应

返回 Map，Key 为 camelCase（`apiFormat`、`authType`），其余为 snake_case。
API Key 返回脱敏值 `****` + 最后4位，无配置时返回空字符串。

### PUT /admin/ai/settings 请求

前端发送 snake_case JSON，后端 DTO 用 `@JsonNaming(SnakeCaseStrategy.class)` 自动映射。
API Key 为脱敏值（以 `****` 开头）时跳过更新，留空时也跳过。

### POST /admin/ai/test 响应

```json
{
  "success": true,
  "message": "连接成功",
  "latency": 1234,
  "model": "deepseek-chat"
}
```

### GET /admin/ai/balance 响应

```json
{
  "success": true,
  "balance": "128.50",
  "currency": "CNY",
  "planType": "prepaid",
  "isAvailable": true,
  "message": "查询成功"
}
```

---

## 七、余额查询自动识别

`BalanceCheckService` 根据 baseUrl 自动识别供应商：

| 供应商 | URL 特征 | 余额接口 |
|--------|---------|---------|
| DeepSeek | `deepseek` | `/user/balance` |
| Kimi | `moonshot` / `kimi` | `/v1/users/me/balance` |
| 通义千问 | `dashscope` / `aliyuncs` | `/api/v1/user/balance` |
| 自定义 | 配置了 `balanceUrl` | 使用自定义 URL |

---

## 八、遇到的问题与解决方案

### 问题 1: JSON 字段名不匹配（Critical）

**现象**: 填写 API Key 后保存，查询余额提示 "未配置 API Key"
**根因**: 前端发 snake_case (`ai_api_key`)，后端 DTO 用 camelCase (`aiApiKey`)，Jackson 默认 camelCase 匹配，所有字段反序列化为 null
**初次修复**: DTO 加 `@JsonNaming(SnakeCaseStrategy.class)` — 但这是错误的！
**二次问题**: `@JsonNaming` 会把 **所有** 字段转 snake_case，包括 `apiFormat` → `api_format`，导致 camelCase 字段也反序列化为 null
**最终修复**: 去掉 `@JsonNaming`，前端加 `toCamelCasePayload()` 函数显式映射为 camelCase
**教训**: `@JsonNaming` 是全量转换，混用 snake_case 和 camelCase 字段时不能用；前后端命名不一致时，前端显式映射更可靠

### 问题 2: 停用按钮失效

**现象**: 点击 "停用" 后 AI 仍然启用
**根因**: `handleSave()` 无条件执行 `form.ai_enabled = 'true'`，覆盖了 `handleDisable()` 设的 `'false'`
**解决**: 只在新增配置时自动启用，编辑时保持原值
**教训**: 共用的保存函数不应无条件修改状态，应由调用者控制

### 问题 3: Ctrl 键关闭弹窗

**现象**: 在输入框中按 Ctrl 键，配置弹窗被关闭
**根因**: `@click.self` / `@mousedown.self` 在修饰键按下时可能被浏览器误触发
**解决**: 改用带修饰键检查的函数 `onMaskMouseDown(e)`，检查 `!e.ctrlKey && !e.metaKey && !e.shiftKey && !e.altKey`
**教训**: 弹窗遮罩的点击关闭应过滤修饰键事件，避免误触

### 问题 4: API Key 安全性

**现象**: 后端返回固定掩码 `sk-****`，前端本地计算脱敏
**根因**: 脱敏逻辑分散在前后端，不够统一
**解决**: 后端统一脱敏为 `****` + 最后4位，前端直接使用；编辑时清空输入框，placeholder 显示脱敏值
**教训**: 安全相关的脱敏逻辑应集中在后端处理

### 问题 5: @Lob 与 columnDefinition

**现象**: 项目规范要求用 `columnDefinition = "TEXT"` 而非 `@Lob`
**根因**: `@Lob` 在 MySQL 映射为 `LONGTEXT`，某些 Hibernate 版本可能冲突
**解决**: `SiteSetting` 实体改为 `@Column(columnDefinition = "TEXT")`
**教训**: MySQL TEXT 字段统一用 `columnDefinition = "TEXT"`

---

## 九、关键技术模式

### 9.1 SiteSetting 键值存储

AI 配置复用已有的 `site_setting` 表，以 `ai_` 前缀区分：
- 优点：无需新建表，简单直接
- 缺点：无法存储多配置（单配置模式）
- 适用：配置项少、不需要多实例的场景

### 9.2 AES-256-GCM 加密

API Key 使用 `AesUtil` 加密存储：
- 算法：AES-256-GCM（认证加密）
- IV：每次加密随机生成
- Key：从 `application.properties` 的 `app.encryption.secret` 派生
- 存储格式：Base64(IV + 密文)

### 9.3 前端弹窗关闭优化

```javascript
function onMaskMouseDown(e) {
  // 只在纯左键无修饰键时关闭
  if (e.button === 0 && !e.ctrlKey && !e.metaKey && !e.shiftKey && !e.altKey) {
    dialogVisible.value = false
  }
}
```

- 用 `@mousedown.self` 代替 `@click.self`（更精确）
- 检查修饰键防止 Ctrl/Shift/Alt 误触
- `.self` 确保只在点击遮罩本身时触发，子元素不触发

### 9.4 formatLabel 辅助函数

```javascript
function formatLabel(options, value) {
  const found = options.find(o => o.value === value)
  return found ? found.label : value
}
```

用于在表格和信息栏中显示选项的标签文本。

### 9.5 高级选项自动展开

```javascript
// 选择供应商时
showAdvanced.value = form.apiFormat !== 'OPENAI' || form.authType !== 'BEARER'

// 编辑时（额外检查 balanceScript）
showAdvanced.value = form.apiFormat !== 'OPENAI' || form.authType !== 'BEARER' || !!form.ai_balance_script
```

当配置使用非默认格式或有自定义脚本时，自动展开高级选项。

### 9.6 AiService 多认证方式支持

```java
private void applyAuthHeader(WebClient.RequestHeadersSpec<?> request, AiConfig config) {
    switch (config.authType()) {
        case "X_API_KEY" -> request.header("x-api-key", config.apiKey());
        case "RAW_TOKEN" -> request.header(HttpHeaders.AUTHORIZATION, config.apiKey());
        case "API_KEY" -> request.header("api-key", config.apiKey());
        default -> request.header(HttpHeaders.AUTHORIZATION, "Bearer " + config.apiKey());
    }
}
```

- `BEARER`: `Authorization: Bearer <key>` — 多数 OpenAI 兼容 API
- `X_API_KEY`: `x-api-key: <key>` — Anthropic 原生
- `RAW_TOKEN`: `Authorization: <key>` — 无 Bearer 前缀
- `API_KEY`: `api-key: <key>` — Azure OpenAI

**教训**: 不能硬编码 Bearer，必须根据配置动态选择认证头

### 9.7 SSE 流式响应行缓冲

TCP 不保证 SSE 数据完整到达，`data:` 行可能跨 buffer 分裂：

```java
StringBuilder lineBuffer = new StringBuilder();
// subscribe 回调中:
lineBuffer.append(raw);
String buffered = lineBuffer.toString();
int lastNewline = buffered.lastIndexOf('\n');
if (lastNewline == -1) return; // 无完整行，继续累积
String completePart = buffered.substring(0, lastNewline);
String remainder = buffered.substring(lastNewline + 1);
lineBuffer.setLength(0);
lineBuffer.append(remainder);
for (String line : completePart.split("\n")) {
    // 处理完整行
}
```

**教训**: `bodyToFlux(DataBuffer.class)` 必须用行缓冲，不能直接 `split("\n")`

### 9.8 SSRF 防护（IPv6 安全）

```java
String host = uri.getHost();
// IPv6 方括号剥离
if (host.startsWith("[") && host.endsWith("]")) {
    host = host.substring(1, host.length() - 1);
}
// 检查 localhost / 私有 IP / IPv6 回环 / IPv4-mapped IPv6
if (host.equals("localhost") || host.startsWith("127.") || host.startsWith("10.")
        || host.matches("^172\\.(1[6-9]|2[0-9]|3[01])\\..*")
        || host.startsWith("192.168.") || host.equals("169.254.169.254")
        || host.equals("0.0.0.0") || host.equals("::1")
        || host.startsWith("::ffff:") || host.startsWith("0:0:0:0:0:ffff:")) {
    throw new IllegalArgumentException("Base URL not allowed");
}
```

**教训**: `URI.getHost()` 返回 IPv6 时带方括号 `[::1]`，必须先剥离再比较

### 9.9 缓存原子更新

```java
// 错误：两次 volatile 写入不原子
private volatile AiConfig cachedConfig;
private volatile long cacheTimestamp;

// 正确：单次 volatile 写入
private record CacheEntry(AiConfig config, long timestamp) {}
private volatile CacheEntry cacheEntry;
```

**教训**: 多个 volatile 变量需要同时更新时，合并为单个 record

---

## 十、文件清单

### 后端

| 文件 | 说明 |
|------|------|
| `dto/ai/AiSettingsRequest.java` | 请求 DTO，纯 `@Data`，无 Jackson 注解（前端显式 camelCase 映射） |
| `controller/admin/AiSettingAdminController.java` | 4 个端点 + `maskApiKey()` + `isMaskedKey()` |
| `service/AiService.java` | AI 调用核心，支持多认证格式 + SSE 流式 + SSRF 防护 |
| `service/BalanceCheckService.java` | 余额查询，自动识别 DeepSeek/Kimi/通义千问 |
| `util/AesUtil.java` | AES-256-GCM 加解密 |
| `entity/SiteSetting.java` | 键值存储实体，`columnDefinition = "TEXT"` |

### 前端

| 文件 | 说明 |
|------|------|
| `views/admin/AiSettings.vue` | AI 配置页面，原生 HTML + CSS |
| `utils/request.js` | Axios 封装，自动附加 JWT |

---

## 十一、待优化项

1. **多配置支持**: 当前单配置模式，如需多配置需改为独立实体 + CRUD
2. **表单验证**: 当前无前端验证，可加必填校验
3. **暗黑模式**: 当前无暗黑模式样式
4. **国际化**: 当前硬编码中文
5. **单元测试**: `AiSettingAdminController`、`AiService` 和 `BalanceCheckService` 无测试覆盖

---

*最后更新: 2026-06-30*
