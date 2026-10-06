# Carlos Security Starter

Security Starter，支持接口级别权限控制（@PreAuthorize）。

## 特性

- 从网关Header提取用户信息
- 支持 Spring Security @PreAuthorize 注解
- 多级缓存权限加载（Caffeine + Redis）
- 可扩展的权限提供器架构

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-security</artifactId>
</dependency>
```

### 2. 配置文件

```yaml
carlos:
  resource-server:
    enabled: true
    whitelist:
      - /public/**
      - /api/v1/open/**
    permission:
      provider: redis  # redis / database
      cache:
        enabled: true
        local-ttl: 300  # 本地缓存5分钟
        max-size: 5000
```

### 3. 使用注解

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<User> listAll() {
        return userService.list();
    }

    @PreAuthorize("hasPermission('user', 'read')")
    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PreAuthorize("@permissionEvaluator.isOwner(#id, 'user')")
    @GetMapping("/{id}/detail")
    public UserDetail getDetail(@PathVariable Long id) {
        return userService.getDetail(id);
    }
}
```

## 获取当前用户信息

```java
// 1. 使用 SecurityUtils 工具类
Long userId = SecurityUtils.getCurrentUserId();
String username = SecurityUtils.getCurrentUsername();
UserContext user = SecurityUtils.getCurrentUser();

// 2. 检查权限
if (SecurityUtils.hasPermission("user:update")) {
    // 执行业务逻辑
}

// 3. 获取当前用户权限和角色
Set<String> permissions = SecurityUtils.getCurrentUserPermissions();
Set<Long> roles = SecurityUtils.getCurrentUserRoles();
```

## 多维鉴权（`@ps`）

`@PreAuthorize` 的表达式里可直接引用容器 Bean。框架提供名为 `ps` 的多维鉴权 Bean
（`PermissionChecker`）——与若依的 `@ss` 是同一机制（SpEL 的 `@beanName` 语法）。

### 五个维度

| 维度 | 方法 |
|------|------|
| 功能权限 | `hasPermission` / `hasAnyPermissions` / `hasAllPermissions` |
| 角色 | `hasRole` / `hasAnyRoles` / `hasAllRoles` |
| 身份 | `isSuperAdmin` / `isAuthenticated` / `isSelf` |
| 租户 | `sameTenant` / `hasTenant` |
| 数据（行级） | `canAccessData` / `canAccess` / `hasScope` / `scopeAtLeast` |

### 用法

```java
// 任一权限（OR）
@PreAuthorize("@ps.hasAnyPermissions('org:user:create', 'org:user:update')")

// 全部权限（AND）
@PreAuthorize("@ps.hasAllPermissions('org:user:import', 'org:file:upload')")

// 本人 或 有管理权限
@PreAuthorize("@ps.isSelf(#userId) or @ps.hasPermission('org:user:update')")

// 租户隔离
@PreAuthorize("@ps.sameTenant(#tenantId)")

// 行级：对单条数据判定（实体需实现 ScopedResource）
@PreAuthorize("@ps.canAccess(#order) and @ps.hasPermission('order:update')")
// 等价写法（走 PermissionEvaluator）
@PreAuthorize("hasPermission(#order, 'update')")
```

> 超级管理员（通配权限 `*` 或超管角色）在 `hasPermission` / 行级判定上**一律放行**。

### 数据权限维度

行级判定需要业务模块提供**数据范围规则**。实体实现 `ScopedResource` 暴露归属维度：

```java
public class Order implements ScopedResource {
    private Long ownerId;   // 归属人（「仅本人」维度）
    private Long deptId;    // 归属部门（「本部门 / 本部门及子部门」维度）
    private Long tenantId;  // 归属租户（租户隔离）

    // getOwnerId() / getDeptId() / getTenantId() ...
}
```

框架定义 SPI `DataScopeProvider`，由业务模块实现。carlos-org 已提供 `OrgDataScopeProvider`，
把角色的 `data_scope`（全部 / 本部门及子部门 / 仅本部门 / 仅本人 / 自定义）翻译成行级可访问性：

```java
Integer scope = dataScopeProvider.currentScopeCode();
boolean allowed = dataScopeProvider.canAccessData(ownerId, deptId, tenantId);
```

未提供实现时（如未引入 org 模块的轻量服务），数据维度方法**一律保守拒绝**，不会因缺规则而误放行。

> ⚠️ `hasPermission(targetId, targetType, permission)` 这个 4 参重载（只给 ID 不给实体）
> 目前仍**一律拒绝**——无法从裸 ID 反查归属。请改用上面的 `ScopedResource` 实体重载。

## 权限提供器

### 默认实现

- **RedisPermissionProvider**：从Redis读取权限（默认）
- **CachedPermissionProvider**：带本地缓存的装饰器

### 自定义实现

```java
@Bean
@Primary
public PermissionProvider customPermissionProvider() {
    return new CustomPermissionProvider();
}

public class CustomPermissionProvider implements PermissionProvider {
    
    @Override
    public Set<String> getPermissions(Long roleId) {
        // 自定义权限获取逻辑
        return myPermissionService.getByRoleId(roleId);
    }
}
```

## 权限数据初始化

```java
@Service
public class PermissionInitService {
    
    @Autowired
    private PermissionService permissionService;
    
    public void initRolePermissions(Long roleId, Set<String> permissions) {
        permissionService.updateRolePermissions(roleId, permissions, true);
    }
}
```

## Redis 数据结构

```
Key: auth:permissions:{roleId}
Value: Set<String>
TTL: 7天（可配置）

示例：
auth:permissions:1
  → ["user:read", "user:write", "order:read"]
```

## 缓存同步

当权限变更时，使用 `PermissionService` 更新权限并同步到其他服务实例：

```java
@Service
public class PermissionSyncService {
    
    @Autowired
    private PermissionService permissionService;
    
    public void updatePermissions(Long roleId, Set<String> newPermissions) {
        // 1. 更新数据库（业务逻辑）
        rolePermissionMapper.update(roleId, newPermissions);
        
        // 2. 更新缓存并同步到其他实例
        permissionService.updateRolePermissions(roleId, newPermissions, true);
    }
}
```
