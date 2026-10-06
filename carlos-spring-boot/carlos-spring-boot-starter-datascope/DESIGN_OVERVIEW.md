# Carlos 数据权限模块 V2 - 设计方案总览

> 本文档汇总了数据权限模块的完整设计方案，包含技术设计、实现示例和对比分析。

---

## 📚 文档导航

| 文档                                                         | 说明           | 推荐阅读顺序 |
|------------------------------------------------------------|--------------|:------:|
| [DESIGN_COMPARISON.md](./DESIGN_COMPARISON.md)             | V1 与 V2 设计对比 |  1️⃣   |
| [TECHNICAL_DESIGN.md](./TECHNICAL_DESIGN.md)               | 完整技术设计文档     |  2️⃣   |
| [IMPLEMENTATION_EXAMPLES.md](./IMPLEMENTATION_EXAMPLES.md) | 核心代码实现示例     |  3️⃣   |

---

## 🎯 设计目标

### 当前痛点

1. **配置繁琐** - `@DataScope` 需要配置5个参数
2. **性能瓶颈** - 无缓存机制，每次查询重复计算
3. **功能单一** - 仅支持行权限，不支持列权限(脱敏)
4. **扩展困难** - 硬编码规则，难以动态调整

### 改进目标

1. **简化配置** - 80%场景只需一个参数
2. **高性能** - 多级缓存 + 规则预编译
3. **多功能** - 行权限 + 列权限(脱敏) + 审计
4. **可扩展** - 插件化架构，支持动态规则

---

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                         API 层 (注解)                             │
├─────────────────────────────────────────────────────────────────┤
│  @DataScope (简化)  @DataMasking (新增)  @DataFilter (新增)       │
├─────────────────────────────────────────────────────────────────┤
│                         Core 层 (核心)                            │
├─────────────────────────────────────────────────────────────────┤
│  DataScopeRuleEngine (规则引擎)                                  │
│  ├── RuleParser (规则解析)                                       │
│  ├── RuleCompiler (规则编译)                                     │
│  └── RuleExecutor (规则执行)                                     │
├─────────────────────────────────────────────────────────────────┤
│                       Strategy 层 (策略)                          │
├─────────────────────────────────────────────────────────────────┤
│  RowScopeStrategy  ColumnScopeStrategy  CustomScopeStrategy      │
├─────────────────────────────────────────────────────────────────┤
│                        Cache 层 (缓存)                            │
├─────────────────────────────────────────────────────────────────┤
│  L1: Caffeine (方法级)  →  L2: Redis (用户级)  →  L3: DB         │
├─────────────────────────────────────────────────────────────────┤
│                      Storage 层 (存储)                            │
├─────────────────────────────────────────────────────────────────┤
│  Annotation (注解)  YAML (配置)  Database (数据库)                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## ✨ 核心亮点

### 1️⃣ 简化配置

```java
// V1: 需要5个参数
@DataScope(
    type = DataScopeType.DEPT_AND_SUB,
    methodPoint = "selectPage",
    caller = UserMapper.class,
    field = "dept_id"
)

// V2: 只需1个参数
@DataScope(dimension = ScopeDimension.DEPT_AND_CHILDREN)
```

### 2️⃣ 多级缓存

```
请求 → L1 Cache (方法级) → L2 Cache (用户级) → DB
          ↓                      ↓
      TTL: 5min              TTL: 30min
      Caffeine               Redis
```

### 3️⃣ 数据脱敏

```java
@GetMapping("/users")
@DataScope(dimension = ScopeDimension.CURRENT_DEPT)
@DataMasking(fields = {
    @DataMasking.Field(name = "phone", type = Type.PHONE),      // 138****8888
    @DataMasking.Field(name = "idCard", type = Type.ID_CARD),  // 110101********1234
    @DataMasking.Field(name = "email", type = Type.EMAIL)      // t***@qq.com
})
public List<User> list() { ... }
```

### 4️⃣ SpEL 表达式

```java
// 复杂权限条件，一行搞定
@DataScope(condition = "@ds.isOwner(#id) or @ds.hasAnyRole('admin', 'manager')")
public Order getOrder(Long id) { ... }
```

---

## 📊 性能对比

| 指标   |  V1   |   V2    |  提升   |
|------|:-----:|:-------:|:-----:|
| 注解解析 | ~5ms  | < 0.1ms |  50x  |
| 权限查询 | 每次DB  |  多级缓存   | 80% ↓ |
| 整体延迟 | ~15ms |  < 5ms  |  3x   |

---

## 🗂️ 权限维度

| 维度                  | 说明      | 适用场景 |
|---------------------|---------|------|
| `CURRENT_USER`      | 仅本人数据   | 个人中心 |
| `CURRENT_DEPT`      | 本部门数据   | 部门管理 |
| `DEPT_AND_CHILDREN` | 本部门及子部门 | 组织架构 |
| `CURRENT_ROLE`      | 本角色数据   | 角色隔离 |
| `CURRENT_REGION`    | 本区域数据   | 区域管理 |
| `DATA_RANGE`        | 指定数据范围  | 特殊场景 |
| `ALL`               | 全部数据    | 管理员  |
| `CUSTOM`            | 自定义规则   | 复杂场景 |

---

## 🔧 快速配置

```yaml
carlos:
  datascope:
    enabled: true
    
    # 缓存配置
    cache:
      local:
        max-size: 1000
        ttl: 5m
      redis:
        enabled: true
        ttl: 30m
    
    # 全局规则
    global-rules:
      - dimension: DEPT_AND_CHILDREN
        tables: [sys_user, sys_order]
    
    # 审计配置
    audit:
      enabled: true
      storage: LOGGER
```

---

## 📅 实施建议

```
Phase 1 (2周): 核心框架搭建
Phase 2 (2周): 功能完善
Phase 3 (1周): 测试验证
Phase 4 (1周): 灰度上线
─────────────────────────
总计: 6周
```

---

## 📖 详细文档

1. **[设计对比](./DESIGN_COMPARISON.md)** - 了解 V1 vs V2 的差异
2. **[技术设计](./TECHNICAL_DESIGN.md)** - 深入了解架构设计
3. **[实现示例](./IMPLEMENTATION_EXAMPLES.md)** - 查看核心代码实现

---

**文档结束**
