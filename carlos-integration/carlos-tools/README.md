# Carlos Tools 工具服务模块

> 基于 Swing 的 GUI 桌面工具集 - 代码生成、项目脚手架、加密工具

---

## 核心功能

- **代码生成** - 从 MySQL 表结构生成 MyBatis-Plus / MongoDB / Elasticsearch 代码
- **项目脚手架** - 快速创建 Carlos Framework 标准项目结构
- **加密工具** - SM2/SM4 国密算法加解密、Base64、MD5 等
- **GitLab 集成** - 项目模板管理与代码仓库操作

## 技术栈

- Java Swing + FlatLaf 主题
- Apache Velocity 模板引擎
- MySQL JDBC 元数据读取

## 快速开始

### 启动工具

```bash
mvn exec:java -Dexec.mainClass="com.carlos.fx.ToolsApplication"
```

或在 IDE 中直接运行 `com.carlos.fx.ToolsApplication.main()`

## 文档

- [文档中心](./docs/README.md)

## License

Copyright (c) Carlos Team
