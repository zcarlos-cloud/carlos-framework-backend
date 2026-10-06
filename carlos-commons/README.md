# Carlos Commons

## 简介

`carlos-commons` 是 Carlos Framework 的基础工具模块集合，提供纯工具库，**不依赖 Spring
Boot**，可以在任何 Java 项目中使用。

## 模块列表

| 模块                                     | 说明    | 主要功能       |
|----------------------------------------|-------|------------|
| [carlos-utils](carlos-utils/README.md) | 通用工具库 | 各种工具类和辅助函数 |

## 特点

### ✅ 纯工具库

- 不依赖 Spring Boot
- 不包含自动配置
- 可在任何 Java 21+ 项目中使用

### ✅ 轻量级

- 最小化依赖
- 按需引入
- 无侵入性

### ✅ 高复用

- 提供通用的基础功能
- 被所有 Starter 模块依赖
- 可独立使用

## 快速开始

### 1. 添加依赖

在项目的 `pom.xml` 中添加：

```xml
<dependencies>
    <!-- 工具类 -->
    <dependency>
        <groupId>com.carlos</groupId>
        <artifactId>carlos-utils</artifactId>
        <version>3.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### 2. 使用示例

#### 统一响应封装

```java
import com.carlos.core.response.Result;

@RestController
public class UserController {

    @GetMapping("/user/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        return Result.success(user);
    }

    @PostMapping("/user")
    public Result<Void> createUser(@RequestBody User user) {
        userService.save(user);
        return Result.success();
    }
}
```

#### 分页查询

```java
import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamPage;

@Service
public class UserService {

    public Paging<UserVO> listUsers(ParamPage param) {
        Page<User> page = userMapper.selectPage(
            new Page<>(param.getPageIndex(), param.getPageSize()),
            null
        );
        return PageConvert.convert(page, UserVO.class);
    }
}
```

#### 异常处理

```java
import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.ErrorCode;

@Service
public class OrderService {

    public void createOrder(Order order) {
        if (order.getAmount() <= 0) {
            throw new ServiceException(StatusCode.PARAM_ERROR, "订单金额必须大于0");
        }
        // 业务逻辑
    }
}
```

## 模块详情

### carlos-utils

通用工具库，提供各种常用工具类和辅助函数。

[查看详细文档](carlos-utils/README.md)

## 版本管理

Commons 模块使用统一的版本号，由 `carlos-parent` 管理：

```xml
<parent>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-parent</artifactId>
    <version>${revision}</version>
</parent>
```

当前版本: `3.0.0-SNAPSHOT`

## 技术栈

- **JDK**: 21
- **Hutool**: 5.8.40
- **Guava**: 33.4.8-jre
- **Lombok**: 自动提供

## 使用场景

### 1. Spring Boot 项目

在 Spring Boot 项目中，Commons 模块通常被 Starter 模块间接依赖：

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-mybatis</artifactId>
</dependency>
<!-- carlos-utils 会被自动引入 -->
```

### 2. 非 Spring Boot 项目

在普通 Java 项目中，可以直接使用 Commons 模块：

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-utils</artifactId>
    <version>3.0.0-SNAPSHOT</version>
</dependency>
```

### 3. 微服务项目

在微服务架构中，Commons 模块提供统一的基础功能：

```
服务A (carlos-utils)
服务B (carlos-utils)
服务C (carlos-utils)
    ↓
统一的工具函数和辅助类
```

## 最佳实践

### 1. 按需引入

只引入需要的模块，避免不必要的依赖：

```xml
<!-- 只需要工具类 -->
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-utils</artifactId>
</dependency>
```

### 2. 统一响应格式

在所有 Controller 中使用 `Result` 统一响应格式：

```java
// ✅ 推荐
@GetMapping("/users")
public Result<List<User>> listUsers() {
    return Result.success(userService.list());
}

// ❌ 不推荐
@GetMapping("/users")
public List<User> listUsers() {
    return userService.list();
}
```

### 3. 统一异常处理

使用框架提供的异常类，配合全局异常处理器：

```java
// 业务异常
throw new ServiceException(StatusCode.BUSINESS_ERROR, "库存不足");

// 数据访问异常
throw new DaoException("数据库连接失败");

// 全局异常
throw new GlobalException(StatusCode.SYSTEM_ERROR, "系统异常");
```

### 4. 使用基础抽象

继承框架提供的基础类：

```java
// Entity
public class User extends BaseEntity {
    // 自动包含 id, createTime, updateTime 等字段
}

// Controller
@RestController
public class UserController extends BaseController {
    // 可以使用父类提供的工具方法
}
```

## 与 Starters 的区别

| 特性        | Commons    | Starters       |
|-----------|------------|----------------|
| Spring 依赖 | ❌ 无        | ✅ 有            |
| 自动配置      | ❌ 无        | ✅ 有            |
| 使用场景      | 任何 Java 项目 | Spring Boot 项目 |
| 功能定位      | 基础工具       | 集成框架           |

## 构建

```bash
# 构建所有 Commons 模块
cd carlos-commons
mvn clean install

# 构建单个模块
cd carlos-utils
mvn clean install
```

## 测试

```bash
# 运行测试
mvn test

# 跳过测试
mvn install -DskipTests
```

## 相关文档

- [Carlos Parent](../carlos-parent/README.md) - 统一父 POM
- [Carlos Spring Boot Starters](../carlos-spring-boot/README.md) - Spring Boot Starters
- [Carlos Dependencies](../carlos-dependencies/README.md) - 依赖管理

## 贡献指南

### 添加新的工具类

1. 确定工具类属于哪个模块（utils）
2. 在对应模块中创建工具类
3. 编写单元测试
4. 更新模块的 README.md

### 添加新的 Commons 模块

1. 在 `carlos-commons/` 下创建新模块
2. 在 `carlos-commons/pom.xml` 中添加 `<module>`
3. 新模块的 parent 设置为 `carlos-commons`
4. 在 `carlos-dependencies/pom.xml` 中添加依赖管理
5. 编写模块的 README.md

> **注意**: Commons 模块严禁依赖 Spring Boot，保持与框架无关。需要 Spring Boot 支持的功能请放在 `carlos-spring-boot/` 下的对应 starter 中。

## 维护者

Carlos Framework Team

## 许可证

内部使用，未开源
