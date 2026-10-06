# carlos-spring-boot-starter-redis

Redis 缓存模块，集成 Redisson 分布式锁和 Caffeine 本地缓存，支持多级缓存和多种序列化方式。

## 特性

- **多种序列化策略**：支持 Jackson（默认）、Fastjson、Kryo、JDK
- **多级缓存**：Caffeine (L1) + Redis (L2)
- **分布式锁**：基于 Redisson
- **缓存监控**：支持 Micrometer 指标收集
- **安全序列化**：防止反序列化漏洞

## 序列化方案对比

| 序列化方式        | 性能    | 空间占用  | 可读性   | 适用场景           |
|--------------|-------|-------|-------|----------------|
| **Jackson**  | ⭐⭐⭐   | ⭐⭐⭐   | ⭐⭐⭐⭐⭐ | 默认方案，可读性好，调试方便 |
| **Fastjson** | ⭐⭐⭐⭐  | ⭐⭐⭐   | ⭐⭐⭐⭐⭐ | 性能较好，兼容性好      |
| **Kryo**     | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐    | 高性能场景，二进制格式    |
| **JDK**      | ⭐⭐    | ⭐⭐    | ⭐⭐    | 兼容性要求高的场景      |

## 配置示例

### application.yml

```yaml
carlos:
  cache:
    # 序列化方式：jackson、fastjson、kryo、jdk
    serializer: jackson
  
  redis:
    caffeine:
      enabled: true
      initial-capacity: 100
      maximum-size: 10000
      expire-after-write-duration: 10m
    
    lock:
      enabled: true
      prefix: "lock:"
```

### 序列化配置切换

```yaml
# 高性能场景推荐 Kryo
carlos:
  cache:
    serializer: kryo

# 调试场景推荐 Jackson
carlos:
  cache:
    serializer: jackson

# 兼容场景推荐 Fastjson
carlos:
  cache:
    serializer: fastjson
```

## 使用方式

### 基础缓存操作

```java
@Service
public class UserService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // 使用 RedisUtil 工具类
    public User getUser(Long id) {
        String key = "user:" + id;
        
        // 获取缓存
        User user = RedisUtil.getValue(key);
        if (user != null) {
            return user;
        }
        
        // 查询数据库
        user = userMapper.selectById(id);
        
        // 设置缓存（带过期时间）
        RedisUtil.setValue(key, user, 30, TimeUnit.MINUTES);
        
        return user;
    }
}
```

### 多级缓存

```java
@Service
public class ProductService {
    
    public Product getProduct(Long id) {
        String key = "product:" + id;
        
        // 使用多级缓存工具
        return (Product) MultiLevelCacheUtil.get(
            key,
            () -> productMapper.selectById(id),  // 加载函数
            30, TimeUnit.MINUTES                  // L2 过期时间
        );
    }
    
    public void updateProduct(Product product) {
        // 更新数据库
        productMapper.updateById(product);
        
        // 清除多级缓存
        String key = "product:" + product.getId();
        MultiLevelCacheUtil.evict(key);
    }
}
```

### 分布式锁

```java
@Service
public class OrderService {
    
    @RedisLock(name = "createOrder", key = "#userId", expire = 30)
    public Order createOrder(Long userId, OrderParam param) {
        // 防止重复提交
        // ...
    }
}
```

### 序列化器直接调用

```java
@Autowired
private RedisSerializerStrategy serializerStrategy;

public void testSerializer() {
    User user = new User();
    user.setName("test");
    
    // 序列化
    byte[] bytes = serializerStrategy.serialize(user);
    
    // 反序列化
    User deserialized = serializerStrategy.deserialize(bytes, User.class);
}
```

## 注意事项

### 序列化安全

- **Jackson/Fastjson**：已配置白名单，只允许框架内和 JDK 基础类型的反序列化
- **Kryo**：二进制格式，不可读，版本升级时注意兼容性
- **类变更**：修改类结构后可能导致缓存反序列化失败，建议配合缓存版本号使用

### 多级缓存一致性

- L1 (Caffeine) 过期时间默认短于 L2 (Redis)
- 写入顺序：先写 L2，再写 L1
- 删除顺序：先删 L2，再删 L1
- 防止缓存击穿：使用了双重检查锁

### 性能优化

- **批量操作**：使用 `getValueList`、`setValueList` 等方法进行批量操作
- **Pipeline**：RedisUtil 内部使用 Pipeline 优化批量操作
- **并行处理**：大批量操作使用线程池并行处理

## 监控指标

如果引入了 Micrometer，会自动收集以下指标：

- `carlos.cache.hits`：缓存命中次数
- `carlos.cache.misses`：缓存未命中次数
- `carlos.cache.errors`：缓存错误次数
- `carlos.cache.get`：GET 操作耗时
- `carlos.cache.set`：SET 操作耗时
- `carlos.cache.delete`：DELETE 操作耗时

## 变更日志

### 2026-03-14

- 新增多种序列化策略支持（Jackson、Fastjson、Kryo、JDK）
- 修复 Jackson 反序列化安全漏洞
- 修复 ForkJoinPool 线程池泄露问题
- 修复多级缓存数据一致性问题
- 修复 CacheProperties 命名问题（userPrefix -> usePrefix）
- 优化 Key 生成器，防止超长 Key
- 统一异常处理策略
- 添加缓存监控指标支持
