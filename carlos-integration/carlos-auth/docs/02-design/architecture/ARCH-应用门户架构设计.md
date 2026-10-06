# 应用门户 — 架构设计

> 版本: v1.0 | 日期: 2026-07-02 | 作者: Carlos

---

## 1. 架构定位

Carlos Portal 是 Carlos Framework 生态的**统一入口层**，位于 IdP 和子应用之间：

```
                    ┌─────────────────────────────┐
                    │        carlos-auth          │
                    │     (IdP + Backend API)     │
                    │                             │
                    │  /oauth2/authorize          │
                    │  /login (统一登录页)         │
                    │  /auth/portal/apps          │
                    │  /auth/app/client/*         │
                    └──────────┬──────────────────┘
                               │
                    ┌──────────┼──────────────────┐
                    │          │                   │
              ┌─────▼────┐ ┌──▼───────┐ ┌────────▼───────┐
              │  Portal  │ │ Admin UI │ │  App N (未来)   │
              │ (入口)   │ │ (管理)   │ │                 │
              └──────────┘ └──────────┘ └─────────────────┘
```

---

## 2. 多应用 SSO 集成流程

### 2.1 首次登录（完整流程）

```
                 Portal           IdP              Admin UI
                   │                │                  │
User ──────────►   │                │                  │
                   │  无Token       │                  │
                   │──── 302 ─────►│                  │
                   │   /oauth2/     │                  │
                   │   authorize    │                  │
                   │                │◄── 无Session     │
                   │                │                  │
                   │                │  显示统一登录页    │
                   │                │◄── 用户输入密码   │
                   │                │                  │
                   │                │  Session Cookie ✓ │
                   │◄── 302 code ──│                  │
                   │                │                  │
                   │  POST /token   │                  │
                   │──────────────►│                  │
                   │◄── tokens ────│                  │
                   │                │                  │
                   │  显示应用列表   │                  │
                   │                │                  │
User clicks ──►   │                │                  │
"管理后台"         │                │                  │
                   │  window.open ──────────────────►  │
                   │                │                  │  无Token
                   │                │◄── 302 ──────── │
                   │                │  /oauth2/auth   │
                   │                │                  │
                   │                │  Session有效!    │
                   │                │── 302 code ───► │
                   │                │                  │  POST /token
                   │                │◄──────────────── │
                   │                │── tokens ──────►│
                   │                │                  │  免密进入 ✓
```

### 2.2 已登录（SSO 静默通过）

```
User → Portal → 本地有Token → 直接展示应用列表
User → 点击应用 → 新标签
  → 子应用检测无本地Token → 跳 IdP
  → IdP Session 有效 → 直接返回 code（无交互）
  → 子应用换Token → 免密进入
```

---

## 3. 访问控制模型

### 3.1 权限层次

```
                    ┌─────────────────────┐
                    │  所有登录用户可见     │  ← app_role_access 无记录
                    ├─────────────────────┤
                    │  角色限制可见         │  ← app_role_access 有记录
                    │  (R_ADMIN, R_SUPER) │     匹配用户角色才可见
                    └─────────────────────┘
```

### 3.2 决策逻辑

```java
// 伪代码
List<PortalAppVO> filter(apps, userRoles) {
    Set<Long> restrictedAppIds = getAllRestrictedAppIds();  // 有记录的
    Set<Long> userAccessible = getAccessibleByRoles(userRoles);

    return apps.filter(app -> {
        if (!restrictedAppIds.contains(app.id)) return true;  // 无限制
        return userAccessible.contains(app.id);                // 匹配角色
    });
}
```

### 3.3 为什么选择"默认开放"

- **初始部署零配置**：新增应用只需设 `portal_visible=1` 即可全员可见
- **按需收紧**：仅对敏感应用（如运维工具）配置角色限制
- **符合最小惊讶原则**：管理员注册一个应用，默认所有人能看到

---

## 4. 部署拓扑

### 4.1 开发环境

```
localhost:5200   → carlos-framework-portal (Vite dev server)
localhost:5173   → carlos-ui (Vite dev server)
localhost:9511   → carlos-auth (Spring Boot)
```

Portal 的 `vite.config.ts` 配置代理：

```typescript
proxy: {
  '/auth': { target: 'http://127.0.0.1:9511' }
}
```

### 4.2 生产环境（推荐）

```
                        ┌─────────────┐
                        │   Nginx     │
                        │ (反向代理)   │
                        ├─────────────┤
portal.carlos.com ────► │ /           │ → Portal 静态文件
                        │ /auth/*     │ → carlos-auth:9511
                        ├─────────────┤
admin.carlos.com ─────► │ /           │ → Admin UI 静态文件
                        │ /auth/*     │ → carlos-auth:9511
                        ├─────────────┤
auth.carlos.com ──────► │ /           │ → carlos-auth:9511
                        └─────────────┘
```

Nginx 示例：

```nginx
server {
    server_name portal.carlos.com;

    location / {
        root /app/carlos-framework-portal/dist;
        try_files $uri $uri/ /index.html;
    }

    location /auth/ {
        proxy_pass http://carlos-auth:9511;
    }

    location /oauth2/ {
        proxy_pass http://carlos-auth:9511;
    }
}
```

---

## 5. 技术选型

| 层面       | 选择             | 理由               |
|----------|----------------|------------------|
| 前端框架     | Vue 3.5        | 与 carlos-ui 保持一致 |
| 构建工具     | Vite 7         | 开发体验 + 构建性能      |
| UI 组件库   | Ant Design Vue | 与 carlos-ui 统一   |
| CSS 方案   | UnoCSS         | 原子化 CSS，轻量高效     |
| 状态管理     | Pinia          | Vue 3 官方推荐       |
| HTTP 客户端 | Axios          | 成熟稳定             |
| 认证方式     | OIDC + PKCE    | OAuth2.1 标准      |

---

## 6. 扩展性设计

### 6.1 未来可能的扩展

- **应用收藏/置顶**：增加 `user_app_favorite` 表
- **最近使用**：前端 localStorage 记录最近打开的应用
- **应用通知徽标**：WebSocket/SSE 推送未读消息数
- **应用搜索增强**：按标签、权限、使用频率搜索
- **应用详情页**：展示应用版本、更新日志、使用文档

### 6.2 集成新子应用的步骤

1. 在 carlos-auth 注册 OAuth2 Client（`/auth/app/client/add`）
2. 填写门户字段（appDesc, homepageUrl, appType, appCategoryId）
3. 设置 `portal_visible = 1`
4. 可选：通过 `/auth/app/client/roles` 配置角色限制
5. 子应用实现 OIDC PKCE 登录流程（参考《新应用接入SSO指南》）

无需修改门户代码，5 分钟内新应用出现在门户中。
