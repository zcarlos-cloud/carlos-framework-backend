# Carlos Framework Jenkins 批量流水线配置

> **用途**: 使用 Job DSL 插件批量创建 Carlos Framework 所有模块的独立构建流水线

---

## 目录

- [前置要求](#前置要求)
- [快速开始](#快速开始)
- [模块分组](#模块分组)
- [文件说明](#文件说明)
- [常见问题](#常见问题)
- [维护更新](#维护更新)

---

## 前置要求

1. **Jenkins 版本**: 2.401+ (LTS 推荐)
2. **必要插件**:
    - [Job DSL](https://plugins.jenkins.io/job-dsl/)
    - [Pipeline](https://plugins.jenkins.io/workflow-aggregator/)
    - [Config File Provider](https://plugins.jenkins.io/config-file-provider/)
    - [Git](https://plugins.jenkins.io/git/)
3. **工具配置**:
    - JDK: `jdk-21`
    - Maven: `maven-3.9`
    - Node.js: `Node20` (前端构建用，可选)
4. **凭据配置**:
    - Git 凭据 ID: `git-credentials`
    - Maven Settings 文件 ID: `carlos-maven-settings`
    - Nexus 凭据 ID: `nexus-carlos` (已有 Jenkinsfile 使用)

---

## 快速开始

### 步骤 1: 安装 Job DSL 插件

进入 **Manage Jenkins → Plugin Manager → Available**，搜索并安装 **Job DSL** 插件，重启 Jenkins。

### 步骤 2: 创建 Seed Job

1. 点击 **New Item**
2. 输入名称: `carlos-framework-seed`
3. 选择类型: **Pipeline**
4. 点击 **OK**

### 步骤 3: 配置 Seed Job

在 Pipeline 配置区域，选择 **Pipeline script from SCM**：

| 配置项              | 值                                                  |
|------------------|----------------------------------------------------|
| SCM              | Git                                                |
| Repository URL   | `https://github.com/your-org/carlos-framework.git` |
| Credentials      | 选择已配置的 Git 凭据                                      |
| Branch Specifier | `*/main`                                           |
| Script Path      | `deploy/jenkins/Jenkinsfile.seed`                  |

点击 **Save**。

### 步骤 4: 执行 Seed Job

点击 **Build Now**。Seed Job 会执行 `seed.groovy` DSL 脚本，自动创建以下流水线任务：

- `carlos/carlos-framework-deploy` — 全局发布流水线（复用根目录 Jenkinsfile）
- `carlos/foundation/*` — 3 个基础模块流水线
- `carlos/spring-boot/*` — 25 个 Starter 模块流水线
- `carlos/business/*` — 6 个业务模块流水线
- `carlos/integration/*` — 4 个集成模块流水线
- `carlos/samples/*` — 21 个示例项目流水线
- `carlos/frontend/*` — 1 个前端项目流水线
- **视图**: `Carlos Framework` — 汇总视图，含 7 个子视图

**总计: 61 个流水线任务**

### 步骤 5: 查看生成的任务

进入 Jenkins 首页 → 找到 **Carlos Framework** 视图，即可看到按组分类的所有模块流水线。

---

## 模块分组

```
carlos/
├── carlos-framework-deploy          # 全局发布流水线 (deploy 到 Nexus)
│
├── foundation/                       # 基础模块 (3个)
│   ├── carlos-dependencies
│   ├── carlos-parent
│   └── carlos-commons
│
├── spring-boot/                      # Spring Boot Starters (25个)
│   ├── carlos-spring-boot-core
│   ├── carlos-spring-boot-redis
│   ├── carlos-spring-boot-starter-ai
│   ├── carlos-spring-boot-starter-apm
│   ├── ... (共25个)
│   └── carlos-spring-cloud-starter
│
├── business/                         # 业务模块 (6个)
│   ├── carlos-auth
│   ├── carlos-system
│   ├── carlos-org
│   ├── carlos-audit
│   ├── carlos-message
│   └── carlos-gateway
│
├── integration/                      # 第三方集成 (4个)
│   ├── carlos-license-core
│   ├── carlos-license-generate
│   ├── carlos-license-verify
│   └── carlos-tools
│
├── samples/                          # 示例项目 (21个)
│   ├── carlos-sample-apm
│   ├── carlos-sample-cloud
│   ├── ... (共21个)
│   └── carlos-test
│
└── frontend/                         # 前端项目 (1个)
    └── carlos-ui
```

---

## 文件说明

| 文件                                  | 说明                                |
|-------------------------------------|-----------------------------------|
| `deploy/jenkins/Jenkinsfile.seed`   | Seed Job 自身流水线定义，执行 DSL 脚本        |
| `deploy/jenkins/seed.groovy`        | Job DSL 脚本，定义所有模块任务的创建逻辑          |
| `deploy/jenkins/Jenkinsfile.module` | 模块级通用构建模板，所有模块流水线共用               |
| `Jenkinsfile` (根目录)                 | 全局发布流水线，用于 `mvn deploy` 发布到 Nexus |

---

## 模块流水线行为

### Java 模块

每个模块流水线执行以下步骤：

1. **Info** — 打印模块信息
2. **Checkout** — 从 Git 检出代码
3. **Build** — 执行 `mvn clean install -pl {module} -am -DskipTests`
    - `-am` 自动构建该模块依赖的其他模块
4. **Test** — 执行 `mvn test -pl {module}` (仅当存在 `src/test/java` 时)
5. **Post** — 收集 JUnit 测试报告

### 前端模块 (carlos-ui)

1. **Info** — 打印模块信息
2. **Checkout** — 从 Git 检出代码
3. **Frontend Build** — 执行 `pnpm install && pnpm build`
    - 如果未安装 pnpm，自动回退到 `npm`

---

## 触发策略

- **SCM 轮询**: 每 10 分钟检查一次代码变更 (`H/10 * * * *`)
- **手动触发**: 可随时点击 **Build Now**
- **Seed Job 触发**: 修改 `seed.groovy` 后重新执行 Seed Job 即可更新所有任务

---

## 常见问题

### Q1: 如何修改 Git 仓库地址？

编辑 `deploy/jenkins/seed.groovy` 顶部变量：

```groovy
def gitUrl = 'https://github.com/your-org/carlos-framework.git'
def credentialsId = 'your-credentials-id'
```

重新执行 Seed Job 即可。

### Q2: 如何添加新模块？

1. 在 `seed.groovy` 中找到对应的模块列表（如 `businessModules`）
2. 添加新的模块定义：
   ```groovy
   [name: 'carlos-new-module', path: 'carlos-integration/carlos-new-module', desc: '新模块描述']
   ```
3. 提交代码并重新执行 Seed Job

### Q3: 如何删除已废弃的模块流水线？

Seed Job 配置了 `removedJobAction('DELETE')`，只需从 `seed.groovy` 中移除对应模块定义，重新执行 Seed Job 即可自动删除。

### Q4: 模块构建失败如何排查？

1. 点击对应模块流水线的 **Console Output**
2. 查看 `MODULE_NAME` 和 `MODULE_PATH` 是否正确
3. 确认 `carlos-maven-settings` 文件 ID 在 Jenkins 中已配置
4. 检查该模块本地执行 `mvn clean install` 是否正常

### Q5: 只想构建部分模块怎么办？

进入具体模块的流水线配置页，点击 **Build with Parameters**（如果配置了参数化构建），或直接修改该模块的环境变量。

---

## 维护更新

### 新增业务模块

例如新增 `carlos-order` 模块：

```groovy
// 在 seed.groovy 的 businessModules 列表中添加:
[name: 'carlos-order', path: 'carlos-integration/carlos-order', desc: '订单管理模块']
```

### 新增 Starter

例如新增 `carlos-spring-boot-starter-cache`：

```groovy
// 在 seed.groovy 的 springBootModules 列表中添加:
[name: 'carlos-spring-boot-starter-cache', path: 'carlos-spring-boot/carlos-spring-boot-starter-cache', desc: '缓存 Starter']
```

### 修改构建逻辑

如需调整所有模块的构建步骤，修改 `deploy/jenkins/Jenkinsfile.module`，提交后所有模块流水线下次执行时会自动使用新的构建逻辑（因为每个流水线都是从
SCM 加载 Jenkinsfile）。

---

## 参考

- [Job DSL Plugin 文档](https://jenkinsci.github.io/job-dsl-plugin/)
- [Jenkins Pipeline 语法](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [Carlos Framework 开发规范](../../CLAUDE.md)

---

**配置日期**: 2026-05-30
**维护者**: Carlos Team
