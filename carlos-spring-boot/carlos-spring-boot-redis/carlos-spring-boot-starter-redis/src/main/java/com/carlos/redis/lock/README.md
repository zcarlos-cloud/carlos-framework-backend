# Redis 分布式锁使用指南

基于 Redisson 实现的分布式锁，支持注解声明式和编程式两种方式。

## 一、注解式使用（推荐）

### 基础用法

```java
@Service
public class OrderService {

    /**
     * 简单的分布式锁
     * 使用看门狗自动续期，业务执行期间锁不会过期
     */
    @RedisLock(name = "order", key = "#orderId")
    public void processOrder(Long orderId) {
        // 业务逻辑
    }

    /**
     * SpEL 表达式支持
     */
    @RedisLock(name = "user", key = "#user.id + ':' + #type")
    public void updateUser(User user, String type) {
        // 业务逻辑
    }

    /**
     * 指定等待时间和租约时间
     */
    @RedisLock(
        name = "inventory",
        key = "#productId",
        waitTime = 3000,      // 最多等待3秒
        leaseTime = 10000,    // 锁10秒后自动释放
        timeUnit = TimeUnit.MILLISECONDS
    )
    public void deductInventory(Long productId, Integer count) {
        // 业务逻辑
    }
}
```

### 锁类型

```java
@Service
public class DataService {

    /**
     * 公平锁 - 按请求顺序获取锁
     * 适用于排队场景，避免饥饿
     */
    @RedisLock(name = "task", key = "#taskId", lockType = LockType.FAIR)
    public void processTask(Long taskId) {
        // 业务逻辑
    }

    /**
     * 读锁 - 共享锁，支持并发读
     */
    @RedisLock(name = "config", key = "'global'", lockType = LockType.READ)
    public Config getConfig() {
        // 读取配置
        return config;
    }

    /**
     * 写锁 - 独占锁，读写互斥
     */
    @RedisLock(name = "config", key = "'global'", lockType = LockType.WRITE)
    public void updateConfig(Config config) {
        // 更新配置
    }
}
```

### 失败处理策略

```java
@Service
public class PayService {

    /**
     * 快速失败 - 获取不到锁立即抛异常
     */
    @RedisLock(
        name = "pay",
        key = "#orderNo",
        waitTime = 0,                    // 不等待
        onFailure = LockStrategy.FAIL_FAST,
        failMessage = "订单正在处理中，请勿重复提交"
    )
    public void pay(String orderNo) {
        // 支付逻辑
    }

    /**
     * 跳过执行 - 获取不到锁直接返回 null
     * 适用于定时任务等可跳过的场景
     */
    @RedisLock(
        name = "sync",
        key = "#type",
        waitTime = 0,
        onFailure = LockStrategy.SKIP
    )
    public Result syncData(String type) {
        // 同步逻辑
        return result;
    }

    /**
     * 无锁继续 - 获取不到锁也继续执行（带警告日志）
     * 适用于降级场景，需要配合其他幂等机制
     */
    @RedisLock(
        name = "notify",
        key = "#id",
        waitTime = 100,
        onFailure = LockStrategy.CONTINUE
    )
    public void sendNotify(Long id) {
        // 通知逻辑
    }
}
```

## 二、编程式使用

### 基础操作

```java
@Service
@RequiredArgsConstructor
public class StockService {

    public void deductStock(Long productId, int count) {
        String lockName = "stock:" + productId;

        // 尝试获取锁（非阻塞）
        if (RedissonLockUtil.tryLock(lockName)) {
            try {
                // 扣减库存
            } finally {
                RedissonLockUtil.unlock(lockName);
            }
        } else {
            throw new RuntimeException("获取锁失败");
        }
    }

    public void deductStockWithWait(Long productId, int count) {
        String lockName = "stock:" + productId;

        // 尝试获取锁，最多等待3秒
        if (RedissonLockUtil.tryLock(lockName, 3, TimeUnit.SECONDS)) {
            try {
                // 扣减库存
            } finally {
                RedissonLockUtil.unlock(lockName);
            }
        }
    }
}
```

### 模板方法（推荐）

```java
@Service
public class UserService {

    public User getUser(Long userId) {
        // 自动获取和释放锁
        return RedissonLockUtil.executeWithLock(
            "user:" + userId,
            () -> {
                // 查询用户
                return userMapper.selectById(userId);
            }
        );
    }

    public User getUserWithDefault(Long userId) {
        // 获取不到锁时返回默认值
        return RedissonLockUtil.executeWithLock(
            "user:" + userId,
            LockType.REENTRANT,
            100,                    // 等待100ms
            TimeUnit.MILLISECONDS,
            null,                   // 获取不到锁返回 null
            () -> userMapper.selectById(userId)
        );
    }
}
```

### 读写锁

```java
@Service
public class CacheService {

    private static final String LOCK_KEY = "cache:data";

    /**
     * 读取数据（使用读锁）
     */
    public Data getData() {
        if (RedissonLockUtil.tryLock(LOCK_KEY, LockType.READ, 1, TimeUnit.SECONDS)) {
            try {
                return readFromDB();
            } finally {
                RedissonLockUtil.unlock(LOCK_KEY, LockType.READ);
            }
        }
        return null;
    }

    /**
     * 刷新缓存（使用写锁）
     */
    public void refreshCache() {
        if (RedissonLockUtil.tryLock(LOCK_KEY, LockType.WRITE, 5, 30, TimeUnit.SECONDS)) {
            try {
                // 写锁会阻塞所有读写操作
                Data data = loadFromRemote();
                saveToDB(data);
            } finally {
                RedissonLockUtil.unlock(LOCK_KEY, LockType.WRITE);
            }
        }
    }
}
```

## 三、配置属性

```yaml
carlos:
  redis:
    lock:
      # 是否启用分布式锁，默认 true
      enabled: true
      # 锁 key 前缀
      prefix: "redis:lock:"
```

## 四、注意事项

### 1. 锁续期问题

- `leaseTime = -1`（默认）：使用 Redisson 看门狗自动续期，业务执行期间锁不会过期
- `leaseTime > 0`：指定时间后自动释放，即使业务未完成

### 2. key 设计原则

```java
// ✅ 好的实践：业务前缀 + 唯一标识
@RedisLock(name = "order", key = "#orderId")

// ❌ 避免：过于宽泛的 key
@RedisLock(name = "lock", key = "'global'")

// ❌ 避免：动态变化频率过高的 key（内存压力）
@RedisLock(name = "request", key = "#T(java.util.UUID).randomUUID()")
```

### 3. 锁粒度控制

```java
// ✅ 细粒度锁：按用户/订单加锁，并发性能好
@RedisLock(name = "user", key = "#userId")

// ❌ 粗粒度锁：全局锁影响性能
@RedisLock(name = "user", key = "'all'")
```

### 4. 异常处理

```java
// 锁内业务异常会自动释放锁，无需担心
@RedisLock(name = "order", key = "#orderId")
public void process(Order order) {
    // 这里抛异常，锁会自动释放
    if (order.getAmount() <= 0) {
        throw new IllegalArgumentException("金额错误");
    }
}
```

### 5. 避免死锁

```java
// ❌ 错误：在锁内调用另一个带锁的方法（可能导致死锁）
@RedisLock(name = "a", key = "#id")
public void methodA(Long id) {
    methodB(id);  // 内部也有 @RedisLock(name = "b", key = "#id")
}

// ✅ 正确：统一 key 或使用编程式锁的可重入特性
```

## 五、最佳实践

1. **优先使用注解式**：代码简洁，自动管理锁生命周期
2. **合理设置等待时间**：避免无限阻塞，设置合理的 `waitTime`
3. **使用看门狗续期**：除非确定业务执行时间，否则使用默认的自动续期
4. **选择合适的锁类型**：读多写少用读写锁，公平性要求高用公平锁
5. **监控锁等待时间**：关注锁竞争情况，优化业务逻辑或锁粒度
