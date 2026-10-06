# 应用门户 — 详细设计

> 版本: v1.0 | 日期: 2026-07-02 | 作者: Carlos

---

## 1. 数据库设计

### 1.1 扩展 app_client 表

```sql
ALTER TABLE app_client
    ADD COLUMN app_desc VARCHAR(255) DEFAULT NULL COMMENT '应用描述' AFTER app_logo,
    ADD COLUMN app_type TINYINT NOT NULL DEFAULT 1 COMMENT '应用类型(1内部 2外部 3移动)' AFTER app_desc,
    ADD COLUMN app_category_id BIGINT DEFAULT NULL COMMENT '应用分类ID' AFTER app_type,
    ADD COLUMN homepage_url VARCHAR(500) DEFAULT NULL COMMENT '应用首页地址' AFTER app_category_id,
    ADD COLUMN display_order INT DEFAULT 0 COMMENT '门户展示排序' AFTER homepage_url,
    ADD COLUMN portal_visible TINYINT NOT NULL DEFAULT 1 COMMENT '门户可见' AFTER display_order;
```

### 1.2 app_category 表

```sql
CREATE TABLE app_category (
    id            BIGINT      NOT NULL COMMENT '主键',
    category_code VARCHAR(32) NOT NULL COMMENT '分类编码',
    category_name VARCHAR(64) NOT NULL COMMENT '分类名称',
    category_icon VARCHAR(128)         DEFAULT NULL COMMENT '分类图标',
    sort_order    INT                  DEFAULT 0 COMMENT '排序',
    is_deleted    TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_by     BIGINT               DEFAULT NULL,
    create_time   DATETIME             DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT               DEFAULT NULL,
    update_time   DATETIME             DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY idx_category_code (category_code)
) COMMENT = '应用分类';
```

### 1.3 app_role_access 表

```sql
CREATE TABLE app_role_access (
    id          BIGINT      NOT NULL COMMENT '主键',
    app_id      BIGINT      NOT NULL COMMENT '应用ID',
    role_code   VARCHAR(64) NOT NULL COMMENT '角色编码',
    create_by   BIGINT               DEFAULT NULL,
    create_time DATETIME             DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_app_id (app_id),
    INDEX idx_role_code (role_code),
    UNIQUE INDEX idx_unique (app_id, role_code)
) COMMENT = '应用角色访问控制';
```

**访问规则**：

- `app_role_access` 无记录 → 所有登录用户可见
- 有记录 → 仅匹配 role_code 的用户可见

---

## 2. API 契约

### 2.1 GET /auth/portal/apps

**请求头**: `Authorization: Bearer {token}`

**响应**:

```json
{
  "code": "00000",
  "data": [
    {
      "category": {
        "id": 1,
        "code": "admin",
        "name": "管理工具",
        "icon": "carbon:settings"
      },
      "apps": [
        {
          "id": 1001,
          "appKey": "carlos-client",
          "appName": "管理后台",
          "appLogo": "/assets/admin-logo.png",
          "appDesc": "Carlos 系统管理后台",
          "appType": 1,
          "homepageUrl": "http://admin.carlos.com",
          "displayOrder": 0
        }
      ]
    },
    {
      "category": {
        "id": 2,
        "code": "business",
        "name": "业务应用",
        "icon": "carbon:application"
      },
      "apps": []
    }
  ],
  "msg": "success"
}
```

### 2.2 GET /auth/portal/categories

**响应**:

```json
{
  "code": "00000",
  "data": [
    { "id": 1, "code": "admin", "name": "管理工具", "icon": "carbon:settings" },
    { "id": 2, "code": "business", "name": "业务应用", "icon": "carbon:application" },
    { "id": 3, "code": "develop", "name": "开发工具", "icon": "carbon:code" }
  ]
}
```

### 2.3 POST /auth/app/client/roles

**请求体**:

```json
{
  "appId": 1001,
  "roleCodes": ["R_ADMIN", "R_SUPER"]
}
```

### 2.4 GET /auth/app/client/roles/{appId}

**响应**:

```json
{
  "code": "00000",
  "data": ["R_ADMIN", "R_SUPER"]
}
```

---

## 3. 前端组件树

```
carlos-framework-portal/
└── src/
    ├── App.vue
    ├── main.ts
    ├── layouts/
    │   └── portal-layout.vue          ← Header + RouterView
    ├── components/
    │   └── portal-header.vue          ← Logo + UserDropdown
    ├── views/
    │   ├── portal/
    │   │   ├── index.vue              ← 门户首页（组合组件）
    │   │   └── modules/
    │   │       ├── app-card.vue       ← 单个应用卡片
    │   │       ├── app-grid.vue       ← 卡片网格（过滤逻辑）
    │   │       └── category-filter.vue ← 分类 Tab 筛选
    │   ├── oauth2-callback/
    │   │   └── index.vue              ← OIDC code→token 交换
    │   └── 404.vue
    ├── router/
    │   ├── index.ts                   ← 路由定义
    │   └── guard.ts                   ← Token 校验 + IdP 跳转
    ├── store/
    │   ├── auth.ts                    ← Token 管理
    │   └── portal.ts                  ← 应用列表 + 分类状态
    ├── service/
    │   ├── request.ts                 ← Axios 封装
    │   └── api/portal.ts             ← 门户 API
    └── utils/
        ├── pkce.ts                    ← PKCE 工具
        └── storage.ts                 ← localStorage 封装
```

---

## 4. 状态管理

### 4.1 Auth Store

```typescript
interface AuthState {
  token: string | null;
  isLogin: boolean;    // computed from token
}
```

### 4.2 Portal Store

```typescript
interface PortalState {
  appGroups: PortalAppGroup[];     // 后端返回的分组数据
  categories: PortalCategory[];    // 分类列表
  activeCategory: string;          // 当前选中分类 ('all' | code)
  searchKeyword: string;           // 搜索关键字
  loading: boolean;
}
```

---

## 5. 缓存策略

| 缓存 Key                        | TTL    | 说明        |
|-------------------------------|--------|-----------|
| `portal:apps:visible`         | 5 min  | 门户可见应用列表  |
| `portal:category:all`         | 30 min | 应用分类列表    |
| `portal:role_access:app:{id}` | 10 min | 单个应用的角色限制 |

**失效时机**：

- 管理端修改应用门户字段 → 清除 `portal:apps:visible`
- 管理端修改分类 → 清除 `portal:category:all`
- 管理端设置应用角色 → 清除对应 `portal:role_access:app:{id}`

---

## 6. 应用卡片交互

```
┌──────────────────────────┐
│  [Logo]  应用名称         │
│          应用描述...       │
│                          │
│  [内部应用]              │
└──────────────────────────┘
     │
     │ click
     ▼
window.open(homepageUrl, '_blank')
     │
     ▼
子应用检测无本地 Token → 跳 IdP
     │
     ▼
IdP 检测 Session Cookie 有效 → 静默下发 code
     │
     ▼
子应用回调页换 Token → 免密进入
```
