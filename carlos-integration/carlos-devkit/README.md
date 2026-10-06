# carlos-devkit — 开发工具平台

把原本只在本机跑的 JavaFX 桌面工具（`carlos-integration/carlos-tools`）里的**代码生成器**与**工程生成器**
搬成管理后台功能，并新增**模板管理 / 在线编辑**。模板从「classpath 静态资源」变为「数据库托管 + 版本留痕」，
产物**在线预览 + zip 下载**，不再写服务器业务磁盘。

> 设计文档：`carlos-framework-backend/docs/design/DESIGN-20261006-开发工具平台功能设计.md`
> （库表设计见同目录 `DESIGN-20261006-开发工具平台库表设计.md`）

## 模块结构

```
carlos-integration/carlos-devkit/
├── carlos-devkit-api/      # Feign 契约（一期未使用，为二期「其他服务发起生成」预留）
├── carlos-devkit-bus/      # 全部实现
│   └── src/main/java/com/carlos/devkit/
│       ├── controller/     # /devkit/**（前端）
│       ├── service/(impl)/ # 业务编排
│       ├── manager/(impl)/ # 数据访问（MyBatis-Plus）
│       ├── mapper/         # Mapper 接口
│       ├── convert/        # MapStruct
│       ├── engine/         # ★ 生成引擎（从 carlos-tools 迁入并改造）
│       │   ├── codegen/    # CodeGenEngine（原 CodeGeneratorService）
│       │   ├── projectgen/ # ProjectGenEngine（原 ProjectGeneratorService）
│       │   ├── render/     # FreemarkerRenderer（含安全策略 + 超时 + 限额）
│       │   ├── support/    # 物化 / 路径校验 / JDBC 元数据 / 命名 / 类型映射
│       │   ├── model/      # 引擎领域模型（不依赖持久化实体）
│       │   └── enums/      # [base] / [for] / [ArtifactId] 等占位符指令
│       ├── pojo/           # dto / entity / enums / param / vo
│       └── support/        # 沙箱 / zip / SM4 凭据 / 分页与产物映射
├── carlos-devkit-boot/     # 单体启动（:9520）
└── carlos-devkit-cloud/    # 微服务启动（Nacos）
```

## 功能与接口

网关前缀 `/devkit/**`（`carlos-gateway.yml` 已加路由；注意 `SelectStripPrefixGatewayFilterFactory`
**只对 Swagger 与 `/*/app/enum(s)` 白名单剥前缀，业务路径不剥** —— 所以 Controller 的
`@RequestMapping` 自带 `devkit/` 前缀）。

| 分组 | 代表端点 | 说明 |
|---|---|---|
| 模板组 | `GET /devkit/template/group/page`、`POST .../add|update|delete|copy|toggle-status` | 内置组不可删除，只可停用 |
| 模板文件 | `GET /devkit/template/file/tree`、`GET .../{id}`、`POST .../add|save|rename|delete` | 在线编辑，保存前自动落一版历史 |
| 模板导出 | `GET /devkit/template/file/download?groupId=` | zip 直出（`@NoWrap`） |
| 版本历史 | `GET /devkit/template/version/page`、`GET .../{id}`、`POST .../rollback/{id}` | 回滚前先把当前内容再落一版，回滚本身可回滚 |
| 模板渲染预览 | `POST /devkit/codegen/template-preview` | 用样例表渲染单个模板，在线编辑即时看效果 |
| 数据源 | `GET /devkit/datasource/page|list`、`POST .../add|update|delete|test` | 密码 SM4 加密，**任何接口都不回发密码** |
| 代码生成 | `GET /devkit/codegen/{schemas,tables,columns}`、`POST .../preview`、`POST .../generate` | `generate` 返回 zip（`@NoWrap`） |
| 工程生成 | `GET /devkit/projectgen/template/list`、`POST .../preview`、`POST .../generate` | 同上 |
| 生成记录 | `GET /devkit/record/page`、`POST .../delete` | 只存摘要，不含产物 |

### 产物交付

预览与下载共用同一份内存产物（`GeneratedResult`）；`generate` 直接把它打成 zip 写入响应体。
**不落服务器业务磁盘**：请求级沙箱目录 `${java.io.tmpdir}/carlos-devkit/{uuid}/` 在 `finally` 中整体删除，
启动时清理 1 小时前的残留。

## 安全要点（改动前必读）

1. **模板在线编辑 = 服务端代码执行面**。`FreemarkerRenderer` 每次渲染新建 `Configuration`，并强制：
   `TemplateClassResolver.SAFER_RESOLVER`（禁 `?new`）、`setAPIBuiltinEnabled(false)`（禁 `?api`）、
   输出限额、渲染超时。**这四条是上线前置条件，删任何一条都会退化成 RCE。**
   守护测试：`FreemarkerRendererSecurityTest`。
2. **路径穿越**：模板 `file_path` 由用户编辑，一律经 `TemplatePathValidator` 校验
   （拒绝绝对路径 / 盘符 / `..`），物化与渲染均在基准目录内解析。
   守护测试：`TemplatePathValidatorTest`。
3. **`@NoWrap` 不能漏**：下载端点漏加会把 `byte[]` 包成 `Result` 信封 —— HTTP 200、文件名正确、
   **文件打不开**。参照 carlos-workflow 的导出端点。
4. **数据源口令**：`dev_datasource.password_cipher` 为 SM4 密文（`CredentialCipher`）；
   列表/详情只回 `hasPassword`；更新时留空表示保持原值。生产必须开 `carlos.encrypt.sm4`，
   未开启时 `EncryptUtil` 原样返回即**明文落库**。
5. **只读元数据**：`JdbcMetaReader` 只调 `DatabaseMetaData`，不执行任何用户 SQL。

## 部署与配置

| 项 | 值 |
|---|---|
| 端口 | `9520`（9510-9516 / 9518 / 9519 已占用） |
| 网关路由 | **两处都要改**：`carlos-integration/carlos-gateway/src/main/resources/application.yml`（仓库内兜底）**与** Nacos `common.yml`（运行时生效）。`id: carlos-devkit, Path=/devkit/**, SelectStripPrefix=1` |
| 服务配置 | Nacos `carlos-devkit.yml`（cloud 模式）；boot 模式读模块内 `application.yml` |
| 环境变量 | `CARLOS_DB_URL` / `CARLOS_DB_USERNAME` / `CARLOS_DB_PASSWORD`、`CARLOS_ENCRYPT_KEY` / `CARLOS_ENCRYPT_IV`；另需 Redis（框架基线装配 RedissonClient）：`REDIS_IP` / `REDIS_PORT` / `REDIS_DB`，有密码再加 `SPRING_DATA_REDIS_PASSWORD` |
| 建表 | `docs/db/devkit_dev_postgresql.sql`（5 张 `dev_*` 表） |
| 内置模板 | `docs/db/devkit_seed_templates.sql`（6 组 109 个文件，由 `docs/db/gen-devkit-template-seed.mjs` 生成） |
| 菜单与权限 | `docs/db/devkit_seed_menu_operations.sql`（1 目录 + 6 菜单 + 18 权限点） |

> ⚠️ **网关路由为什么是两处**：网关的 `spring.config.import` 引了 Nacos `common.yml`，而 `common.yml` 里
> 有一份同内容的 routes（为了动态刷新）。导入配置源优先级高于仓库内 `application.yml`，且 **YAML 列表整体替换、不合并** ——
> 只改本地文件重启不生效。Nacos 上的 `carlos-gateway.yml` 是历史遗留 dataId，**未被 import，不生效**。

内置模板的**重新生成**（改了 carlos-tools 模板之后）：

```bash
cd carlos-framework-backend
node docs/db/gen-devkit-template-seed.mjs   # 重新产出 devkit_seed_templates.sql
```

## 前端

`carlos-framework-ui/src/views/devkit/`：`datasource` / `codegen` / `project` / `template` /
`template-editor` / `record`，另有共用的 `components/code-editor.vue`（CodeMirror 6）与
`components/generated-preview.vue`（产物文件树 + 只读代码预览）。

菜单 `component` 必须是 `view.{route_name}`（叶子）/ `layout.base`（一级目录），
`route_name` 由 elegant-router 按 `src/views/devkit/**` 目录自动生成，**两者必须对得上**。

## 测试

父 POM 把 surefire 的 `skipTests` 写死为 `true`，`mvn test` 不会跑。IDE 里跑，或：

```bash
# 编译测试类后，用 junit-platform-launcher 直接执行（脚本见会话记录，核心是）
mvn -o -llr -pl carlos-integration/carlos-devkit/carlos-devkit-bus test-compile
java -cp "target/classes;target/test-classes;<deps>;<junit-platform-launcher>" JUnitRunner <FQCN>...
```

| 测试 | 覆盖 |
|---|---|
| `TemplatePathValidatorTest` | 目录穿越 / 绝对路径 / 盘符 / NUL / 超长 |
| `FreemarkerRendererSecurityTest` | `?new` / `?api` 被拒、输出限额、UTF-8 落盘 |
| `ProjectGenEngineEndToEndTest` | 用真实模板资源跑完整工程生成，断言占位符全部替换 |
| `CodeGenEngineEndToEndIT` | `PG_CONNECTION_STRING` 齐备时，从 `dev_template` 读模板 + 读真实表元数据生成代码 |

## 与 carlos-tools 的关系

`carlos-tools`（JavaFX 桌面）**保持不动**，devkit 以「迁移 + 改造」方式复制其引擎代码：

| 原实现 | devkit | 差异 |
|---|---|---|
| `CodeGeneratorService` | `CodeGenEngine` | 模板来自 DB；产物收集进内存；请求级沙箱 |
| `ProjectGeneratorService` | `ProjectGenEngine` | 同上；渲染统一走本模块 FreemarkerRenderer（顺带修掉 jar 内取不到模板的缺陷） |
| `TemplateUtil` | `FreemarkerRenderer` | 每次渲染新建 Configuration（原静态实例 + `setDirectoryForTemplateLoading` 并发下会互相踩踏）；加安全策略 / 超时 / 限额 |
| `XmlUtils.readConvertXml` | `TypeConverter` | 改从 classpath 读 `devkit/typeConverter.xml` |
| `DatabaseUtil` | `JdbcMetaReader` | 连接改由受管数据源提供；补超时 |
| `ResourceUtil.copyResourceFile2Temp` | `TemplateMaterializer` | 不再解包 classpath，改为 DB → 沙箱 |
| `NameUtil` / 模板资源 | 原样搬 | 保证既有模板产物不发生命名漂移 |

> 一期完成后若确认桌面端下线，可再评估删除 `carlos-tools` 或将其降级为「仅 GitLab 工具」。

## 已知未做（后续）

- `@Log` 操作审计埋点：需要额外引入 `carlos-spring-boot-starter-log`（会带 disruptor / aspectjweaver）与审计 MQ 桥接，
  一期未接，生成/删除等写操作目前只有应用日志。
- 生成产物落 OSS 供二次下载：`dev_generate_record.oss_object_key` 字段已预留。
- 生成后直接推 GitLab 新仓库 / MR（原 `carlos-tools` 的 gitlab4j 能力未迁移）。
- 模板整组导入（zip 上传）：导出已做，导入未做。
- `dev_template_version` 的保留数量清理只在「保存」时触发（无定时任务）。
