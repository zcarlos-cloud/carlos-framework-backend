# Carlos Framework 框架核心代码扫描报告

**生成时间**: 2026-09-15 20:02 (CST)
**扫描 Commit**: `9dfa0b50`（`docs: 组件特性利用分析报告 2026-09-11`，即当前 HEAD）
**扫描范围**: 框架核心代码层（carlos-dependencies / carlos-parent / carlos-commons / carlos-spring-boot 全部 Starter）
**对比基线**: 上次扫描 `framework-scan-report-2026-09-11-1.md`（commit `94a46a75`）
**发现问题总数**: 20 个（P0: 2 个, P1: 4 个, P2: 11 个, P3: 3 个）——与上次完全一致，无新增/无关闭

---

## 一、执行摘要

### 1.1 今日变更概览

- Git 拉取状态: ✅ 成功（token 认证 fetch，`FETCH_HEAD...HEAD` = `0 0`，远端与本地完全同步）
- Commit: `9dfa0b50`
- 距上次扫描（09-11）提交数: **0 个**（无任何新提交）
- **核心层源码变更: 0 个文件**

### 1.2 源码变更核实

对 `94a46a75`（09-08 扫描基线）→ `9dfa0b50`（当前 HEAD）做全量 diff：

```
git diff --name-only 94a46a75 HEAD
docs/reports/component-analysis-report-2026-09-11-1.md
docs/reports/framework-scan-report-2026-09-11-1.md
docs/reports/framework-scan-unfixed-summary.md
```

仅 3 个 `docs/reports/*.md` 文件变更，**非 docs 源码文件变更 = 0**。`carlos-dependencies / carlos-parent / carlos-commons / carlos-spring-boot` 目录源码持续冻结（自 08-16 前 `3bad6bfa` 起已近 30 天无真实源码变更）。

### 1.3 核心结论

**今日（09-15）核心代码层零源码变更，且自上次扫描（09-11）起无任何新提交。** 本次对 P0/P1 全量问题做 grep 复核（P2/P3 沿用冻结结论），确认 **20 个问题清单与上次完全一致，无新增、无关闭、无回归**。

---

## 二、P0/P1 问题复核（本次重点）

### P0-1 HealthIndicator 非标准包路径残留 ✅ 仍存在（19 文件 / 29 处 import）

`grep -rl "org.springframework.boot.health.contributor"` 命中 **19 个文件 / 29 处 import**，与上次记录精确一致。分布：encrypt(2)、oss(2)、mq(2)、mybatis(2)、spring-cloud-starter(1)、disruptor(2)、flowable(2)、mongodb(2)、redis(2)、audit-bus(2)。

`org.springframework.boot.health.contributor` 为不存在的包，标准包应为 `org.springframework.boot.actuate.health.*`，标准 Spring Boot 环境下无法编译。**已 3 个月未修复**。

### P0-2 Datascope 旧版根包类未清理 ✅ 仍存在

`com/carlos/datascope/` 根包（非子包）下仍残留 10 个旧类：`CustomScope`、`DataScopeAspect`、`DataScopeConsumer`、`DataScopeHandler`、`DataScopeInfo`、`DataScope`、`DataScopeProvider`、`DataScopes`、`DataScopeType`、`DefaultDataScopeProvider`，与新版 `core/aspect/`、`provider/`、`annotation/` 子包路径重复（如根包 `DataScopeAspect` vs `core/aspect/DataScopeAspect`）。

### P1 问题复核

| # | 标题 | 模块 | 复核结果 |
|---|------|------|---------|
| P1-3 | `carlos-spring-boot-starter-minio` 模块完全缺失 | — | ✅ `find carlos-spring-boot -iname "*minio*"` 为空，确认缺失 |
| P1-4 | 全量 Starter 测试覆盖率极低 | 全模块 | ✅ 24 个 Starter 仍仅 encrypt/integration/migration 3 模块有测试类 |
| P1-5 | 全量 Starter 缺失 spring-configuration-metadata.json | 全模块 | ✅ 元数据 JSON 数量仍为 0 |
| P1-6 | SslUtil SSL 校验绕过工具（MITM 风险） | carlos-utils | ✅ `SslUtil.java` 仍存在，无 `@Deprecated` 标记 |

### P2/P3 问题（沿用冻结结论）

P2-7 ~ P2-17、P3-18 ~ P3-20 共 14 项，因核心层源码零变更，沿用 09-11 报告结论，无需重复 grep。

---

## 三、问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 编译/类路径错误 | 1 | 0 | 0 | 0 | 1 |
| 功能缺失 | 0 | 1 | 0 | 0 | 1 |
| 安全漏洞 | 0 | 1 | 2 | 0 | 3 |
| 技术债务/规范违反 | 1 | 2 | 8 | 2 | 13 |
| 文档/日志缺陷 | 0 | 0 | 1 | 1 | 2 |
| **合计** | **2** | **4** | **11** | **3** | **20** |

---

## 四、Starter 健康度评分（与上次一致）

| 模块 | 设计规范 | 功能完整 | 代码质量 | 安全性 | 综合评分 |
|------|---------|---------|---------|--------|---------|
| carlos-spring-boot-core | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.8/5.0 |
| carlos-spring-boot-starter-web | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | 4.1/5.0 |
| carlos-spring-boot-starter-security | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | 3.9/5.0 |
| carlos-spring-boot-starter-mybatis | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.9/5.0 |
| carlos-spring-boot-starter-redis | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.7/5.0 |
| carlos-spring-boot-starter-datascope | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | 2.9/5.0 |
| 其余 Starter（无独立 P0/P1） | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.6/5.0 |

---

## 五、待办事项与修复计划

### 5.1 今日新增待办

无（今日无源码变更）。

### 5.2 历史遗留跟踪（沿用 09-11 清单，均已拖延）

- [ ] **P0-001** 全局替换 HealthIndicator 错误 import（19 文件 / 29 处，一条 sed）⏳ 3 个月未修复
- [ ] **P0-002** 删除 datascope 旧版根包 10 类文件 ⏳ 未修复
- [ ] **P1-003** 确认 minio Starter 设计决策（合并至 oss 或新建）⏳ 待评估
- [ ] **P1-004** 为核心 Starter 补充测试（当前仅 3 模块有测试类）⏳ 筹备中
- [ ] **P1-005** 生成 spring-configuration-metadata.json ⏳ 未修复
- [ ] **P1-006** SslUtil 落地生产禁用方案或标记 @Deprecated ⏳ 未修复（曾修复被 revert）
- [ ] **P2-007** 实现 DefaultDataScopeProvider 默认方法 ⏳ 未修复
- [ ] **P2-017** MultiLevelCacheUtil 移除误导性 TTL 重载 ⏳ 未修复

---

## 六、操作建议

### 紧急 — 本批次修复（强烈建议本周完成，均已拖延 3 个月）

1. **P0 #1**: HealthIndicator 包路径全局替换（19 文件，一条 sed 命令）
2. **P0 #2**: 删除 datascope 旧版 10 个类文件
3. **P1 #6**: SslUtil 重新落地生产禁用方案（查清 revert 原因）

### 计划 — 下批次修复

4. **P1 #4**: 为核心 Starter 补充测试
5. **P1 #5**: 生成配置元数据 JSON
6. **P2 #7** / **P2 #17**: 实现 datascope 默认 Provider、修复 MultiLevelCacheUtil TTL 语义

---

*本报告由 Hermes 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*

*注：本次为「无代码变更」扫描——自上次扫描（2026-09-11）起无任何新提交，全部 20 个问题经 P0/P1 全量 grep 复核 + P2/P3 冻结结论确认，状态不变。*
