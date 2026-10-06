# Carlos Framework Maven 插件使用指南

> **本文档基于当前项目实际使用的插件配置编写，所有版本号与 `carlos-parent` / `carlos-dependencies` POM 保持一致。**

---

## 一、概述

Carlos Framework 采用**集中式插件版本管理**方案：

- **`carlos-dependencies`**：定义所有插件的版本属性（`<properties>`），并在 `<pluginManagement>` 中统一配置插件参数
- **`carlos-parent`**：继承 `carlos-dependencies`，在 `<build>` 中声明实际参与构建生命周期的插件
- **所有子模块**：继承 `carlos-parent`，引用插件时**无需指定版本号**

### 版本管理速查

```xml
<!-- 在 carlos-parent 或 carlos-dependencies 的 <properties> 中定义 -->
<maven-compiler-plugin.version>3.15.0</maven-compiler-plugin.version>
<maven-surefire-plugin.version>3.5.5</maven-surefire-plugin.version>
<flatten-maven-plugin.version>1.7.3</flatten-maven-plugin.version>
<!-- ... -->
```

---

## 二、插件分类总览

| 分类              | 插件                          | 所在 POM                | 作用                                |
|-----------------|-----------------------------|-----------------------|-----------------------------------|
| **核心编译**        | `maven-compiler-plugin`     | parent                | Java 源码编译（JDK 21 + `-parameters`） |
| **源码/文档**       | `maven-source-plugin`       | dependencies          | 打包源码为 `-sources.jar`              |
|                 | `maven-javadoc-plugin`      | parent                | 打包 Javadoc 为 `-javadoc.jar`       |
| **测试**          | `maven-surefire-plugin`     | dependencies          | 执行单元测试（`*Test.java`）              |
|                 | `maven-failsafe-plugin`     | dependencies          | 执行集成测试（`*IT.java`）                |
| **资源/打包**       | `maven-resources-plugin`    | dependencies          | 资源文件过滤与复制                         |
|                 | `maven-jar-plugin`          | parent                | 主代码打包为 JAR                        |
|                 | `maven-assembly-plugin`     | dependencies          | 制作分发包（含脚本、文档等）                    |
| **生命周期**        | `maven-clean-plugin`        | parent                | 清理 `target/` 目录                   |
|                 | `maven-install-plugin`      | parent                | 安装到本地 Maven 仓库                    |
|                 | `maven-deploy-plugin`       | parent                | 部署到远程 Nexus 仓库                    |
| **Spring Boot** | `spring-boot-maven-plugin`  | dependencies          | Spring Boot 应用打包（含 `repackage`）   |
| **版本管理**        | `flatten-maven-plugin`      | parent + dependencies | 解析 `${revision}` 占位符              |
|                 | `versions-maven-plugin`     | dependencies          | 批量检查/升级依赖版本                       |
| **构建辅助**        | `build-helper-maven-plugin` | dependencies          | 动态添加源码/资源目录                       |
|                 | `maven-help-plugin`         | dependencies          | 查看有效 POM / 系统属性                   |
|                 | `maven-dependency-plugin`   | dependencies          | 依赖树分析                             |
| **规则管理**        | `maven-enforcer-plugin`     | dependencies          | 强制 Java 21+ / Maven 3.8+          |
| **数据库**         | `flyway-maven-plugin`       | dependencies          | 数据库迁移脚本执行                         |
| **原生镜像**        | `native-maven-plugin`       | dependencies          | GraalVM 原生镜像编译（可选）                |

---

## 三、核心构建插件详解

### 3.1 maven-compiler-plugin（Java 编译）

**作用**：将 Java 源码编译为 `.class` 字节码，本项目配置为 **JDK 21**。

**项目配置**（`carlos-parent/pom.xml`）：

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>${maven-compiler-plugin.version}</version>  <!-- 3.15.0 -->
  <configuration>
    <source>${java.version}</source>      <!-- 21 -->
    <target>${java.version}</target>      <!-- 21 -->
    <encoding>${project.build.sourceEncoding}</encoding>
    <!-- 关键：保留方法参数名，用于 Spring Boot 3.x + OpenAPI 识别参数名 -->
    <compilerArgs>
      <arg>-parameters</arg>
    </compilerArgs>
  </configuration>
</plugin>
```

**使用方式**：

- 绑定到 `compile` / `test-compile` 阶段，自动执行
- 手动触发：`mvn compile` 或 `mvn test-compile`

---

### 3.2 maven-source-plugin（源码打包）

**作用**：将项目源码打包为 `*-sources.jar`，发布到 Nexus 供 IDE 下载调试。

**项目配置**（`carlos-dependencies/pom.xml`）：

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-source-plugin</artifactId>
  <version>${maven-source-plugin.version}</version>  <!-- 3.4.0 -->
  <configuration>
    <attach>true</attach>
  </configuration>
  <executions>
    <execution>
      <phase>compile</phase>  <!-- 编译阶段自动执行 -->
      <goals>
        <goal>jar</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

**使用方式**：

- 自动执行：`mvn compile` 时自动生成 `-sources.jar`
- 手动触发：`mvn source:jar`

---

### 3.3 maven-javadoc-plugin（Javadoc 打包）

**作用**：生成 API 文档并打包为 `*-javadoc.jar`，发布到 Nexus。

**项目配置**（`carlos-parent/pom.xml`）：

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-javadoc-plugin</artifactId>
  <version>${maven-javadoc-plugin.version}</version>  <!-- 3.12.0 -->
  <configuration>
    <encoding>UTF-8</encoding>
    <doclint>none</doclint>   <!-- 忽略 @param 缺失等警告 -->
  </configuration>
  <executions>
    <execution>
      <id>attach-javadocs</id>
      <goals>
        <goal>jar</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

**使用方式**：

- 手动触发：`mvn javadoc:jar`
- 在 `deploy` 阶段自动执行（若配置了 `attach-javadocs` execution）

---

### 3.4 maven-resources-plugin（资源处理）

**作用**：处理 `src/main/resources` 和 `src/test/resources` 中的资源文件，支持占位符替换（如 `@revision@`）。

**项目配置**（`carlos-dependencies/pom.xml`）：

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-resources-plugin</artifactId>
  <version>${maven-resources-plugin.version}</version>  <!-- 3.5.0 -->
  <configuration>
    <encoding>${utf8}</encoding>
    <delimiters>
      <delimiter>${resource.delimiter}</delimiter>  <!-- @ -->
    </delimiters>
    <useDefaultDelimiters>false</useDefaultDelimiters>
  </configuration>
</plugin>
```

**项目中的资源过滤配置**：

```xml
<resources>
  <resource>
    <directory>${basedir}/src/main/resources</directory>
    <filtering>true</filtering>
    <includes>
      <include>**/application*.yml</include>
      <include>**/application*.yaml</include>
      <include>**/application*.properties</include>
      <include>**/bootstrap*.yml</include>
    </includes>
  </resource>
  <resource>
    <directory>${basedir}/src/main/resources</directory>
    <excludes>
      <exclude>**/application*.yml</exclude>
      <exclude>**/application*.yaml</exclude>
      <exclude>**/application*.properties</exclude>
      <exclude>**/bootstrap*.yml</exclude>
    </excludes>
  </resource>
</resources>
```

**使用方式**：

- 绑定到 `process-resources` / `process-test-resources` 阶段，自动执行
- 手动触发：`mvn resources:resources`

---

## 四、测试插件详解

### 4.1 maven-surefire-plugin（单元测试）

**作用**：执行 `src/test/java` 目录下的单元测试类（`*Tests.java`）。

**项目配置**（`carlos-dependencies/pom.xml`）：

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <version>${maven-surefire-plugin.version}</version>  <!-- 3.5.5 -->
  <configuration>
    <skipTests>true</skipTests>              <!-- 默认跳过测试 -->
    <testFailureIgnore>true</testFailureIgnore>
    <includes>
      <include>**/*Tests.java</include>
      <include>**/*com.carlos.bbt.test.Test.java</include>
    </includes>
    <excludes>
      <exclude>**/Abstract*.java</exclude>
    </excludes>
    <!-- JUnit 5 并行执行 -->
    <properties>
      <configurationParameters>
        junit.jupiter.execution.parallel.enabled=true
        junit.jupiter.execution.parallel.mode.default=concurrent
      </configurationParameters>
    </properties>
  </configuration>
</plugin>
```

**使用方式**：

```bash
# 执行单元测试（忽略 skipTests 配置）
mvn test

# 执行指定测试类
mvn test -Dtest=ClassName

# 执行指定方法
mvn test -Dtest=ClassName#methodName

# 强制执行（覆盖 skipTests=true）
mvn test -DskipTests=false
```

---

### 4.2 maven-failsafe-plugin（集成测试）

**作用**：执行集成测试类（`*IT.java`），生命周期与 surefire 隔离，支持 `pre-integration-test` / `post-integration-test`
阶段启动/关闭外部容器。

**项目配置**（`carlos-dependencies/pom.xml`）：

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-failsafe-plugin</artifactId>
  <version>${maven-failsafe-plugin.version}</version>  <!-- 3.5.5 -->
  <executions>
    <execution>
      <goals>
        <goal>integration-test</goal>
        <goal>verify</goal>   <!-- 必须，否则阶段失败不中断构建 -->
      </goals>
    </execution>
  </executions>
</plugin>
```

**使用方式**：

```bash
# 执行集成测试（pre-integration-test → integration-test → post-integration-test → verify）
mvn verify

# 仅执行集成测试
mvn failsafe:integration-test
```

**与 surefire 的区别**：

| 特性       | surefire                     | failsafe                      |
|----------|------------------------------|-------------------------------|
| 测试类后缀    | `*Test.java` / `*Tests.java` | `*IT.java`                    |
| 生命周期阶段   | `test`                       | `integration-test` + `verify` |
| 失败是否中断构建 | 是                            | `verify` 阶段才判定                |
| 适用场景     | 纯内存单元测试                      | 需外部服务的集成测试                    |

---

## 五、打包与部署插件

### 5.1 maven-jar-plugin（JAR 打包）

**作用**：将编译后的 `classes/` 打包为 JAR 文件。

**项目配置**：

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-jar-plugin</artifactId>
  <version>${maven-jar-plugin.version}</version>  <!-- 3.5.0 -->
</plugin>
```

**使用方式**：

- 绑定到 `package` 阶段，自动执行
- 手动触发：`mvn jar:jar`

---

### 5.2 spring-boot-maven-plugin（Spring Boot 打包）

**作用**：将 Spring Boot 应用重新打包为可执行 FAT-JAR（内含内嵌容器和所有依赖），并支持分层打包优化 Docker 构建。

**项目配置**（`carlos-dependencies/pom.xml`）：

```xml
<plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
  <version>${spring-boot.version}</version>  <!-- 3.5.9 -->
  <executions>
    <execution>
      <goals>
        <goal>repackage</goal>  <!-- 将普通 JAR 重命名为 *.original，生成可执行 FAT-JAR -->
      </goals>
    </execution>
  </executions>
  <configuration>
    <mainClass>${start-class}</mainClass>
  </configuration>
</plugin>
```

**使用方式**：

```bash
# 构建可执行 JAR（自动触发 repackage）
mvn package

# 手动执行 Spring Boot 打包
mvn spring-boot:repackage

# 直接运行 Spring Boot 应用
mvn spring-boot:run

# 构建 Docker 镜像（分层 JAR）
mvn spring-boot:build-image
```

---

### 5.3 maven-assembly-plugin（分发包制作）

**作用**：将可执行文件、源代码、文档、脚本等集中打包为 ZIP/TAR 分发包。

**项目配置**：

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-assembly-plugin</artifactId>
  <version>${maven-assembly-plugin.version}</version>  <!-- 3.8.0 -->
</plugin>
```

**使用方式**：

```bash
# 使用自定义 assembly 描述符打包
mvn assembly:single
```

---

### 5.4 maven-install-plugin（安装到本地仓库）

**作用**：将构建产物安装到本地 Maven 仓库（`~/.m2/repository`）。

**使用方式**：

- 绑定到 `install` 阶段，自动执行
- 手动触发：`mvn install`

---

### 5.5 maven-deploy-plugin（部署到远程仓库）

**作用**：将构建产物部署到远程 Nexus 仓库。

**使用方式**：

```bash
# 部署到私服（默认 carlos-public profile）
mvn deploy -P carlos-public

# 部署到私有服务器
mvn deploy -P carlos-private
```

**Nexus 仓库地址**（`carlos-parent/pom.xml`）：

| Profile         | Releases                                                      | Snapshots                                                      |
|-----------------|---------------------------------------------------------------|----------------------------------------------------------------|
| `carlos-public` | `http://100.110.168.82:8081/nexus/repository/maven-releases/` | `http://100.110.168.82:8081/nexus/repository/maven-snapshots/` |

---

## 六、版本管理插件详解

### 6.1 flatten-maven-plugin（${revision} 占位符解析）

**作用**：生成扁平化 POM，将 `${revision}` 解析为实际版本号（如 `3.0.0-SNAPSHOT`），避免消费者 POM 中出现未解析的占位符。

**项目配置**（`carlos-parent/pom.xml`）：

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>flatten-maven-plugin</artifactId>
  <version>${flatten-maven-plugin.version}</version>  <!-- 1.7.3 -->
  <configuration>
    <updatePomFile>true</updatePomFile>
    <flattenMode>resolveCiFriendliesOnly</flattenMode>
  </configuration>
  <executions>
    <execution>
      <id>flatten</id>
      <phase>process-resources</phase>
      <goals>
        <goal>flatten</goal>
      </goals>
    </execution>
    <execution>
      <id>flatten.clean</id>
      <phase>clean</phase>
      <goals>
        <goal>clean</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

**为什么需要它**：

Maven 3.5.0+ 支持 `${revision}` 占位符，但发布到仓库的 POM 中如果仍包含占位符，会导致依赖方无法解析版本。
`flatten-maven-plugin` 在 `process-resources` 阶段生成一个**扁平化的 POM**（`.flattened-pom.xml`），其中所有占位符已被替换，并将该文件作为发布的
POM。

**使用方式**：

- 自动执行：`mvn clean` 时清理，`mvn install` 时生成

---

### 6.2 versions-maven-plugin（版本统一管理）

**作用**：批量检查、更新依赖和插件版本，告别手动修改 `pom.xml`。

**项目配置**：

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>versions-maven-plugin</artifactId>
  <version>${versions-maven-plugin.version}</version>  <!-- 2.18.0 -->
  <configuration>
    <generateBackupPoms>false</generateBackupPoms>
  </configuration>
</plugin>
```

**常用命令**：

```bash
# 查看可升级的依赖
mvn versions:display-dependency-updates

# 查看可升级的插件
mvn versions:display-plugin-updates

# 一键升级到最新 RELEASE 版本
mvn versions:use-latest-versions

# 仅升级 release 版本（忽略 snapshot）
mvn versions:use-latest-releases

# 确认修改（移除版本号的 property 覆盖）
mvn versions:commit

# 回滚修改
mvn versions:revert

# 设置项目新版本
mvn versions:set -DnewVersion=3.0.0
```

---

## 七、构建辅助插件

### 7.1 build-helper-maven-plugin（构建辅助）

**作用**：动态扩容源码/资源目录，典型场景：多模块 `generated-sources` 需要加入编译路径，集成 OpenAPI / gRPC / MapStruct
生成代码。

**项目配置**：

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>build-helper-maven-plugin</artifactId>
  <version>${build-helper-maven-plugin.version}</version>  <!-- 3.6.1 -->
  <executions>
    <!-- 示例：添加生成的源码目录 -->
    <!--
    <execution>
      <id>add-generated-source</id>
      <goals>
        <goal>add-source</goal>
      </goals>
      <configuration>
        <sources>
          <source>${project.build.directory}/generated-sources/openapi</source>
        </sources>
      </configuration>
    </execution>
    -->
  </executions>
</plugin>
```

---

### 7.2 maven-dependency-plugin（依赖分析）

**作用**：分析项目依赖树、复制依赖、解包依赖等。

**常用命令**：

```bash
# 查看依赖树
mvn dependency:tree

# 查看依赖树（过滤特定依赖）
mvn dependency:tree -Dincludes=com.baomidou

# 分析未使用/缺失的依赖
mvn dependency:analyze

# 复制所有依赖到指定目录
mvn dependency:copy-dependencies -DoutputDirectory=lib
```

---

### 7.3 maven-help-plugin（辅助工具）

**作用**：查看有效 POM、评估表达式、打印系统属性。

**常用命令**：

```bash
# 查看有效 POM（解析所有继承和变量后的最终 POM）
mvn help:effective-pom

# 查看有效 settings.xml
mvn help:effective-settings

# 评估表达式
mvn help:evaluate -Dexpression=project.version

# 查看所有 profile
mvn help:active-profiles

# 查看系统属性
mvn help:system
```

---

### 7.4 maven-enforcer-plugin（规则管理）

**作用**：创建规则强制团队遵守，包括设定 Java 版本、Maven 版本、禁止循环依赖、禁止 SNAPSHOT 依赖等。

**项目配置**：

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <version>${maven-enforcer-plugin.version}</version>  <!-- 3.6.2 -->
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

**使用方式**：

```bash
# 手动执行规则检查
mvn enforcer:enforce
```

---

## 八、数据库插件

### 8.1 flyway-maven-plugin（数据库迁移）

**作用**：执行数据库初始化/迁移脚本，支持版本化管理。

**项目配置**：

```xml
<plugin>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-maven-plugin</artifactId>
  <version>${flyway.version}</version>  <!-- 10.15.2 -->
</plugin>
```

**使用方式**：

```bash
# 执行迁移
mvn flyway:migrate

# 查看迁移状态
mvn flyway:info

# 验证迁移脚本
mvn flyway:validate

# 创建新的迁移脚本（需配合配置）
mvn flyway:baseline
```

---

## 九、原生镜像插件（可选）

### 9.1 native-maven-plugin（GraalVM 原生镜像）

**作用**：将 Spring Boot 应用编译为 GraalVM 原生可执行文件，启动速度提升 10x+，内存占用降低 50%+。

**项目配置**：

```xml
<plugin>
  <groupId>org.graalvm.buildtools</groupId>
  <artifactId>native-maven-plugin</artifactId>
  <version>${native-maven-plugin.version}</version>  <!-- 0.10.4 -->
</plugin>
```

**使用方式**：

```bash
# 构建原生镜像（需要 GraalVM JDK）
mvn -Pnative native:compile

# 使用 Spring Boot 集成方式构建
mvn spring-boot:build-image -Pnative
```

---

## 十、常用构建命令速查

### 10.1 日常开发

| 命令                                     | 作用                |
|----------------------------------------|-------------------|
| `mvn clean install`                    | 清理并构建所有模块（默认跳过测试） |
| `mvn clean install -P carlos-public`   | 使用公共 Nexus 构建     |
| `mvn clean deploy -P carlos-public`    | 部署到公共 Nexus       |
| `mvn test -Dtest=ClassName#methodName` | 运行指定单元测试          |
| `mvn verify`                           | 执行单元测试 + 集成测试     |

### 10.2 依赖与版本

| 命令                                            | 作用       |
|-----------------------------------------------|----------|
| `mvn dependency:tree`                         | 查看依赖树    |
| `mvn dependency:tree -Dincludes=com.baomidou` | 过滤查看特定依赖 |
| `mvn versions:display-dependency-updates`     | 查看可升级依赖  |
| `mvn versions:display-plugin-updates`         | 查看可升级插件  |
| `mvn help:effective-pom`                      | 查看有效 POM |

### 10.3 代码质量

| 命令                       | 作用         |
|--------------------------|------------|
| `mvn enforcer:enforce`   | 执行构建规则检查   |
| `mvn dependency:analyze` | 分析未使用/缺失依赖 |

---

## 十一、插件版本一览表

> 以下版本号对应 `carlos-dependencies/pom.xml` 中定义的属性值

| 插件                          | 属性名                                 | 当前版本    | 所属组织          |
|-----------------------------|-------------------------------------|---------|---------------|
| `maven-compiler-plugin`     | `maven-compiler-plugin.version`     | 3.15.0  | Apache        |
| `maven-surefire-plugin`     | `maven-surefire-plugin.version`     | 3.5.5   | Apache        |
| `maven-failsafe-plugin`     | `maven-failsafe-plugin.version`     | 3.5.5   | Apache        |
| `maven-javadoc-plugin`      | `maven-javadoc-plugin.version`      | 3.12.0  | Apache        |
| `maven-resources-plugin`    | `maven-resources-plugin.version`    | 3.5.0   | Apache        |
| `maven-jar-plugin`          | `maven-jar-plugin.version`          | 3.5.0   | Apache        |
| `maven-clean-plugin`        | `maven-clean-plugin.version`        | 3.5.0   | Apache        |
| `maven-source-plugin`       | `maven-source-plugin.version`       | 3.4.0   | Apache        |
| `maven-deploy-plugin`       | `maven-deploy-plugin.version`       | 3.1.4   | Apache        |
| `maven-install-plugin`      | `maven-install-plugin.version`      | 3.1.4   | Apache        |
| `maven-dependency-plugin`   | `maven-dependency-plugin.version`   | 3.10.0  | Apache        |
| `maven-enforcer-plugin`     | `maven-enforcer-plugin.version`     | 3.6.2   | Apache        |
| `maven-assembly-plugin`     | `maven-assembly-plugin.version`     | 3.8.0   | Apache        |
| `maven-help-plugin`         | `maven-help-plugin.version`         | 3.5.1   | Apache        |
| `flatten-maven-plugin`      | `flatten-maven-plugin.version`      | 1.7.3   | Codehaus Mojo |
| `versions-maven-plugin`     | `versions-maven-plugin.version`     | 2.18.0  | Codehaus Mojo |
| `build-helper-maven-plugin` | `build-helper-maven-plugin.version` | 3.6.1   | Codehaus Mojo |
| `spring-boot-maven-plugin`  | `spring-boot.version`               | 3.5.9   | Spring        |
| `flyway-maven-plugin`       | `flyway.version`                    | 10.15.2 | Flyway        |
| `native-maven-plugin`       | `native-maven-plugin.version`       | 0.10.4  | GraalVM       |

---

## 十二、注意事项

1. **版本升级策略**：使用 `mvn versions:display-plugin-updates` 定期检查插件更新，但升级前需在 CI 环境验证兼容性。
2. **JDK 21 要求**：`maven-compiler-plugin` 配置为 JDK 21，确保本地和 CI 环境使用兼容版本。
3. **`-parameters` 参数**：编译插件配置了 `-parameters` 参数，用于保留方法参数名，Spring Boot 3.x +
   OpenAPI（Knife4j）依赖此功能正确显示参数名。
4. **测试默认跳过**：`maven-surefire-plugin` 配置 `<skipTests>true</skipTests>`，生产构建时建议显式关闭：
   `mvn clean install -DskipTests=false`。
5. **flatten 插件清理**：`flatten-maven-plugin` 绑定了 `clean` 阶段，执行 `mvn clean` 时会自动删除 `.flattened-pom.xml`。
