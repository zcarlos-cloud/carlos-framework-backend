# Reactive Redis 支持

从 3.0.0 版本开始，`carlos-spring-boot-starter-redis` 模块增加了对 **Reactive Redis** 的支持，专为 Spring Cloud Gateway
等基于 WebFlux 的响应式应用设计。

## 特性

- ✅ 完整的 Reactive Redis 操作支持（Value、Hash、List、Set、ZSet）
- ✅ 与 Lettuce 客户端原生集成，性能优异
- ✅ 防缓存击穿（互斥锁机制）
- ✅ 统一的异常处理，不抛出异常，使用返回值标识
- ✅ 支持多种序列化方式（Jackson、Fastjson、Kryo、JDK）
- ✅ 与现有同步 Redis 配置共享连接池

## 启用 Reactive Redis

Reactive Redis 默认**自动启用**（当类路径存在 `reactor-core` 时）。如需显式控制：

```yaml
carlos:
  cache:
    reactive:
      enabled: true  # 默认 true
```

## 依赖说明

在 Gateway 模块中使用时，只需引入：

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-redis</artifactId>
</dependency>
```

**注意**：由于 `spring-boot-starter-webflux` 是可选依赖，请确保 Gateway 模块已引入 WebFlux。

## 使用方式

### 1. 注入 ReactiveRedisTemplate

```java
@Service
public class MyReactiveService {
    
    @Autowired
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    
    public Mono<String> getData(String key) {
        return reactiveRedisTemplate.opsForValue().get(key)
            .map(Object::toString);
    }
}
```

### 2. 使用 ReactiveRedisUtil 工具类（推荐）

```java
@Component
public class MyGatewayFilter implements GlobalFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        String key = "rate_limit:" + clientIp;
        
        // 使用工具类进行 Reactive 操作
        return ReactiveRedisUtil.incrementValue(key, 1, 60, TimeUnit.SECONDS)
            .flatMap(count -> {
                if (count > 100) {
                    // 限流处理
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    return exchange.getResponse().setComplete();
                }
                return chain.filter(exchange);
            });
    }
}
```

## API 参考

### 通用操作

| 方法                              | 说明          | 返回值             |
|---------------------------------|-------------|-----------------|
| `setExpire(key, timeout, unit)` | 设置过期时间      | `Mono<Boolean>` |
| `getExpire(key)`                | 获取过期时间（秒）   | `Mono<Long>`    |
| `hasKey(key)`                   | 判断 key 是否存在 | `Mono<Boolean>` |
| `delete(key)`                   | 删除 key      | `Mono<Boolean>` |
| `delete(keys)`                  | 批量删除        | `Mono<Long>`    |
| `deletePattern(pattern)`        | 按通配符删除      | `Mono<Long>`    |
| `scanKeys(pattern)`             | 扫描 key      | `Flux<String>`  |

### Value 操作

| 方法                                    | 说明         | 返回值             |
|---------------------------------------|------------|-----------------|
| `setValue(key, value)`                | 设置值（永不过期）  | `Mono<Boolean>` |
| `setValue(key, value, timeout, unit)` | 设置值（带过期时间） | `Mono<Boolean>` |
| `getValue(key)`                       | 获取值        | `Mono<T>`       |
| `getValue(key, loader)`               | 获取值（带缓存加载） | `Mono<T>`       |
| `setIfAbsent(key, value)`             | 不存在时设置     | `Mono<Boolean>` |
| `incrementValue(key, delta)`          | 递增         | `Mono<Long>`    |
| `decrementValue(key, delta)`          | 递减         | `Mono<Long>`    |

### Hash 操作

| 方法                          | 说明             | 返回值                         |
|-----------------------------|----------------|-----------------------------|
| `getHash(key, item)`        | 获取 Hash 字段     | `Mono<T>`                   |
| `getHash(key)`              | 获取整个 Hash      | `Mono<Map<String, Object>>` |
| `setHash(key, item, value)` | 设置 Hash 字段     | `Mono<Boolean>`             |
| `setHash(key, map)`         | 批量设置 Hash      | `Mono<Boolean>`             |
| `deleteHash(key, items)`    | 删除 Hash 字段     | `Mono<Long>`                |
| `hasHashKey(key, item)`     | 判断 Hash 字段是否存在 | `Mono<Boolean>`             |

### List 操作

| 方法                          | 说明      | 返回值             |
|-----------------------------|---------|-----------------|
| `getList(key, start, end)`  | 获取列表范围  | `Mono<List<T>>` |
| `getListSize(key)`          | 获取列表长度  | `Mono<Long>`    |
| `listRightPush(key, value)` | 右侧 push | `Mono<Long>`    |
| `listLeftPush(key, value)`  | 左侧 push | `Mono<Long>`    |
| `listLeftPop(key)`          | 左侧 pop  | `Mono<T>`       |
| `listRightPop(key)`         | 右侧 pop  | `Mono<T>`       |

### Set 操作

| 方法                         | 说明      | 返回值             |
|----------------------------|---------|-----------------|
| `setAdd(key, values)`      | 添加成员    | `Mono<Long>`    |
| `setMembers(key)`          | 获取所有成员  | `Mono<Set<T>>`  |
| `setIsMember(key, member)` | 判断是否是成员 | `Mono<Boolean>` |
| `setSize(key)`             | 获取大小    | `Mono<Long>`    |
| `setRemove(key, values)`   | 移除成员    | `Mono<Long>`    |

### ZSet 操作

| 方法                                  | 说明        | 返回值             |
|-------------------------------------|-----------|-----------------|
| `zSetAdd(key, value, score)`        | 添加成员（带分数） | `Mono<Boolean>` |
| `zSetRange(key, start, end)`        | 按排名范围获取   | `Mono<Set<T>>`  |
| `zSetReverseRange(key, start, end)` | 反向获取      | `Mono<Set<T>>`  |
| `zSetSize(key)`                     | 获取大小      | `Mono<Long>`    |
| `zSetRemove(key, values)`           | 移除成员      | `Mono<Long>`    |

## 防缓存击穿示例

```java
public Mono<User> getUserWithCache(String userId) {
    return ReactiveRedisUtil.getValue(
        "user:" + userId,
        () -> userRepository.findById(userId),  // 数据库查询
        300L,  // 缓存 5 分钟
        TimeUnit.SECONDS
    );
}
```

## 配置说明

Reactive Redis 继承同步 Redis 的所有配置：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    lettuce:
      pool:
        max-active: 8
        max-idle: 8

carlos:
  cache:
    serializer: jackson  # 序列化方式
    reactive:
      enabled: true      # 启用 Reactive 支持
```

## 注意事项

1. **线程安全**：ReactiveRedisUtil 所有方法都是线程安全的
2. **异常处理**：所有操作都已封装异常处理，不会抛出 Redis 异常，失败时返回空值或 false
3. **序列化**：与同步 Redis 使用相同的序列化配置
4. **连接池**：与同步 Redis 共享 Lettuce 连接池配置

## 与同步 Redis 对比

| 特性     | 同步 Redis (RedisUtil)     | Reactive Redis (ReactiveRedisUtil) |
|--------|--------------------------|------------------------------------|
| 返回值类型  | 直接值                      | Mono/Flux                          |
| 适用场景   | 传统 Servlet 应用            | WebFlux/Gateway                    |
| 线程阻塞   | 阻塞 IO                    | 非阻塞 IO                             |
| 连接工厂   | LettuceConnectionFactory | ReactiveRedisConnectionFactory     |
| 性能（并发） | 良好                       | 更优（高并发）                            |
