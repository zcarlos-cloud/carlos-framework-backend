# 应用门户 — 概要设计

> 版本: v1.0 | 日期: 2026-07-02 | 作者: Carlos

---

## 1. 功能概述

**应用门户（Carlos Portal）** 是 Carlos Framework 的统一应用入口，为用户提供一站式的应用访问体验。

核心能力：

- 用户 SSO 登录后看到其有权访问的所有应用
- 应用按分类分组展示，支持搜索和筛选
- 点击应用卡片跳转到对应系统（SSO 免登）
- 管理员可管理应用元数据、分类和访问权限

---

## 2. 系统边界

```
┌─────────────────────────────────────────────────────────────────┐
│                         用户浏览器                                │
├─────────────────┬──────────────────┬───────────────────────────┤
│  carlos-framework-portal  │   carlos-ui      │   其他子应用 ...           │
│  (门户/工作台)   │  (管理后台)       │                           │
└────────┬────────┴────────┬─────────┴───────────┬───────────────┘
         │                  │                      │
         │  OIDC/PKCE       │  OIDC/PKCE           │ OIDC/PKCE
         ▼                  ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                    carlos-auth (IdP)                              │
│  - 统一登录页 (/login)                                            │
│  - OAuth2 Authorization Server                                    │
│  - 应用管理 API (/auth/app/client/*)                              │
│  - 门户 API (/auth/portal/*)                                      │
│  - 分类管理 API (/auth/app/category/*)                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 部署架构

| 组件                      | 域名（生产）            | 端口（开发） | 说明           |
|-------------------------|-------------------|--------|--------------|
| carlos-auth             | auth.carlos.com   | 9511   | IdP + 后端 API |
| carlos-framework-portal | portal.carlos.com | 5200   | 门户前端         |
| carlos-ui               | admin.carlos.com  | 5173   | 管理后台前端       |

---

## 4. 接口清单

### 4.1 门户接口（面向用户）

| 方法  | 路径                      | 说明                 | 认证           |
|-----|-------------------------|--------------------|--------------|
| GET | /auth/portal/apps       | 当前用户可见的应用列表（按分类分组） | Bearer Token |
| GET | /auth/portal/categories | 应用分类列表             | Bearer Token |

### 4.2 管理接口（面向管理员）

| 方法   | 路径                             | 说明       |
|------|--------------------------------|----------|
| GET  | /auth/app/category/list        | 分类列表     |
| POST | /auth/app/category/add         | 新增分类     |
| POST | /auth/app/category/update      | 更新分类     |
| POST | /auth/app/category/delete      | 删除分类     |
| GET  | /auth/app/client/roles/{appId} | 获取应用可见角色 |
| POST | /auth/app/client/roles         | 设置应用可见角色 |

### 4.3 现有接口（扩展字段）

`POST /auth/app/client/add` 和 `/update` 新增以下字段：

- `appDesc` — 应用描述
- `appType` — 应用类型 (1内部/2外部/3移动)
- `appCategoryId` — 分类ID
- `homepageUrl` — 应用首页地址
- `displayOrder` — 排序
- `portalVisible` — 门户可见

---

## 5. 非功能需求

| 维度  | 要求                                |
|-----|-----------------------------------|
| 性能  | 门户首页加载 < 2s (含 API 响应)            |
| 缓存  | 应用列表 Redis 缓存 5min TTL            |
| 安全  | 所有门户 API 需 Bearer Token 认证        |
| 可用性 | 门户与子应用独立部署，互不影响                   |
| 兼容  | Chrome/Edge/Firefox/Safari 最新两个版本 |

---

## 6. 约束与假设

- SSO 统一认证已完成（carlos-auth 作为 IdP）
- 应用跳转依赖 IdP Session Cookie 实现免登
- 门户本身是一个 OAuth2 Client（client-id: carlos-framework-portal）
- 第一个应用为现有管理后台（carlos-ui, client-id: carlos-client）
