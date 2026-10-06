# carlos-spring-boot-starter-datascope V2

## 简介

Carlos 数据权限模块 V2 是一个基于规则引擎的多维度数据权限控制解决方案。

相比V1版本，V2版本提供了：

- **简化配置** - 80%场景只需一个参数
- **高性能** - 多级缓存（Caffeine + Redis）
- **多功能** - 行权限 + 列权限（脱敏）+ 审计
- **可扩展** - 插件化架构，支持动态规则

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-datascope</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

### 2. 实现 DataScopeProvider

```java
@Component
public class MyDataScopeProvider implements DataScopeProvider {
    
    @Override
    public Serializable getCurrentUserId() {
        // 从SecurityContext获取当前用户ID
        return SecurityUtils.getCurrentUserId();
    }
    
    @Override
    public Serializable getCurrentDeptId() {
        return SecurityUtils.getCurrentDeptId();
    }
    
    @Override
    public Set<Serializable> getDeptChildrenIds(Serializable deptId) {
        // 查询子部门
        return deptService.getChildrenIds(deptId);
    }
    
    // ... 其他方法
}
```

### 3. 配置YAML

```yaml
carlos:
  datascope:
    enabled: true
    cache:
      local:
        max-size: 1000
        ttl: 5m
    # 全局规则
    global-rules:
      - dimension: DEPT_AND_CHILDREN
        tables: [sys_user, sys_order]
        priority: 100
```

### 4. 使用注解

```java
@Service
public class UserService {
    
    // 本部门及子部门数据
    @DataScope(dimension = ScopeDimension.DEPT_AND_CHILDREN)
    public List<User> list() {
        return userMapper.selectList();
    }
    
    // 仅本人数据
    @DataScope(dimension = ScopeDimension.CURRENT_USER)
    public List<Order> listMyOrders() {
        return orderMapper.selectByUserId();
    }
    
    // 使用SpEL表达式
    @DataScope(condition = "@ds.isOwner(#id) or @ds.hasRole('admin')")
    public Order getOrder(Long id) {
        return orderMapper.selectById(id);
    }
    
    // 数据脱敏
    @DataScope(dimension = ScopeDimension.CURRENT_DEPT)
    @DataMasking(fields = {
        @DataMasking.Field(name = "phone", type = DataMasking.Type.PHONE),
        @DataMasking.Field(name = "idCard", type = DataMasking.Type.ID_CARD)
    })
    public List<UserSensitive> listSensitive() {
        return userMapper.selectSensitiveList();
    }
}
```

## 权限维度

| 维度                | 说明      | 适用场景 |
|-------------------|---------|------|
| CURRENT_USER      | 仅本人     | 个人中心 |
| CURRENT_DEPT      | 本部门     | 部门管理 |
| DEPT_AND_CHILDREN | 本部门及子部门 | 组织架构 |
| CURRENT_ROLE      | 本角色     | 角色隔离 |
| CURRENT_REGION    | 本区域     | 区域管理 |
| ALL               | 全部数据    | 管理员  |
| CUSTOM            | 自定义     | 复杂场景 |

## 缓存配置

```yaml
carlos:
  datascope:
    cache:
      # 本地缓存（Caffeine）
      local:
        enabled: true
        max-size: 1000
        ttl: 5m
      # 分布式缓存（Redis）
      redis:
        enabled: true
        ttl: 30m
```

## 审计日志

```yaml
carlos:
  datascope:
    audit:
      enabled: true
      storage: LOGGER  # LOGGER, DATABASE, MQ
      sample-rate: 100
```

## 与V1的区别

| 特性   |  V1  |        V2        |
|------|:----:|:----------------:|
| 注解参数 | 5个必填 |      1个核心参数      |
| 缓存   |  无   | Caffeine + Redis |
| 脱敏   | 不支持  |        支持        |
| SpEL | 不支持  |        支持        |
| 动态规则 | 不支持  |        支持        |

## 迁移指南

从V1迁移到V2：

1. 替换注解导入：`DataScopeType` → `ScopeDimension`
2. 简化注解参数，删除 `caller` 和 `methodPoint`
3. 实现 `DataScopeProvider` 接口
4. 添加YAML配置

---

**版本**: 2.0.0  
**作者**: Carlos
