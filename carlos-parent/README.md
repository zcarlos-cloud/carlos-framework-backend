# Carlos Spring Boot Parent

## 简介

`carlos-spring-boot-parent` 是 Carlos Spring Boot Framework 的统一父 POM，为所有子模块提供统一的构建配置、插件管理和依赖管理。

## 核心功能

### 1. 统一构建配置

- **编译配置**: Java 21，UTF-8 编码
- **资源过滤**: 自动处理 application*.yml/properties 文件中的 Maven 属性
- **源码打包**: 自动生成 source jar
- **版本管理**: 使用 `${revision}` 变量统一管理版本

### 2. 插件管理

提供以下插件的统一版本和配置：

| 插件                       | 版本     | 说明                |
|--------------------------|--------|-------------------|
| maven-compiler-plugin    | 3.11.0 | Java 编译器          |
| maven-source-plugin      | 3.3.0  | 源码打包              |
| maven-javadoc-plugin     | 3.6.3  | Javadoc 生成        |
| maven-surefire-plugin    | 3.2.5  | 单元测试（默认跳过）        |
| maven-failsafe-plugin    | 3.2.5  | 集成测试              |
| maven-jar-plugin         | 3.3.0  | JAR 打包            |
| maven-deploy-plugin      | 3.1.2  | 部署到仓库             |
| flatten-maven-plugin     | 1.6.0  | 处理 ${revision} 变量 |
| spring-boot-maven-plugin | 3.5.9  | Spring Boot 打包    |

### 3. 依赖管理

通过导入 `carlos-spring-boot-dependencies` BOM，所有子模块自动获得统一的依赖版本管理。

### 4. 共享依赖

所有子模块自动继承以下依赖：

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

### 5. Nexus 仓库配置

提供三个 profile 用于不同环境的部署：

| Profile        | 说明    | 仓库地址                       |
|----------------|-------|----------------------------|
| carlos-public  | 公网服务器 | http://zcarlos.com:8081    |
| carlos-private | 内网服务器 | http://192.168.3.30:8081   |
| carlos-yanfa   | 研发服务器 | http://192.168.20.145:8081 |

## 使用方式

### 1. 作为父 POM

在子模块的 `pom.xml` 中声明：

```xml
<parent>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-parent</artifactId>
    <version>${revision}</version>
    <relativePath>../carlos-spring-boot-parent/pom.xml</relativePath>
</parent>
```

### 2. 子模块无需配置

继承 parent 后，子模块自动获得：

- ✅ 统一的编译配置
- ✅ 统一的插件版本
- ✅ 统一的依赖版本
- ✅ 资源过滤规则
- ✅ Lombok 依赖

### 3. 子模块只需声明依赖

```xml
<dependencies>
    <!-- 无需指定版本，由 parent 管理 -->
    <dependency>
        <groupId>com.carlos</groupId>
        <artifactId>carlos-spring-boot-core</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

## 版本管理

### 统一版本变量

使用 `${revision}` 变量统一管理版本：

```xml
<properties>
    <revision>3.0.0-SNAPSHOT</revision>
</properties>
```

### 发布流程

```bash
# 1. 设置发布版本
mvn versions:set -DnewVersion=3.0.0

# 2. 构建和测试
mvn clean install

# 3. 部署到 Nexus（选择对应的 profile）
mvn deploy -P carlos-public

# 4. 提交和打标签
git add .
git commit -m "Release 3.0.0"
git tag v3.0.0

# 5. 设置下一个开发版本
mvn versions:set -DnewVersion=3.1.0-SNAPSHOT
git add .
git commit -m "Prepare for next development iteration"
```

## 资源过滤

Parent 配置了资源过滤，支持在配置文件中使用 Maven 属性：

```yaml
# application.yml
app:
  name: @project.name@
  version: @project.version@
  description: @project.description@
```

构建后会自动替换为实际值。

## 插件配置说明

### flatten-maven-plugin

用于处理 `${revision}` 变量，确保发布到仓库的 POM 文件包含实际版本号而不是变量。

**配置模式**: `resolveCiFriendliesOnly`

### maven-surefire-plugin

默认配置为跳过测试：

```xml
<configuration>
    <skipTests>true</skipTests>
</configuration>
```

如需运行测试，使用：

```bash
mvn test -DskipTests=false
```

## 架构说明

### 依赖管理层次

```
carlos-spring-boot-dependencies (BOM)
    ↓ (import)
carlos-spring-boot-parent (父POM)
    ↓ (继承)
所有子模块
```

### 职责分离

- **BOM (dependencies)**: 纯依赖版本管理
- **Parent (parent)**: 构建配置和插件管理
- **子模块**: 只声明需要的依赖，无需配置版本和插件

## 最佳实践

### 1. 不要覆盖插件版本

子模块应该使用 parent 提供的插件版本，除非有特殊需求。

### 2. 使用 relativePath

确保子模块的 parent 声明中包含正确的 `relativePath`：

```xml
<relativePath>../carlos-spring-boot-parent/pom.xml</relativePath>
```

### 3. 不要重复配置

以下配置由 parent 提供，子模块无需重复：

- ❌ `<dependencyManagement>` 导入 dependencies
- ❌ `<properties>` 中的 `${revision}`
- ❌ 插件配置（compiler, source, jar 等）
- ❌ 编码、Java 版本等属性

### 4. 保持 POM 简洁

子模块的 POM 应该只包含：

- ✅ parent 声明
- ✅ artifactId
- ✅ 模块特定的依赖
- ✅ 模块特定的配置（如果有）

## 常见问题

### Q: 如何查看有效 POM？

```bash
mvn help:effective-pom
```

### Q: 如何覆盖 parent 的配置？

在子模块中重新声明即可覆盖 parent 的配置。

### Q: 如何使用独立版本？

```xml
<parent>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-parent</artifactId>
    <version>${revision}</version>
</parent>

<!-- 覆盖父版本 -->
<version>1.0.0-SNAPSHOT</version>
```

### Q: 为什么需要 flatten-maven-plugin？

Maven 不支持在发布的 POM 中使用 `${revision}` 变量，flatten-maven-plugin 会在构建时生成一个包含实际版本号的 POM 文件。

## 相关文档

- [Carlos Spring Boot Dependencies](../carlos-spring-boot-dependencies/README.md) - BOM 依赖管理
- [Carlos Spring Boot Commons](../carlos-spring-boot-commons/README.md) - 基础套件
- [Carlos Spring Boot Starters](../carlos-spring-boot-starters/README.md) - Spring Boot Starters

## 技术栈

- Maven 3.8+
- JDK 21
- Spring Boot 3.5.9
- Spring Cloud 2025.0.1
- Spring Cloud Alibaba 2025.0.0.0

## 维护者

Carlos Framework Team

## 许可证

内部使用，未开源
