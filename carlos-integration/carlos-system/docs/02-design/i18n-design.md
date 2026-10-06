# carlos-system i18n 国际化功能设计与使用说明

> **版本**: v1.2
> **日期**: 2026-09-17
> **状态**: 已实现
> **维护者**: Development Team
>
> **v1.2 变更**：后端表从「前端语言文件的全量镜像」改为「**只存覆盖值**」。
> 原先要维护一条「前端 → 迁移 SQL → 后端表」的同步管道，漏跑就会静默不一致
> （曾发生：253 个 key 滞后数月）。覆盖模型下没有需要同步的东西 —— 没改过就没有行。
> 迁移见 `docs/02-design/database/sys_i18n_override_migration.sql`，说明见 §7。

---

## 目录

1. [功能定位](#1-功能定位)
2. [设计目标](#2-设计目标)
3. [数据库设计](#3-数据库设计)
4. [模块结构](#4-模块结构)
5. [接口设计](#5-接口设计)
6. [缓存策略](#6-缓存策略)
7. [本地文案与后端文案边界](#7-本地文案与后端文案边界)
8. [前端对接](#8-前端对接)
9. [状态字段约定](#9-状态字段约定)
10. [认证与白名单](#10-认证与白名单)
11. [常见问题](#11-常见问题)
12. [文件清单](#12-文件清单)

---

## 1. 功能定位

`carlos-system` 的 i18n 模块为整个 Carlos Framework 提供**后端驱动的国际化消息管理能力**。

- 前端 SoybeanAdmin 在保留本地基础文案的同时，应用启动时异步从后端拉取业务文案并合并到 `vue-i18n`。
- 运营/产品团队可通过「系统管理 → 国际化管理」页面在线维护文案，无需前端发版即可生效。
- 与 `sys_module` 功能模块表打通，支持按模块分组和按需加载。

---

## 2. 设计目标

| 目标   | 说明                                                    |
|------|-------------------------------------------------------|
| 统一管理 | `sys_i18n_message` 表集中维护业务相关文案                        |
| 动态加载 | 前端初始化及切换语言时异步拉取后端消息                                   |
| 模块隔离 | 按 `sys_module.module_code` 分组，支持 `moduleCodes` 按需加载   |
| 多级缓存 | `MultiLevelCacheUtil`（Caffeine L1 + Redis L2）缓存 30 分钟 |
| 边界清晰 | 前端 UI 框架文案保留在本地，业务文案由后端管理，冲突时告警                       |
| 唯一约束 | 同一语言 + 同一模块 + 同一消息键不可重复                               |

---

## 3. 数据库设计

### 3.1 sys_module（功能模块表）

复用系统已有的 `sys_module` 表。通过 `module_code` 与 i18n 消息关联。

真实 PostgreSQL DDL 见：
`carlos-integration/carlos-system/docs/02-design/database/system_postgresql.sql`

### 3.2 sys_i18n_message（国际化消息表）

```sql
DROP TABLE IF EXISTS sys_i18n_message CASCADE;
CREATE TABLE sys_i18n_message
(
    id          BIGINT       NOT NULL,
    lang        VARCHAR(20)  NOT NULL,
    module_id   BIGINT,
    msg_key     VARCHAR(200) NOT NULL,
    msg_value   TEXT         NOT NULL,
    description VARCHAR(500),
    msg_status  BOOLEAN,
    create_by   BIGINT,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE sys_i18n_message IS '国际化消息';
COMMENT ON COLUMN sys_i18n_message.lang IS '语言标识';
COMMENT ON COLUMN sys_i18n_message.module_id IS '所属模块ID';
COMMENT ON COLUMN sys_i18n_message.msg_key IS '消息键';
COMMENT ON COLUMN sys_i18n_message.msg_value IS '消息值';
COMMENT ON COLUMN sys_i18n_message.description IS '描述说明';
COMMENT ON COLUMN sys_i18n_message.msg_status IS '状态：true启用 false禁用';
COMMENT ON COLUMN sys_i18n_message.create_by IS '创建者';
COMMENT ON COLUMN sys_i18n_message.create_time IS '创建时间';
COMMENT ON COLUMN sys_i18n_message.update_by IS '更新者';
COMMENT ON COLUMN sys_i18n_message.update_time IS '更新时间';

CREATE INDEX idx_i18n_module_key ON sys_i18n_message (module_id, msg_key);
CREATE INDEX idx_i18n_lang ON sys_i18n_message (lang);

-- 唯一约束：同语言 + 同消息键 不可重复
-- module_id 不参与唯一性（它只是可选的分类标签，覆盖行一律为 NULL）
CREATE UNIQUE INDEX uk_i18n_lang_msg_key
    ON sys_i18n_message (lang, msg_key);
```

**设计要点：**

- `lang` 使用下划线格式存储，如 `zh_CN`、`en_US`。
- `module_id` 允许为 `NULL`，表示全局消息；唯一索引通过 `COALESCE(module_id, 0)` 将 NULL 统一归为一组。
- `msg_key` 支持点号分隔的层级结构，如 `system.title`、`page.manage.user.title`。
- `msg_status` 为 `BOOLEAN`，`true` 启用，`false` 禁用；只有启用状态的消息会返回给前端。

---

## 4. 模块结构

```
com.carlos.system.i18n/
├── controller/
│   └── SysI18nController.java          # REST 接口（管理 + 前端暴露）
├── service/
│   └── SysI18nService.java             # 业务串联 + 新增/更新查重
├── manager/
│   ├── SysI18nManager.java             # 数据操作接口
│   └── impl/
│       └── SysI18nManagerImpl.java     # 数据操作实现
├── mapper/
│   └── SysI18nMapper.java              # MyBatis Mapper
├── convert/
│   └── SysI18nConvert.java             # MapStruct 转换
└── pojo/
    ├── entity/SysI18nMessage.java
    ├── dto/SysI18nMessageDTO.java
    ├── vo/SysI18nMessageVO.java        # 管理后台 VO
    ├── vo/SysI18nMessagesVO.java       # 前端接口 VO（key-value 格式）
    ├── param/SysI18nCreateParam.java
    ├── param/SysI18nUpdateParam.java
    └── param/SysI18nPageParam.java
```

---

## 5. 接口设计

### 5.1 管理接口（需登录）

| 方法   | 路径                 | 说明       |
|------|--------------------|----------|
| POST | `/sys/i18n`        | 新增消息     |
| POST | `/sys/i18n/delete` | 删除（支持批量） |
| POST | `/sys/i18n/update` | 更新消息     |
| GET  | `/sys/i18n/{id}`   | 详情       |
| GET  | `/sys/i18n/page`   | 分页列表     |

**新增/更新参数：**

```json
{
  "lang": "zh_CN",
  "moduleId": 1,
  "msgKey": "system.title",
  "msgValue": "Carlos 管理系统",
  "description": "系统标题",
  "msgStatus": true
}
```

**字段约定：**

- `msgStatus` 为 `Boolean`，`true` 启用，`false` 禁用。
- 同一 `(lang, moduleId, msgKey)` 组合不可重复，重复会返回 `409` 错误码 `20973`。

### 5.2 前端暴露接口（公开只读）

| 方法  | 路径                   | 说明          |
|-----|----------------------|-------------|
| GET | `/sys/i18n/messages` | 获取指定语言的启用消息 |

**请求示例：**

```
GET /sys/i18n/messages?lang=zh_CN&moduleCodes=common,system
```

**参数：**

| 参数          | 类型     | 必填 | 说明                                                           |
|-------------|--------|----|--------------------------------------------------------------|
| lang        | String | 是  | 语言标识，如 `zh_CN`、`en_US`，需符合 `^[a-zA-Z]{2}([-_][a-zA-Z]{2})?$` |
| moduleCodes | String | 否  | 模块编码列表，逗号分隔，最大长度 200 字符                                      |

**参数校验与安全防护：**

- `lang` 使用 `@Pattern` 校验格式，防止缓存 key 被任意字符串污染。
- `moduleCodes` 使用 `@Size` 限制长度，并在 Controller 中做去重、排序、过滤空值处理，保证缓存 key 稳定。
- 当 `moduleCodes` 传入了编码但全部解析失败时，返回空结果，不会退化为全量查询。

**响应：**

```json
{
  "success": true,
  "code": "00000",
  "msg": "操作成功",
  "data": {
    "lang": "zh_CN",
    "messages": {
      "system.title": "Carlos 管理系统",
      "common.add": "新增",
      "common.delete": "删除"
    }
  },
  "timestamp": 1710638258000
}
```

---

## 6. 缓存策略

使用 `MultiLevelCacheUtil`（基于 Redisson `RLocalCachedMap`），实现 Caffeine L1 + Redis L2 自动两级缓存。

### 6.1 缓存 Key 规则

| 场景    | Key 格式                                             |
|-------|----------------------------------------------------|
| 全量加载  | `i18n:messages:{lang}`                             |
| 按模块加载 | `i18n:messages:{lang}:{moduleCode1},{moduleCode2}` |

### 6.2 缓存配置

| 配置项  | 值                | 说明           |
|------|------------------|--------------|
| TTL  | 30 分钟            | 缓存过期时间       |
| 同步策略 | Redisson Pub/Sub | 多实例间本地缓存自动同步 |

### 6.3 失效策略

| 操作     | 缓存行为                                            |
|--------|-------------------------------------------------|
| 新增单条消息 | 按语言前缀失效：`evictByPrefix("i18n:messages:{lang}")` |
| 更新单条消息 | 按语言前缀失效：`evictByPrefix("i18n:messages:{lang}")` |
| 批量删除   | 按全局前缀失效：`evictByPrefix("i18n:messages:")`       |

**注意：** 已废弃直接调用 `MultiLevelCacheUtil.clear()` 全量清除的方式，避免误清其它业务缓存。

---

## 7. 前端目录与后端覆盖

> **v1.2 起语义变更**：后端表从「前端语言文件的全量镜像」改为「**只存运营改过的那部分覆盖值**」。
> 迁移脚本：`docs/02-design/database/sys_i18n_override_migration.sql`

前端与后端各承担一件事，**不存在"两份都要维护"**：

| 层 | 职责 | 权威性 |
|---|---|---|
| 前端 `locales/langs/*.ts` | 完整的 key 目录 + 基线值 + `$t()` 的类型源 + 离线兜底 | **key 与基线值的权威** |
| 后端 `sys_i18n_message` | 只存被改过的 key 的覆盖值 | **覆盖值的权威** |

运行时 `loadBackendMessages` 拉回覆盖值，经 `mergeLocaleMessage` 深合并：

```
$t(key) 拿到的值 = 后端覆盖值 ?? 前端基线值
```

**这个模型下没有"同步"这一步** —— 没改过就没有行。因此不会再出现
「前端加了 key、后端不知道」这类不一致（旧模型下曾发生：253 个 key 滞后数月无人察觉）。

### 7.1 边界划分

**保留在前端本地（不允许被后端覆盖）：**

| 命名空间       | 说明                |
|------------|-------------------|
| `theme`    | 主题设置面板文案          |
| `form`     | 表单校验、日期等组件级文案     |
| `dropdown` | 下拉选择器文案           |
| `icon`     | 图标选择器文案           |
| `request`  | 请求级提示（如 token 过期） |

**允许被后端覆盖（运营按需改值）：**

| 命名空间     | 说明         |
|----------|------------|
| `system` | 系统标题、更新提示等 |
| `common` | 通用操作按钮、提示  |
| `route`  | 路由/菜单标题    |
| `page`   | 各业务页面文案    |

### 7.2 冲突检测

`loadBackendMessages` 在合并前会检查：若后端返回的 key 首段属于本地保留命名空间，则跳过并在浏览器控制台告警：

```
[i18n] backend key "theme.mode" conflicts with local-only namespace "theme", skipped
```

这保证了 UI 框架级文案不会被后端配置意外覆盖。

### 7.3 维护流程

**开发新增文案**（走仓库，与代码一起评审、发版）

1. 三处同步写：`locales/langs/zh-cn.ts` + `en-us.ts` + `src/typings/app.d.ts`
2. 页面里用 `$t('...')`（ESLint 规则 `local/no-hardcoded-cjk` 会拦住硬编码）
3. **到此结束，不需要任何后端操作** —— 后端会持续使用基线值，直到有人去改它

**运营改文案值**（走「系统管理 → 国际化管理」页面）

- 页面列出**前端目录里的全部 key** + 基线值 + 后端覆盖值（key 列表来自前端，不依赖后端）
- 改哪条就产生哪条覆盖行；点「还原」删掉覆盖行即回落到基线
- 写操作会 `evictByPrefix` 清缓存，**但前端需要刷新页面**才能看到新值
  （`loadBackendMessages` 只在启动与切换语言时调用，没有轮询）

### 7.4 关键约束：为什么 key 必须留在前端

`$t()` 的签名是 `(key: I18nKey)`，而 `I18nKey = GetI18nKey<App.I18n.Schema>`，
`Schema` 定义在 `src/typings/app.d.ts` 且与语言文件严格同构。**key 不在前端就编译不过。**

由此推出两条：

- **加 key 必须走前端** —— 只往后端加 key，前端 `$t('它')` 编译不过；而且 key 不在前端目录里，
  「国际化管理」页面也列不出它
- **前端基线值不能删** —— 覆盖行是**叠加在基线之上的补丁**，不是替代品。删掉基线，
  那些没有被覆盖的 key 就全空了

> 早期版本文档曾建议「迁移完成后从本地文件中删除对应 key，避免双维护」。那是全量镜像模型下的
> 说法，而且实际做不到（见上）。现在的覆盖模型从根上取消了「双维护」这件事。


---

## 8. 前端对接

### 8.1 初始化加载

```typescript
// src/main.ts
import {setupI18n} from './locales';

async function setupApp() {
  // ...
  await setupI18n(app);
  // ...
}
```

```typescript
// src/locales/index.ts
export async function setupI18n(app: App) {
  app.use(i18n);
  const currentLocale = i18n.global.locale.value as App.I18n.LangType;
  await loadBackendMessages(currentLocale);
}
```

### 8.2 语言切换

```typescript
// src/store/modules/app/index.ts
import {setLocale, loadBackendMessages} from '@/locales';

async function changeLocale(lang: App.I18n.LangType) {
  setLocale(lang);
  localStg.set('lang', lang);
  await loadBackendMessages(lang);
}
```

### 8.3 在组件中使用

与标准 vue-i18n 用法一致：

```vue

<template>
  <h1>{{ $t('system.title') }}</h1>
  <button>{{ $t('common.add') }}</button>
</template>
```

---

## 9. 状态字段约定

前后端统一使用 `msgStatus` 表示国际化消息的启用/禁用状态。

- **后端**：`SysI18nMessage` / `SysI18nMessageDTO` / `SysI18nMessageVO` / `SysI18nCreateParam` / `SysI18nUpdateParam` / `SysI18nPageParam` 全部使用 `Boolean msgStatus`。
- **前端**：`Api.SystemManage.I18nMessage` 类型中字段名为 `msgStatus: boolean`。
- 数据库列名为 `msg_status`，类型 `BOOLEAN`。

---

## 10. 认证与白名单

`/sys/i18n/messages` 需要在未登录状态下被前端调用（应用初始化时），因此已加入网关 OAuth2 认证白名单：

```yaml
carlos:
  gateway:
    oauth2:
      whitelist:
        - /public/**
        - /static/**
        - /auth/public-key
        - /auth/user/login
        - /auth/user/pre-check
        - /auth/user/logout
        - /auth/captcha/**
        - /auth/mfa/step-up-verify
        - /sys/i18n/messages
```

**注意：** 如果生产环境使用 Nacos 或外部化配置管理网关白名单，请同步更新对应配置，否则本地 YAML 修改不会生效。

---

## 11. 常见问题

### Q1: 前端切换语言后翻译未更新？

**A**: 检查以下几点：

1. 后端 `/sys/i18n/messages` 接口是否正常返回数据。
2. `loadBackendMessages` 是否正确执行。
3. 浏览器控制台查看网络请求是否有 401/403 报错。
4. 检查缓存是否已过期（30 分钟）。

### Q2: 新增消息后前端不显示？

**A**:

1. 确认消息状态为「启用」（`msgStatus = true`）。
2. 确认消息键拼写与前端使用的一致。
3. 确认消息 key 首段不属于本地保留命名空间（否则会被跳过）。
4. 清除浏览器缓存或等待后端缓存过期。

### Q3: 如何只加载特定模块的翻译？

**A**: 调用接口时传入 `moduleCodes` 参数：

```typescript
fetchGetI18nMessages('zh_CN', 'system,common');
```

### Q4: 插入重复 key 时报什么错？

**A**: 后端会返回 HTTP 409，错误码 `20973`，提示「同语言同模块下消息键已存在」。数据库唯一索引 `uk_i18n_lang_module_key` 提供最终保证。

### Q5: 本地开发时后端不可用？

**A**: 前端本地 `locales/langs/*.ts` 会作为兜底文案，即使后端不可用也能正常显示基础界面。

### Q6: 批量删除会不会影响其它业务缓存？

**A**: 不会。批量删除仅调用 `MultiLevelCacheUtil.evictByPrefix("i18n:messages:")`，不会清空其它业务缓存。

---

## 12. 文件清单

### 12.1 后端

| 路径                                                                                                                                                | 说明         |
|---------------------------------------------------------------------------------------------------------------------------------------------------|------------|
| `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/i18n/controller/SysI18nController.java`                       | REST 接口    |
| `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/i18n/service/SysI18nService.java`                             | 业务逻辑       |
| `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/i18n/manager/SysI18nManager.java`                             | Manager 接口 |
| `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/i18n/manager/impl/SysI18nManagerImpl.java`                    | Manager 实现 |
| `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/i18n/pojo/entity/SysI18nMessage.java`                         | 实体         |
| `carlos-integration/carlos-system/carlos-system-api/src/main/java/com/carlos/system/enums/SystemErrorCode.java`                                   | 错误码        |
| `carlos-integration/carlos-system/docs/02-design/database/system_postgresql.sql`                                                                  | DDL        |
| `carlos-spring-boot/carlos-spring-boot-redis/carlos-spring-boot-starter-redis/src/main/java/com/carlos/redis/multilevel/MultiLevelCacheUtil.java` | 缓存工具       |
| `carlos-integration/carlos-gateway/src/main/resources/application.yml`                                                                            | 网关白名单      |
| `carlos-integration/carlos-gateway/docs/06-deploy/config/application.yml`                                                                         | 部署白名单      |

### 12.2 前端

| 路径                                                                | 说明                     |
|-------------------------------------------------------------------|------------------------|
| `carlos-ui/src/locales/index.ts`                                  | i18n 初始化和后端消息加载        |
| `carlos-ui/src/locales/langs/zh-cn.ts`                            | 中文本地文案                 |
| `carlos-ui/src/locales/langs/en-us.ts`                            | 英文本地文案                 |
| `carlos-ui/src/service/api/i18n.ts`                               | i18n CRUD API          |
| `carlos-ui/src/service/api/app.ts`                                | `fetchGetI18nMessages` |
| `carlos-ui/src/typings/api.d.ts`                                  | i18n 类型定义              |
| `carlos-ui/src/views/manage/i18n/index.vue`                       | i18n 列表页               |
| `carlos-ui/src/views/manage/i18n/modules/i18n-operate-drawer.vue` | 新增/编辑抽屉                |

---

**文档结束**

*最后更新: 2026-08-15*
