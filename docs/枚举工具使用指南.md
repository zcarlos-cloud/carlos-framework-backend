# EnumUtil 枚举工具类 - 使用指南

## 概述

`EnumUtil` 是 Carlos Framework 提供的枚举工具类，用于根据 code 值高效获取枚举实例。

## 核心功能

### 1. 根据 code 获取枚举

```java
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.util.EnumUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum implements BaseEnum {
    NORMAL(1, "正常"),
    LOCKED(2, "锁定"),
    DISABLED(3, "禁用");

    private final Integer code;
    private final String desc;
}

// 使用示例
UserStatusEnum status = EnumUtil.getByCode(UserStatusEnum.class, 1);
if (status != null) {
    System.out.println(status.getDesc()); // 输出：正常
}
```

### 2. 支持多种 Code 类型

```java
// 整数 Code
GenderEnum gender1 = EnumUtil.getByCode(GenderEnum.class, 1);

// 字符串 Code
GenderEnum gender2 = EnumUtil.getByCode(GenderEnum.class, "M");

// 类型转换匹配（"1" 可以匹配到 Integer 的 1）
GenderEnum gender3 = EnumUtil.getByCode(GenderEnum.class, "1");
```

### 3. 缓存机制

`EnumUtil` 使用两级缓存提升性能：

- **一级缓存**：枚举数组缓存
- **二级缓存**：Code 映射缓存

```java
// 首次查询会遍历枚举数组
UserStatusEnum status1 = EnumUtil.getByCode(UserStatusEnum.class, 1);

// 后续查询直接从缓存获取（O(1) 时间复杂度）
UserStatusEnum status2 = EnumUtil.getByCode(UserStatusEnum.class, 1);
```

### 4. 缓存管理

```java
// 清除所有缓存
EnumUtil.clearCache();

// 清除指定枚举类型的缓存
EnumUtil.clearCache(UserStatusEnum.class);
```

## 实际应用场景

### 场景 1：翻译服务中的枚举转换

```java
private Map<String, EnumInfo> translateEnums(
    Map<Class<? extends BaseEnum>, Set<Object>> enumValues) {
    
    Map<String, EnumInfo> result = new HashMap<>();

    for (Map.Entry<Class<? extends BaseEnum>, Set<Object>> entry : enumValues.entrySet()) {
        Class<? extends BaseEnum> enumClass = entry.getKey();
        
        for (Object code : entry.getValue()) {
            // 使用 EnumUtil 根据 code 获取枚举实例
            BaseEnum enumItem = EnumUtil.getByCode(enumClass, code);
            if (enumItem != null) {
                EnumInfo vo = enumItem.getEnumVo();
                String key = enumClass.getName() + ":" + code;
                result.put(key, vo);
            }
        }
    }

    return result;
}
```

### 场景 2：Controller 层参数转换

```java
@GetMapping("/user/status/{code}")
public Result<EnumInfo> getUserStatus(@PathVariable Integer code) {
    UserStatusEnum status = EnumUtil.getByCode(UserStatusEnum.class, code);
    if (status == null) {
        return Result.error("无效的状态码");
    }
    return Result.success(status.getEnumVo());
}
```

### 场景 3：业务逻辑处理

```java
@Service
public class OrderService {
    
    public void processOrder(Integer statusCode) {
        OrderStatusEnum status = EnumUtil.getByCode(OrderStatusEnum.class, statusCode);
        
        switch (status) {
            case PENDING:
                // 待支付处理
                break;
            case PAID:
                // 已支付处理
                break;
            case CANCELLED:
                // 已取消处理
                break;
            default:
                throw new BusinessException("无效的订单状态");
        }
    }
}
```

## 性能优势

| 场景   | 传统方式 | EnumUtil  | 性能提升     |
|------|------|-----------|----------|
| 首次查询 | O(n) | O(n)      | -        |
| 后续查询 | O(n) | O(1)      | 10-100 倍 |
| 内存占用 | -    | ~2KB/枚举类型 | 可忽略      |

## 注意事项

1. **枚举必须实现 BaseEnum 接口**
   ```java
   public enum MyEnum implements BaseEnum {
       // ...
   }
   ```

2. **空值安全**
   ```java
   // 传入 null 会返回 null，不会抛出异常
   MyEnum e1 = EnumUtil.getByCode(null, 1);        // null
   MyEnum e2 = EnumUtil.getByCode(MyEnum.class, null); // null
   ```

3. **异常处理**
   ```java
   try {
       MyEnum e = EnumUtil.getByCode(MyEnum.class, invalidCode);
       if (e == null) {
           log.warn("未找到对应的枚举：code={}", invalidCode);
       }
   } catch (Exception ex) {
       log.error("枚举查询失败", ex);
   }
   ```

## API 速查

| 方法                            | 说明           | 返回值      |
|-------------------------------|--------------|----------|
| `getByCode(Class<T>, Object)` | 根据 code 获取枚举 | T 或 null |
| `clearCache()`                | 清除所有缓存       | void     |
| `clearCache(Class<?>)`        | 清除指定枚举缓存     | void     |

## 最佳实践

✅ **推荐：**

- 在 Service 层使用 EnumUtil 进行枚举转换
- 利用缓存机制避免重复遍历
- 对不存在的 code 进行空值检查

❌ **避免：**

- 频繁调用 `clearCache()`（除非必要）
- 直接访问缓存内部结构
- 在循环中重复创建相同的查询对象

## 技术实现

- **线程安全**: 使用 `ConcurrentHashMap`
- **原子操作**: `computeIfAbsent` 保证并发安全
- **智能匹配**: 支持不同类型 code 的字符串比较
- **日志记录**: DEBUG 级别记录命中情况，WARN 级别记录未找到

## 版本

- v1.0.0 (2026/04/03): 初始版本

---

更多问题请参考框架文档或联系维护团队。
