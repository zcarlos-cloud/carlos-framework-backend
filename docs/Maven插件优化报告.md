# Carlos Framework Maven 插件优化报告

> 报告日期：2026-06-04
> 执行人：Claude Code
> 涉及模块：`carlos-framework`（根 POM）、`carlos-dependencies`、`carlos-parent`

---

## 一、项目背景

Carlos Framework 采用集中式 Maven 插件版本管理方案：

- `carlos-dependencies`：定义所有插件版本属性，并在 `pluginManagement` 中统一配置插件参数
- `carlos-parent`：继承 `carlos-dependencies`，在 `build/plugins` 中声明实际参与构建生命周期的插件
- 所有子模块：继承 `carlos-parent`，引用插件时无需指定版本号

本次优化基于文档 `docs/Maven插件使用指南.md`，通过扫描全部 `pom.xml` 文件，分析插件配置与实际使用情况的匹配度。

---

## 二、分析方法与依据

### 2.1 分析范围

扫描了项目全部 81 个 `pom.xml` 文件，重点检查以下三个核心 POM：

| POM 文件                        | 角色     | 检查重点                                                |
|-------------------------------|--------|-----------------------------------------------------|
| `pom.xml`（根）                  | 聚合 POM | 是否存在与 `carlos-parent` 冲突的插件声明                       |
| `carlos-dependencies/pom.xml` | BOM    | `pluginManagement` 中插件是否被子模块引用                      |
| `carlos-parent/pom.xml`       | 父 POM  | 哪些插件仅定义在 `pluginManagement` 但未在 `build/plugins` 中激活 |

### 2.2 判断标准

**删除标准（满足任一即删除）：**

1. 插件定义在 `pluginManagement` 中，但项目中没有任何模块（包括子模块 POM）引用它
2. 插件对应的技术栈与项目实际使用的技术栈冲突（如 Flyway vs Liquibase）
3. 插件的所有 execution 均被注释，且无任何子模块使用
4. 插件功能当前无任何实际需求（如 GraalVM 原生镜像编译）

**激活标准（从 `pluginManagement` 提升到 `build/plugins`）：**

1. 插件对项目构建质量有直接价值（如版本强制校验、Javadoc 生成）
2. 插件在当前项目有明确使用场景（如集成测试）
3. 插件属于 CI/CD 流程的标准组成部分

**保留标准：**

1. Maven 核心插件（clean、jar、install、deploy、resources 等），即使未显式声明，也会绑定到默认生命周期，版本锁定可避免 Maven
   使用自带旧版本
2. 被多个子模块实际引用的插件（如 `spring-boot-maven-plugin`）

---

## 三、发现的问题

### 问题 1：根 POM 存在冗余且版本冲突的编译插件

**位置：** `pom.xml:55-69`

根 POM 显式声明了 `maven-compiler-plugin`，版本硬编码为 `3.11.0`：

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.11.0</version>
  ...
</plugin>
```

**问题分析：**

- 根 POM 不是任何子模块的父 POM（子模块继承的是 `carlos-parent`）
- `carlos-parent` 的 `pluginManagement` 中已定义版本 `3.15.0`
- `carlos-parent` 的 `build/plugins` 中已显式引用了该插件
- 版本不一致（`3.11.0` vs `3.15.0`）存在配置冲突风险

**影响：** 根 POM 的编译插件声明实际上不会被任何子模块继承，属于无效配置，且版本落后于 `carlos-parent` 中的定义。

---

### 问题 2：多个插件仅在 `pluginManagement` 中定义，未参与构建生命周期

以下插件仅在 `carlos-dependencies` 的 `pluginManagement` 中锁定版本，未在 `<build><plugins>` 中声明，因此不会主动参与构建：

| 插件                          | 未激活原因                                            |
|-----------------------------|--------------------------------------------------|
| `maven-javadoc-plugin`      | 无 `attach-javadocs` execution，不会自动生成 javadoc jar |
| `maven-failsafe-plugin`     | 无 execution 配置，集成测试功能完全未启用                       |
| `build-helper-maven-plugin` | 所有 execution 被 XML 注释包裹，无任何实际作用                  |
| `maven-enforcer-plugin`     | 仅在 `pluginManagement` 中定义，未在 `build/plugins` 中激活 |

**影响：** 这些插件的配置形同虚设，无法发挥预期作用（如 JDK 版本强制校验、Javadoc 自动生成、集成测试执行等）。

---

### 问题 3：技术栈冲突 — Flyway 与 Liquibase 并存

**位置：** `carlos-dependencies/pom.xml`

`pluginManagement` 中定义了 `flyway-maven-plugin`（版本 `10.15.2`）。

**问题分析：**

- 项目的 `carlos-spring-boot-starter-migration` 模块实际使用的是 **Liquibase**（`liquibase-core` 依赖）
- 该模块的全部代码、配置、README 文档均围绕 Liquibase 编写
- Flyway 作为另一种数据库迁移工具，与项目当前技术栈完全无关

**影响：** 保留 Flyway 插件会造成技术栈混乱，增加维护成本，且误导开发者。

---

### 问题 4：无实际使用场景的插件

| 插件                      | 未使用证据                                                            |
|-------------------------|------------------------------------------------------------------|
| `maven-assembly-plugin` | 项目中无任何 `src/assembly/*.xml` 描述符文件；无任何子模块调用 `mvn assembly:single` |
| `native-maven-plugin`   | 项目无任何 GraalVM 原生镜像编译需求；所有启动模块均为传统 JVM 启动；版本 `0.10.4` 已较旧         |

**影响：** 这些插件仅占用 POM 空间和版本属性，无任何实际产出。

---

## 四、执行的操作

### 4.1 根 `pom.xml`

**操作：** 删除整个 `<build><plugins>` 块中的 `maven-compiler-plugin` 声明。

**依据：**

- 根 POM 为聚合 POM，非父 POM，子模块不继承其 `build` 配置
- `carlos-parent` 已统一管理编译插件，版本为 `3.15.0`
- 避免版本冲突和不必要的重复配置

---

### 4.2 `carlos-dependencies/pom.xml`

#### 4.2.1 删除版本属性

**删除的属性：**

```xml
<!-- 删除前 -->
<native-maven-plugin.version>0.10.4</native-maven-plugin.version>
<flyway.version>10.15.2</flyway.version>
```

**依据：** 对应的插件已被删除，版本属性不再需要。

#### 4.2.2 删除 `pluginManagement` 中的插件

**删除的插件：**

| 插件                          | 删除依据                                     |
|-----------------------------|------------------------------------------|
| `build-helper-maven-plugin` | 全部 execution 被 XML 注释；无任何子模块使用           |
| `flyway-maven-plugin`       | 项目使用 Liquibase，Flyway 为冲突技术栈             |
| `maven-assembly-plugin`     | 无 assembly 描述符；无分发包制作需求                  |
| `maven-enforcer-plugin`     | 移至 `carlos-parent` 的 `build/plugins` 中激活 |
| `native-maven-plugin`       | 无 GraalVM 原生镜像需求                         |

---

### 4.3 `carlos-parent/pom.xml`

#### 4.3.1 新增版本属性

```xml
<maven-enforcer-plugin.version>3.6.2</maven-enforcer-plugin.version>
```

**依据：** `maven-enforcer-plugin` 从 `carlos-dependencies` 移至 `carlos-parent` 激活，需要版本属性支持。

#### 4.3.2 更新 `pluginManagement` 中的插件配置

**更新 `maven-javadoc-plugin`：**

新增 `attach-javadocs` execution，使其在 `package` 阶段自动生成 `*-javadoc.jar`：

```xml
<executions>
  <execution>
    <id>attach-javadocs</id>
    <goals>
      <goal>jar</goal>
    </goals>
  </execution>
</executions>
```

**更新 `maven-failsafe-plugin`：**

新增 `integration-test` 和 `verify` execution，支持 `*IT.java` 集成测试：

```xml
<executions>
  <execution>
    <goals>
      <goal>integration-test</goal>
      <goal>verify</goal>
    </goals>
  </execution>
</executions>
```

**新增 `maven-enforcer-plugin`：**

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <version>${maven-enforcer-plugin.version}</version>
  <executions>
    <execution>
      <id>enforce-versions</id>
      <goals>
        <goal>enforce</goal>
      </goals>
      <configuration>
        <rules>
          <requireJavaVersion>
            <version>[21,)</version>
          </requireJavaVersion>
          <requireMavenVersion>
            <version>[3.8,)</version>
          </requireMavenVersion>
          <banCircularDependencies/>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

#### 4.3.3 在 `build/plugins` 中激活插件

以下插件从仅定义在 `pluginManagement` 中，提升到实际参与构建生命周期：

```xml
<!-- 强制校验 JDK 21+ / Maven 3.8+ -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
</plugin>

<!-- 自动生成并附加 javadoc jar -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-javadoc-plugin</artifactId>
</plugin>

<!-- 执行集成测试 -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-failsafe-plugin</artifactId>
</plugin>
```

**依据：**

- `maven-enforcer-plugin`：确保所有开发者和 CI 环境满足最低 JDK 21 和 Maven 3.8 要求，同时禁止循环依赖
- `maven-javadoc-plugin`：发布到 Nexus 时自动生成 `-javadoc.jar`，便于 IDE 下载和调试
- `maven-failsafe-plugin`：为项目后续编写 `*IT.java` 集成测试提供基础设施

---

## 五、结果总结

### 5.1 变更统计

| 指标                                        | 变更前 | 变更后 | 变化 |
|-------------------------------------------|-----|-----|----|
| 根 POM 冗余插件                                | 1   | 0   | -1 |
| `carlos-dependencies` pluginManagement 插件 | 20  | 15  | -5 |
| 构建生命周期激活插件                                | 2   | 5   | +3 |

### 5.2 删除的插件（5 个）

| 插件                                      | 删除原因                   |
|-----------------------------------------|------------------------|
| `flyway-maven-plugin`                   | 技术栈冲突（项目使用 Liquibase）  |
| `native-maven-plugin`                   | 无 GraalVM 原生镜像需求       |
| `maven-assembly-plugin`                 | 无 assembly 描述符和分发包需求   |
| `build-helper-maven-plugin`             | 全部 execution 被注释，无任何使用 |
| `maven-enforcer-plugin`（从 dependencies） | 移至 parent 中激活          |

### 5.3 激活的插件（3 个）

| 插件                      | 激活效果                                  |
|-------------------------|---------------------------------------|
| `maven-enforcer-plugin` | 构建时强制校验 JDK 21+ / Maven 3.8+ / 禁止循环依赖 |
| `maven-javadoc-plugin`  | `package` 阶段自动生成 `-javadoc.jar`       |
| `maven-failsafe-plugin` | `verify` 阶段执行 `*IT.java` 集成测试         |

### 5.4 保留的核心插件（10 个）

以下插件继续保留在 `pluginManagement` 中，用于版本锁定和子模块引用：

| 插件                         | 保留原因                                 |
|----------------------------|--------------------------------------|
| `maven-compiler-plugin`    | 核心编译插件，已激活                           |
| `maven-source-plugin`      | 核心源码打包插件，已激活                         |
| `maven-surefire-plugin`    | 核心单元测试插件，已激活                         |
| `maven-resources-plugin`   | 核心资源处理插件，已激活                         |
| `maven-jar-plugin`         | 核心打包插件，Maven 默认生命周期隐式执行              |
| `maven-clean-plugin`       | 核心清理插件，Maven 默认生命周期隐式执行              |
| `maven-install-plugin`     | 核心安装插件，Maven 默认生命周期隐式执行              |
| `maven-deploy-plugin`      | 核心部署插件，Maven 默认生命周期隐式执行              |
| `maven-dependency-plugin`  | 用于手动依赖分析命令（如 `mvn dependency:tree`）  |
| `maven-help-plugin`        | 用于手动辅助命令（如 `mvn help:effective-pom`） |
| `spring-boot-maven-plugin` | 被多个 boot/cloud 启动模块引用                |
| `flatten-maven-plugin`     | 处理 `${revision}` 占位符，已激活             |
| `versions-maven-plugin`    | 用于手动版本管理命令                           |

### 5.5 预期收益

1. **减少配置冗余**：删除 5 个无用插件和 2 个版本属性，POM 文件更简洁
2. **消除技术栈冲突**：移除 Flyway，避免与 Liquibase 混淆
3. **提升构建质量**：激活 enforcer 插件，强制环境和依赖规范
4. **增强发布能力**：自动附加 javadoc jar，提升 Nexus 发布质量
5. **支持集成测试**：为后续编写 `*IT.java` 测试提供基础设施

---

## 六、后续建议

1. **验证构建**：执行 `mvn clean install` 验证所有模块是否正常构建
2. **更新文档**：同步更新 `docs/Maven插件使用指南.md`，移除已删除插件的章节
3. **考虑添加 `maven-checkstyle-plugin`**：若团队需要代码风格强制校验
4. **考虑添加 `jacoco-maven-plugin`**：若需要自动化测试覆盖率报告
