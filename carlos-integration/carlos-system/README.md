# Carlos System 系统服务模块

> 系统基础数据服务 - 字典管理、参数配置、文件存储、地区数据等

---

## 核心功能

- **字典管理** - 系统字典、业务字典的增删改查与缓存
- **参数配置** - 运行期参数动态配置与热更新
- **文件管理** - 文件上传、下载、预览、分片存储
- **地区数据** - 省市区三级联动数据与行政区划
- **附件存储** - 支持本地、MinIO、OSS 等多存储源

## 模块结构

```
carlos-system/
├── carlos-system-api/        # Feign 接口定义、AO/Param
├── carlos-system-bus/        # 业务实现 (Controller/Service/Manager)
├── carlos-system-cloud/      # Spring Cloud 微服务启动入口
├── carlos-system-boot/       # 单体应用启动入口
└── docs/                     # 文档中心
```

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-system-api</artifactId>
</dependency>
```

### 2. 启动服务

```bash
cd carlos-system-boot
mvn spring-boot:run
```

## 文档

- [文档中心](./docs/README.md)

## License

Copyright (c) Carlos Team
