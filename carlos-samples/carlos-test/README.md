# carlos-test

测试工具和示例模块，提供框架功能的测试用例和使用示例。

## 模块说明

本模块不是一个可复用的组件，而是用于：

1. **功能测试**: 测试框架各个模块的功能
2. **使用示例**: 展示框架各个模块的使用方法
3. **集成测试**: 测试多个模块的集成场景
4. **性能测试**: 测试框架的性能表现

## 目录结构

```
carlos-test/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/carlos/test/
│   │   │       ├── controller/    # 测试控制器
│   │   │       ├── service/       # 测试服务
│   │   │       ├── entity/        # 测试实体
│   │   │       └── TestApplication.java
│   │   └── resources/
│   │       ├── application.yml    # 完整配置示例
│   │       └── mapper/            # MyBatis映射文件
│   └── test/
│       └── java/                  # 单元测试
└── pom.xml
```

## 包含的测试示例

### 1. 核心功能测试

- 统一响应封装
- 异常处理
- 分页查询
- 枚举处理
- 字典管理

### 2. 数据访问测试

- MyBatis-Plus CRUD
- 多数据源切换
- 数据权限控制
- MongoDB操作
- Redis缓存

### 3. 安全认证测试

- OAuth2认证
- JWT令牌
- 权限控制
- 数据加密（SM2/SM4）

### 4. 第三方集成测试

- 钉钉消息推送
- 融政通集成
- MinIO文件上传
- 短信发送

### 5. 工具组件测试

- Excel导入导出
- JSON序列化
- 雪花算法ID生成
- Magic API接口

## 运行测试应用

### 启动应用

```bash
cd carlos-spring-boot/carlos-test
mvn spring-boot:run
```

### 访问接口

应用启动后，访问以下地址：

- **Swagger文档**: http://localhost:9812/doc.html
- **Magic API**: http://localhost:9812/magic

### 测试接口示例

```bash
# 测试用户查询
curl http://localhost:9812/api/user/list

# 测试文件上传
curl -F "file=@test.txt" http://localhost:9812/api/file/upload

# 测试短信发送
curl -X POST http://localhost:9812/api/sms/send \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","code":"123456"}'
```

## 配置说明

`application.yml` 包含了框架所有模块的配置示例，可以作为实际项目的配置参考。

### 主要配置项

- **数据库配置**: MySQL连接、多数据源
- **Redis配置**: 缓存、分布式锁
- **OAuth2配置**: 认证授权
- **第三方集成**: 钉钉、融政通、MinIO
- **组件配置**: 短信、加密、APM等

## 单元测试

运行单元测试：

```bash
mvn test
```

测试覆盖：

- Service层业务逻辑测试
- Repository层数据访问测试
- Controller层接口测试
- 工具类测试

## 性能测试

使用JMeter或Apache Bench进行性能测试：

```bash
# 使用ab工具测试
ab -n 10000 -c 100 http://localhost:9812/api/user/list
```

## 注意事项

- **本模块仅用于测试，不要部署到生产环境**
- 配置文件中的敏感信息已替换为环境变量引用
- 测试数据库建议使用独立的测试库
- 运行测试前确保依赖的服务（MySQL、Redis等）已启动

## 依赖模块

本模块依赖框架的所有组件，用于集成测试：

- carlos-spring-boot-core
- carlos-spring-boot-starter-mybatis
- carlos-spring-boot-starter-redis
- carlos-auth
- carlos-minio
- carlos-spring-boot-starter-sms
- carlos-spring-boot-starter-docking
- carlos-spring-boot-starter-encrypt
- carlos-spring-boot-starter-json
- carlos-magicapi
- 等等...

## 版本要求

- JDK 21+
- Spring Boot 3.x
- MySQL 8.0+
- Redis 6.0+
