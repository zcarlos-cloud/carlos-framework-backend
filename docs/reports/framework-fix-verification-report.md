# Carlos Framework 问题修复验证报告

**报告生成时间**: 2026-06-30  
**修复周期**: P0-P3 全批次  
**原未修复问题总数**: 45 个  
**修复状态**: 全部已完成（含标记为误报/已不存在的项）  

---

## 一、修复总览

| 优先级 | 原数量 | 已修复 |
|--------|--------|--------|
| P0     | 9      | 9      |
| P1     | 14     | 14     |
| P2     | 13     | 13     |
| P3     | 9      | 9      |
| **合计** | **45** | **45** |

---

## 二、P0 级别修复详情

| 原编号 | 标题 | 修复说明 | 验证方式 |
|--------|------|----------|----------|
| #1 | HealthIndicator 包路径错误（32 个文件） | 经核实 Spring Boot 4.0.6 中相关类仍在 `org.springframework.boot.health.contributor`，原报告为误报；代码无需修改 | 源码与 Spring Boot 4.0.6 源码核对 |
| #2 | 遗留 `com.alibaba:fastjson` 与 fastjson2 混用 | `carlos-dependencies` 移除 fastjson1，统一使用 `com.alibaba.fastjson2:fastjson2:2.0.62`；`FastjsonSerializer` 迁移到 fastjson2 API | 全仓库 grep + 编译验证 |
| #3 | Knife4j 4.6.0-SNAPSHOT 替换为 RELEASE | 移除 `com.github.carlos` Fork 的 SNAPSHOT，统一使用 `com.github.xingfudeshi` 社区版 `4.6.0` RELEASE | `mvn clean compile` |
| #4 | SslUtil 完全禁用 SSL 验证 | 重构为默认严格校验，保留可选宽松模式用于测试，默认信任库可配置 | 代码审查 + 单元测试 |
| #5 | 硬编码国密密钥 | `EncryptUtil` / `EncryptConfig` 移除硬编码，改为必须外部配置 | 代码审查 |
| #6 | encrypt.yml 硬编码密钥 | 配置文件示例中的密钥改为占位符/空值，并补充注释说明 | 配置文件审查 |
| #10 | @PreAuthorize 方法级安全默认不激活 | 默认启用 `@EnableMethodSecurity` | 代码审查 + 编译 |
| #12 | UserContextAuthenticationFilter 信任 Header | 增加 IP 白名单、token 签名校验，失败时抛异常而非静默降级 | 代码审查 |
| #14 | XSS JSON 请求绕过 | 增强 `XssHttpServletRequestWrapper` 对 JSON 请求体的白名单与转义策略 | 代码审查 |

---

## 三、P1 级别修复详情

| 原编号 | 标题 | 修复说明 | 验证方式 |
|--------|------|----------|----------|
| #16 | 版本信息不符 | 统一 `AGENTS.md` / `TOOLS.md` / `README.md` 中的版本号与依赖版本 | 文档审查 |
| #17 | ExcelDataImportListener 不调用 saveBatch | 修复导入监听器，导入完成后批量保存 | 代码审查 |
| #18 | isExistField 逻辑反转 | `ExcelUtil.isExistField` 修正返回值逻辑 | 单元测试 |
| #19 | SslUtil 使用废弃 SSL 协议 + 信任所有证书 | 默认使用 TLSv1.2+，移除信任所有证书逻辑 | 代码审查 |
| #21 | AES 禁用 ECB，强制 UTF-8 | SM4/AES 默认使用 CBC/GCM，统一 UTF-8 编码 | 代码审查 |
| #22 | EncryptProperties 密钥明文输出到应用日志 | 日志中不再输出密钥明文 | 代码审查 |
| #23 | EncryptUtil 静态设计缺陷 | 重构为非静态 Bean，支持多密钥配置 | 代码审查 |
| #25 | LocalOssTemplate normalizeObjectName 无路径穿越防护 | 增加路径规范化与穿越检测，抛出 `OssException` | 单元测试 |
| #26 | OssController 预签名 URL 过期时间无上限 | Controller/Service 层增加最大过期时间限制 | 代码审查 |
| #27 | S3Client 无 destroyMethod | S3Client/S3Presigner Bean 增加 `destroyMethod` | 代码审查 + 编译 |
| #28 | S3Client/S3Presigner Bean 缺少生命周期管理 | 增加 `@PreDestroy` 释放资源 | 代码审查 |
| #29 | CarlosPermissionEvaluator.hasPermission 忽略 targetId | 修正权限评估逻辑，使用 targetId 做细粒度判断 | 代码审查 |
| #30 | Header 认证失败时请求静默降级为匿名 | 失败时抛出 `AuthenticationException`，不再降级 | 代码审查 |
| #31 | Snowflake 兜底方案使用固定 workerId=0 | 基于本机 IP 一致性哈希生成 workerId/dataCenterId，避免多实例冲突 | 代码审查 |

---

## 四、P2 级别修复详情

| 原编号 | 标题 | 修复说明 | 验证方式 |
|--------|------|----------|----------|
| #35 | skipTests=true 默认跳过 | `carlos-parent` 移除默认 `skipTests=true`，恢复测试执行 | 编译 + 测试 |
| #36 | ProviderFactoryHolder 中文注释乱码 | 修复为正确 UTF-8 中文注释 | 源码查看 |
| #37 | 断言错误消息字段名错误 | `EncryptProperties` 错误消息字段名修正 | 代码审查 |
| #39 | MinioProperties 日志脱敏 | 敏感配置日志脱敏输出 | 代码审查 |
| #40 | updateBatch 方法缺少空 Wrapper 保护 | `BatchServiceImpl.updateBatch(List, Wrapper)` 增加 `wrapper == null` 保护 | 代码审查 |
| #41 | 预签名 URL expires 无上限 | 配置属性增加最大过期时间校验 | 代码审查 |
| #42 | OssController expires 无 @Max | 参数增加 `@Max` 注解 | 代码审查 |
| #43 | OssObjectView InputStream 未关闭 | 使用 try-with-resources 确保关闭 | 代码审查 |
| #45 | SMS 模块缺少发送限流机制 | 增加基于手机号日/分钟维度限流 | 代码审查 |
| #47 | RequestInfo 日志脱敏 | 敏感字段日志脱敏 | 代码审查 |
| #48 | RequestWrapperFilter Content-Type 默认 JSON | 修正默认 Content-Type 处理逻辑 | 代码审查 |
| #50 | XSS 过滤器 multipart 处理 | 增强 multipart 请求体过滤 | 代码审查 |
| #51 | XssHttpServletRequestWrapper 非 JSON 请求体全局转义 | 限制转义范围，避免误伤普通请求 | 代码审查 |

---

## 五、P3 级别修复详情

| 原编号 | 标题 | 修复说明 | 验证方式 |
|--------|------|----------|----------|
| #52 | POI 版本不统一 | `carlos-dependencies` 显式管理 `poi/poi-ooxml/poi-ooxml-lite` 5.4.0；`fastexcel`/`easyexcel` 排除 POI 传递依赖 | `mvn dependency:tree` |
| #53 | 技术债务 | 清理空壳 TODO；`carlos-audit-bus` 中 Jackson 2 迁移至 Jackson 3 | 编译 + 测试 |
| #54 | JavaDoc 缺失补充 | 为关键公共类/方法补充 JavaDoc | 代码审查 |
| #55 | Fastjson2 2.0.61 版本较旧 | 升级到 `2.0.62` | 依赖树验证 |
| #56 | POI 5.2.5 → 5.4.0 | 升级到 `5.4.0` 并统一版本 | `mvn dependency:tree` |
| #59 | 测试代码包含疑似真实身份证号（PII） | `SM4UtilTest` / `SensitiveDataUtilTest` 替换为明显虚构身份证号 | 单元测试 |
| #60 | sm2 privateIv 拼写错误 | 源码中已不存在 `privateIv` 字段，问题随重构消除 | 全仓库 grep |
| #62 | OssUtil.init 双重 volatile 写无原子性 | `init` 方法改为 `synchronized` | 代码审查 |
| #63 | Snowflake 初始化阶段可能执行 1024 次 Redis SETNX | 改为基于 IP 哈希预占 9 个候选槽位，再顺序探测 | 代码审查 |

---

## 六、验证结论

1. **依赖版本统一**：全仓库已无 `com.alibaba:fastjson`；POI 全 artifact 统一为 `5.4.0`；Knife4j 统一为社区版 `4.6.0` RELEASE。
2. **安全加固**：SSL、加密、认证、XSS、OSS、SMS 等模块均完成对应安全加固。
3. **测试质量**：修复了 `SensitiveDataUtilTest` 中的 Jackson 版本冲突与断言不匹配问题；替换了测试 PII。
4. **编译通过**：涉及的 Starter、Integration、Commons 模块均通过 `mvn clean compile` 验证；相关测试通过 `mvn test` 验证。

---

## 七、后续建议

1. 继续推进全仓库 Jackson 2 → Jackson 3 迁移（当前仅 audit 模块完成）。
2. 建立 CI 流水线，默认执行测试并拒绝 SNAPSHOT 依赖进入生产分支。
3. 定期运行依赖扫描，防止 fastjson1、POI 版本分裂等问题回潮。
