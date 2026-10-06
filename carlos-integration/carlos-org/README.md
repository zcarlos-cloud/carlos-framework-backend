# Carlos Org 组织服务模块

> 组织架构管理中心 - 部门、岗位、人员统一管理与用户中心

---

## 核心功能

- **部门管理** - 组织架构树、部门增删改查、层级关系维护
- **岗位管理** - 岗位定义、岗位与部门关联、岗位权限
- **人员管理** - 员工信息、入职离职、人员与部门/岗位绑定
- **用户中心** - 统一用户管理、用户与组织架构关联
- **组织缓存** - 基于 Caffeine + Redis 的多级组织架构缓存

## 模块结构

```
carlos-org/
├── carlos-org-api/        # Feign 接口定义、AO/Param
├── carlos-org-bus/        # 业务实现 (Controller/Service/Manager)
├── carlos-org-cloud/      # Spring Cloud 微服务启动入口
├── carlos-org-boot/       # 单体应用启动入口
└── docs/                  # 文档中心
```

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-org-api</artifactId>
</dependency>
```

### 2. 启动服务

```bash
cd carlos-org-boot
mvn spring-boot:run
```

## 文档

- [文档中心](./docs/README.md)

## License

Copyright (c) Carlos Team
