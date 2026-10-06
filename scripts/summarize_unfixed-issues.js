#!/usr/bin/env node
// -*- coding: utf-8 -*-
/**
 * 汇总 docs/reports/framework-scan-report-*.md 中标记为未修复的问题。
 * 输出到 docs/reports/framework-scan-unfixed-summary.md，并删除旧报告文件。
 */
const fs = require('fs');
const path = require('path');

const REPORTS_DIR = 'docs/reports';
const OUTPUT_FILE = 'docs/reports/framework-scan-unfixed-summary.md';

function extractDateFromFilename(filename) {
    const base = path.basename(filename);
    const m = base.match(/(20\d{2}-\d{2}-\d{2})/);
    return m ? m[1] : base;
}

function classifyStatus(text, sourceType = 'detail') {
    const t = (text || '').toLowerCase();

    // 明确忽略/无需修复
    if (/无需修复|忽略|不存在|false positive|wontfix/i.test(t)) {
        return 'fixed';
    }

    // 特殊处理："⚠️ 60% 已修复，剩余本周内完成" 视为 fixed
    if (/\d+%\s*已修复/.test(t) && !/未修复/.test(t)) {
        return 'fixed';
    }

    const fixedMarkers = ['已修复', '✅', '部分修复', '完成', 'fixed'];
    const unfixedMarkers = ['未修复', '❌', '⏳', '遗留', '持续', '确认未修复', '今日确认未修复', '仍存在', '确认仍存在'];

    const hasFixed = fixedMarkers.some(m => t.includes(m.toLowerCase()));
    const hasUnfixed = unfixedMarkers.some(m => t.includes(m.toLowerCase()));

    if (hasFixed && !hasUnfixed) return 'fixed';
    if (hasUnfixed && !hasFixed) return 'unfixed';
    if (hasFixed && hasUnfixed) {
        if (/已修复.*未修复/.test(t) || /未修复.*已修复/.test(t)) {
            if (/\d+%/.test(t)) return 'fixed';
        }
        return 'unfixed';
    }

    // 表格和复选框必须明确标记状态
    if (sourceType === 'table' || sourceType === 'checkbox') {
        return 'unknown';
    }

    // 详情段落：如果明确是当天新发现（无历史遗留标记），视为未修复
    if (/新发现|新增|本次扫描发现|今日发现/i.test(t)) {
        return 'unfixed';
    }

    return 'unknown';
}

function isValidTitle(title) {
    if (!title || title.length < 3) return false;
    const t = title.toLowerCase().trim();
    const invalidTitles = [
        '未修复', '⏳ 未修复', '已修复', '无变化', '代码未变更',
        'p0', 'p1', 'p2', 'p3',
        '模块', '标题', '状态', '优先级',
    ];
    if (invalidTitles.includes(t)) return false;
    // 排除纯状态 emoji + 文字
    if (/^[⏳❌✅⚠️🆕🔁\s]+$/.test(t)) return false;
    // 排除纯英文状态短语
    if (/^(unfixed|fixed|todo|done|pending|open|closed)$/i.test(t)) return false;
    return true;
}

function shortStatus(text) {
    if (!text) return '未修复';
    const t = text.replace(/\n/g, ' ').trim();
    if (t.length > 120) {
        return t.slice(0, 120) + '...';
    }
    return t;
}

function isLikelyModule(text) {
    if (!text) return false;
    const t = text.toLowerCase();
    const modulePatterns = [
        /^starter-/,
        /^carlos-/,
        /^(core|parent|dependencies|utils|web|security|encrypt|oss|mq|json|sms|mybatis|datacenter|datascope|openapi|redis|snowflake|translation|migration|log)$/,
        /^(多个|所有|全局|框架级|模块级)/,
    ];
    return modulePatterns.some(p => p.test(t));
}

function normalizePriority(p) {
    if (!p) return '';
    const s = String(p).trim();
    const m = s.match(/^(P[0-3])$/i);
    return m ? m[1].toUpperCase() : '';
}

function extractPriorityFromId(id) {
    if (!id) return '';
    const s = String(id).trim();
    const m = s.match(/^(P[0-3])[-_]/i);
    return m ? m[1].toUpperCase() : '';
}

function normalizeModule(raw) {
    if (!raw) return '';
    let m = String(raw)
        .replace(/^`+|`+$/g, '')
        .replace(/\*/g, '')
        .replace(/^carlos-commons\//, '')
        .replace(/^carlos-commons\s*\/\s*/, '')
        .replace(/^carlos-spring-boot-starter-/, 'starter-')
        .replace(/^carlos-spring-boot-core$/, 'core')
        .replace(/^carlos-parent$/, 'parent')
        .replace(/^carlos-dependencies$/, 'carlos-dependencies')
        .trim();

    // 处理 carlos-spring-boot/carlos-spring-boot-starter-xxx
    const fullStarterMatch = m.match(/carlos-spring-boot\/(?:carlos-)?spring-boot-starter-([a-z0-9-]+)/i);
    if (fullStarterMatch) {
        m = 'starter-' + fullStarterMatch[1];
    }
    // 处理 carlos-spring-boot-starter-xxx (已经是标准格式但可能带路径)
    const starterMatch = m.match(/(?:^|\/)carlos-spring-boot-starter-([a-z0-9-]+)$/i);
    if (starterMatch) {
        m = 'starter-' + starterMatch[1];
    }

    // 处理 "carlos-dependencies/parent"
    if (m.includes('dependencies') && m.includes('parent')) {
        m = 'carlos-dependencies/parent';
    }
    // 单独的 "dependencies" 映射为 "carlos-dependencies"
    if (m === 'dependencies') {
        m = 'carlos-dependencies';
    }

    // 文件路径/类名映射到模块
    const fileModuleMap = {
        'SslUtil.java': 'carlos-utils',
        'UserContextAuthenticationFilter.java': 'starter-security',
        'XssHttpServletRequestWrapper.java': 'starter-web',
        'EncryptConfig.java': 'starter-encrypt',
        'OssController.java': 'starter-oss',
    };

    for (const [file, mod] of Object.entries(fileModuleMap)) {
        if (m.endsWith(file)) return mod;
    }

    return m;
}

function normalizeTitle(title) {
    if (!title) return '';
    return title
        .replace(/[\u{1F300}-\u{1F9FF}]/gu, '') // 移除 emoji
        .replace(/[\s`"'']/g, ' ') // 将反引号、引号替换为空格
        .replace(/[（(]\s*[)）]/g, '') // 移除空括号
        .replace(/[（(]\d{2}-\d{2}[^)）]*[)）]/g, '') // 移除日期括号
        .replace(/[\s]*[—-]\s*历史遗留.*$/i, '') // 移除 "— 历史遗留"
        .replace(/\s*（历史遗留）/g, '')
        .replace(/\s*\(历史遗留\)/g, '')
        .replace(/\s*（🆕 新发现）/g, '')
        .replace(/\s*\(🆕 新发现\)/g, '')
        .replace(/\s*（新发现）/g, '')
        .replace(/\s*\(新发现\)/g, '')
        .replace(/\s+/g, ' ')
        .replace(/^[\s「【\[]+|[\s」】\]]+$/g, '')
        .trim();
}

function makeKey(module, title) {
    const m = normalizeModule(module).toLowerCase();
    const t = normalizeTitle(title).toLowerCase();
    return `${m}::${t}`;
}

function extractStatusFromBlock(block) {
    const statusMatch = block.match(/^\*\*状态\*\*[:：]\s*(.+?)(?:\s*$|\s+\*\*)/m);
    if (statusMatch) return statusMatch[1].trim();
    const fixStatusMatch = block.match(/-\s*\*\*修复状态\*\*[:：]\s*(.+?)$/m);
    if (fixStatusMatch) return fixStatusMatch[1].trim();
    return null; // 没有明确状态字段
}

function extractModuleAndTitleFromCheckbox(line) {
    // 格式1: - [ ] **P0-001** `module`: 标题 ⏳ 状态
    const match1 = line.match(/^-\s*\[\s*[xX\s]\s*\]\s*\*\*(P\d+-\d+)\*\*\s*`?([^`:]+?)`?\s*[:：]\s*(.+?)(?:\s+[⏳❌✅⚠️]|\s+\*\*|$)/);
    if (match1) {
        return {
            id: match1[1],
            module: normalizeModule(match1[2]),
            title: normalizeTitle(match1[3]),
        };
    }
    // 格式2: - [ ] P0-001: 标题 ⏳ 状态（无 module）
    const match2 = line.match(/^-\s*\[\s*[xX\s]\s*\]\s*(P\d+-\d+)[:：]\s*(.+?)(?:\s+[⏳❌✅⚠️]|\s+\*\*|$)/);
    if (match2) {
        return {
            id: match2[1],
            module: '',
            title: normalizeTitle(match2[2]),
        };
    }
    return null;
}

function extractSolution(detail) {
    if (!detail) return '';
    const patterns = [
        /(?:#+\s*(?:解决|推荐修复|修复方案).*?:?)(.*?)(?=#+\s|##\s|$)/is,
        /\*\*推荐修复\*\*[:：]?\s*(.+?)(?=\*\*|##\s|$)/is,
        /\*\*解决方案\*\*[:：]?\s*(.+?)(?=\*\*|##\s|$)/is,
    ];
    for (const pattern of patterns) {
        const match = detail.match(pattern);
        if (match) {
            const text = match[1].trim();
            if (text && text.length > 5) return text;
        }
    }
    return '';
}

function parseMarkdownReport(filePath) {
    const text = fs.readFileSync(filePath, 'utf-8');
    const date = extractDateFromFilename(filePath);
    const reportTitle = path.basename(filePath);
    const issues = [];

    // 1. 提取问题详情段落 - 模式A: ### [Px] 问题 N：标题（支持 3-5 级标题）
    const detailPatternA = /#{3,5}\s*\[(P[0-3])\]\s*问题\s*\d+\s*[:：]\s*(.+?)(?=#{3,5}\s*\[(P[0-3])\]|## |$)/gs;
    let m;
    while ((m = detailPatternA.exec(text)) !== null) {
        const priority = m[1];
        const title = normalizeTitle(m[2].split(/\r?\n/)[0]);
        const block = m[0];
        const statusText = extractStatusFromBlock(block);
        let status;
        if (statusText === null) {
            // 没有明确状态字段，视为当天新发现；只要不是明确已修复，就视为未修复
            status = /已修复|✅|完成|fixed/i.test(block) ? 'fixed' : 'unfixed';
        } else {
            status = classifyStatus(statusText, 'detail');
        }
        if (status !== 'unfixed') continue;
        if (!isValidTitle(title)) continue;

        const moduleMatch = block.match(/^\*\*模块\*\*[:：]\s*`?([^`\n]+?)`?\s*$/m);
        const module = moduleMatch ? normalizeModule(moduleMatch[1]) : '';
        const fileMatch = block.match(/^\*\*文件\*\*[:：]\s*`?([^`\n]+?)`?\s*$/m);
        const filePathInIssue = fileMatch ? fileMatch[1].trim() : '';

        issues.push({
            date,
            report: reportTitle,
            priority,
            module,
            title,
            status,
            statusText: shortStatus(statusText || '未修复'),
            file: filePathInIssue,
            detail: block.trim(),
            source: 'detailA'
        });
    }

    // 2. 提取问题详情段落 - 模式B: ### 问题 Px-xxx: 标题
    const detailPatternB = /###\s*问题\s+(P\d+-\d+)[:：]\s*(.+?)(?=###\s*问题|## |$)/gs;
    while ((m = detailPatternB.exec(text)) !== null) {
        const id = m[1];
        const priority = normalizePriority(id);
        const title = normalizeTitle(m[2].split(/\r?\n/)[0]);
        const block = m[0];
        const statusText = extractStatusFromBlock(block);
        let status;
        if (statusText === null) {
            status = /已修复|✅|完成|fixed/i.test(block) ? 'fixed' : 'unfixed';
        } else {
            status = classifyStatus(statusText, 'detail');
        }
        if (status !== 'unfixed') continue;
        if (!isValidTitle(title)) continue;

        const moduleMatch = block.match(/^\*\*模块\*\*[:：]\s*`?([^`\n]+?)`?\s*$/m) ||
                           block.match(/^\*\*影响范围\*\*[:：]\s*`?([^`\n]+?)`?\s*$/m);
        const module = moduleMatch ? normalizeModule(moduleMatch[1]) : '';
        const fileMatch = block.match(/^\*\*文件\*\*[:：]\s*`?([^`\n]+?)`?\s*$/m);
        const filePathInIssue = fileMatch ? fileMatch[1].trim() : '';

        issues.push({
            date,
            report: reportTitle,
            id,
            priority,
            module,
            title,
            status,
            statusText: shortStatus(statusText || '未修复'),
            file: filePathInIssue,
            detail: block.trim(),
            source: 'detailB'
        });
    }

    // 3. 从历史遗留跟踪/待办章节提取表格和复选框（支持二级或三级标题）
    const sections = text.split(/(?=#{2,3}\s+.*(?:历史遗留|遗留跟踪|遗留未修复|修复状态|问题跟踪|新增待办|待办事项|修复计划))/i);
    for (let i = 1; i < sections.length; i++) {
        const section = sections[i];
        const isTodoSection = /新增待办|待办事项|修复计划/i.test(section.slice(0, 200));
        const lines = section.split(/\r?\n/);

        for (const line of lines) {
            const trimmed = line.trim();
            if (!trimmed) continue;

            // 表格行
            if (trimmed.startsWith('|')) {
                const cells = trimmed
                    .replace(/^\|+|\|+$/g, '')
                    .split('|')
                    .map(s => s.trim());
                if (cells.length < 4) continue;

                let priority, module, title, statusText;

                // 判断表格格式
                const firstIsPriority = !!normalizePriority(cells[0]);
                const firstIsId = /^P\d+[-_]\S+$/i.test(cells[0]);
                const secondIsPriority = !!normalizePriority(cells[1]);
                const secondIsNumber = /^#\d+$/.test(cells[1]);

                if (firstIsPriority && secondIsNumber && cells.length >= 5) {
                    // 待办/修复计划表格：优先级 | # | 模块 | 标题 | 修复方案 | 预计时间 |
                    priority = normalizePriority(cells[0]);
                    module = normalizeModule(cells[2]);
                    title = normalizeTitle(cells[3]);
                    statusText = cells.slice(4).join(' ');
                } else if (firstIsPriority && cells.length >= 4) {
                    // 4列：优先级 | 模块 | 标题 | 状态
                    priority = normalizePriority(cells[0]);
                    module = normalizeModule(cells[1]);
                    title = normalizeTitle(cells[2]);
                    statusText = cells[cells.length - 1];
                } else if (firstIsId && secondIsPriority && cells.length >= 5) {
                    // 5列：ID | 优先级 | 模块 | 标题 | 状态
                    priority = normalizePriority(cells[1]);
                    module = normalizeModule(cells[2]);
                    title = normalizeTitle(cells[3]);
                    statusText = cells[cells.length - 1];
                } else if (firstIsId && cells.length >= 5) {
                    // 5列格式：可能是 ID | 模块 | 标题 | 状态 | 备注
                    // 或：ID | 标题 | 模块 | 状态 | 备注
                    priority = extractPriorityFromId(cells[0]);
                    if (isLikelyModule(cells[2])) {
                        module = normalizeModule(cells[2]);
                        title = normalizeTitle(cells[1]);
                    } else {
                        module = normalizeModule(cells[1]);
                        title = normalizeTitle(cells[2]);
                    }
                    // 状态取最后一个包含未修复/已修复的列
                    const statusCols = cells.slice(3).filter(c => /未修复|已修复|❌|✅|⏳/i.test(c));
                    statusText = statusCols.length > 0 ? statusCols[statusCols.length - 1] : cells[cells.length - 1];
                } else {
                    continue;
                }

                // 跳过标题行和说明行
                if (!priority) continue;
                if (!module || module.toLowerCase() === '模块' || module.toLowerCase() === 'module') continue;
                if (!title || title.toLowerCase() === '标题' || title.toLowerCase() === '问题描述' || title.toLowerCase() === 'description') continue;

                // 跳过版本号被当作模块的行（如模块是 "1.5.4"）
                if (/^\d+(\.\d+)*$/.test(module)) continue;
                // 跳过模块本身就是优先级的错误行
                if (normalizePriority(module)) continue;
                if (!isValidTitle(title)) continue;

                let status = classifyStatus(statusText, 'table');
                // 待办 section 中的问题默认视为未修复
                if (status === 'unknown' && isTodoSection) {
                    status = 'unfixed';
                    statusText = '未修复（新增待办）';
                }
                if (status === 'unfixed' && module && title) {
                    issues.push({
                        date,
                        report: reportTitle,
                        priority,
                        module,
                        title,
                        status,
                        statusText: shortStatus(statusText),
                        file: '',
                        detail: '',
                        source: 'table'
                    });
                }
            }

            // 复选框列表
            if (/^-\s*\[\s*[xX\s]\s*\]/.test(trimmed)) {
                const parsed = extractModuleAndTitleFromCheckbox(trimmed);
                if (parsed && parsed.module && parsed.title && isValidTitle(parsed.title)) {
                    const priority = normalizePriority(parsed.id);
                    const status = classifyStatus(trimmed, 'checkbox');
                    if (status === 'unfixed') {
                        issues.push({
                            date,
                            report: reportTitle,
                            id: parsed.id,
                            priority,
                            module: parsed.module,
                            title: parsed.title,
                            status,
                            statusText: shortStatus(trimmed),
                            file: '',
                            detail: '',
                            source: 'checkbox'
                        });
                    }
                }
            }
        }
    }

    // 后处理：为没有模块的 detail 问题，从同一报告的表格/复选框中推断模块
    const tableIssues = issues.filter(i => i.source === 'table' || i.source === 'checkbox');
    for (const issue of issues) {
        if ((issue.source === 'detailA' || issue.source === 'detailB') && !issue.module && tableIssues.length > 0) {
            const normTitle = normalizeTitle(issue.title).toLowerCase();
            let bestMatch = null;
            let bestSim = 0;
            for (const tableIssue of tableIssues) {
                const tableNormTitle = normalizeTitle(tableIssue.title).toLowerCase();
                const sim = similarity(normTitle, tableNormTitle);
                if (sim > bestSim) {
                    bestSim = sim;
                    bestMatch = tableIssue;
                }
            }
            if (bestMatch && bestSim >= 0.3) {
                issue.module = bestMatch.module;
            }
        }
    }

    return issues;
}

function similarity(a, b) {
    // 简单 Jaccard 相似度，基于字符二元组
    const setA = new Set();
    const setB = new Set();
    for (let i = 0; i < a.length - 1; i++) setA.add(a.slice(i, i + 2));
    for (let i = 0; i < b.length - 1; i++) setB.add(b.slice(i, i + 2));
    const intersection = new Set([...setA].filter(x => setB.has(x)));
    const union = new Set([...setA, ...setB]);
    return union.size === 0 ? 0 : intersection.size / union.size;
}

function isSameModuleFamily(a, b) {
    if (a === b) return true;
    const na = (a || '').toLowerCase();
    const nb = (b || '').toLowerCase();
    // 如果其中一个模块为空/未知，允许与任何模块合并
    if ((!na || na === '未知模块') || (!nb || nb === '未知模块')) return true;
    const crossModuleFamilies = [
        ['carlos-dependencies', 'carlos-dependencies/parent', 'parent', '框架级（所有 spring boot starter 模块）'],
    ];
    for (const family of crossModuleFamilies) {
        const inA = family.some(f => na.includes(f.toLowerCase()));
        const inB = family.some(f => nb.includes(f.toLowerCase()));
        if (inA && inB) return true;
    }
    return false;
}

function keywordOverlap(a, b) {
    // 提取中文词和英文技术词
    const wordsA = a.toLowerCase().match(/[a-z]+|\d+(?:\.\d+)+|[\u4e00-\u9fa5]{2,}/g) || [];
    const wordsB = b.toLowerCase().match(/[a-z]+|\d+(?:\.\d+)+|[\u4e00-\u9fa5]{2,}/g) || [];
    const setA = new Set(wordsA);
    const setB = new Set(wordsB);
    const intersection = new Set([...setA].filter(x => setB.has(x)));
    const union = new Set([...setA, ...setB]);
    return union.size === 0 ? 0 : intersection.size / union.size;
}

function deduplicateIssues(issues) {
    // 按日期排序
    issues.sort((a, b) => a.date.localeCompare(b.date));

    const groups = [];

    for (const issue of issues) {
        if (!issue.priority) continue;
        const key = makeKey(issue.module, issue.title);
        if (!key || key === '::') continue;

        // 查找是否有相似问题
        let merged = false;
        for (const group of groups) {
            if (!isSameModuleFamily(issue.module, group.module)) continue;

            const normTitleA = normalizeTitle(issue.title).toLowerCase();
            const normTitleB = normalizeTitle(group.title).toLowerCase();
            const titleSim = similarity(normTitleA, normTitleB);
            const kwOverlap = keywordOverlap(normTitleA, normTitleB);

            // 合并条件：Jaccard >= 0.5 或 关键词重叠 >= 0.5（且至少共享2个关键词）
            const sharedKeywords = new Set(
                [...(normTitleA.match(/[a-z]+|\d+(?:\.\d+)+|[\u4e00-\u9fa5]{2,}/g) || [])]
                    .filter(w => (normTitleB.match(/[a-z]+|\d+(?:\.\d+)+|[\u4e00-\u9fa5]{2,}/g) || []).includes(w))
            );
            const shouldMerge = titleSim >= 0.35 || (kwOverlap >= 0.4 && sharedKeywords.size >= 2);

            if (shouldMerge) {
                const priorityRank = { 'P0': 0, 'P1': 1, 'P2': 2, 'P3': 3 };
                // 保留较高优先级（数字小的）
                if ((priorityRank[issue.priority] ?? 99) < (priorityRank[group.priority] ?? 99)) {
                    group.priority = issue.priority;
                }
                if (issue.date > group.date ||
                    (issue.date === group.date && issue.detail && !group.detail)) {
                    group.date = issue.date;
                    group.report = issue.report;
                    group.status = issue.status;
                    group.statusText = issue.statusText;
                    group.module = issue.module; // 保留较新的模块名
                    if (issue.detail) group.detail = issue.detail;
                    if (issue.file) group.file = issue.file;
                    if (issue.title.length > group.title.length) group.title = issue.title;
                }
                if (issue.title.length > group.title.length) {
                    group.title = issue.title;
                }
                merged = true;
                break;
            }
        }

        if (!merged) {
            groups.push({ ...issue });
        }
    }

    return groups;
}

function main() {
    const files = fs.readdirSync(REPORTS_DIR)
        .filter(f => f.startsWith('framework-scan-report-') && f.endsWith('.md'))
        .map(f => path.join(REPORTS_DIR, f))
        .sort((a, b) => extractDateFromFilename(a).localeCompare(extractDateFromFilename(b)));

    if (files.length === 0) {
        console.log('未找到报告文件。');
        return;
    }

    const allIssues = [];
    for (const f of files) {
        console.log(`解析: ${f}`);
        const issues = parseMarkdownReport(f);
        allIssues.push(...issues);
        const unfixedCount = issues.filter(x => x.status === 'unfixed').length;
        console.log(`  找到 ${issues.length} 个问题，其中未修复 ${unfixedCount} 个`);
    }

    // 去重
    const dedupedIssues = deduplicateIssues(allIssues);

    // 只输出最新状态为未修复的问题
    const unfixedIssues = dedupedIssues.filter(issue => issue.status === 'unfixed');

    const priorityOrder = { 'P0': 0, 'P1': 1, 'P2': 2, 'P3': 3 };
    const sortedIssues = unfixedIssues.sort((a, b) => {
        const pa = priorityOrder[a.priority] ?? 99;
        const pb = priorityOrder[b.priority] ?? 99;
        if (pa !== pb) return pa - pb;
        const ma = a.module || '';
        const mb = b.module || '';
        if (ma !== mb) return ma.localeCompare(mb);
        return (a.title || '').localeCompare(b.title || '');
    });

    const now = new Date();
    const lines = [];
    lines.push('# Carlos Framework 未修复问题汇总');
    lines.push('');
    lines.push(`**生成时间**: ${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`);
    lines.push(`**扫描报告数量**: ${files.length} 份`);
    lines.push(`**未修复问题总数**: ${sortedIssues.length} 个`);
    lines.push('');
    lines.push('---');
    lines.push('');

    // 概览表格
    lines.push('## 一、按优先级分布');
    lines.push('');
    lines.push('| 优先级 | 数量 |');
    lines.push('|--------|------|');
    const counts = {};
    for (const issue of sortedIssues) {
        counts[issue.priority] = (counts[issue.priority] || 0) + 1;
    }
    for (const p of ['P0', 'P1', 'P2', 'P3']) {
        if (counts[p]) {
            lines.push(`| ${p} | ${counts[p]} |`);
        }
    }
    lines.push('');

    lines.push('## 二、按模块分布');
    lines.push('');
    lines.push('| 模块 | 数量 |');
    lines.push('|------|------|');
    const moduleCounts = {};
    for (const issue of sortedIssues) {
        const module = issue.module || '未知模块';
        moduleCounts[module] = (moduleCounts[module] || 0) + 1;
    }
    Object.entries(moduleCounts)
        .sort((a, b) => b[1] - a[1])
        .forEach(([module, count]) => {
            lines.push(`| ${module} | ${count} |`);
        });
    lines.push('');
    lines.push('---');
    lines.push('');

    // 详细列表
    lines.push('## 三、未修复问题清单');
    lines.push('');

    let currentPriority = null;
    sortedIssues.forEach((issue, index) => {
        const p = issue.priority || '未知';
        if (p !== currentPriority) {
            currentPriority = p;
            lines.push(`### ${p} 级别`);
            lines.push('');
        }

        const title = issue.title || '';
        const module = issue.module || '未知模块';
        const report = issue.report || '';
        const date = issue.date || '';
        const statusText = issue.statusText || '未修复';
        const filePath = issue.file || '';

        lines.push(`#### ${index + 1}. ${title}`);
        lines.push('');
        lines.push(`- **优先级**: ${p}`);
        lines.push(`- **模块**: ${module}`);
        if (filePath) {
            lines.push(`- **文件**: \`${filePath}\``);
        }
        lines.push(`- **状态**: ${statusText}`);
        lines.push(`- **最近报告**: ${report} (${date})`);

        const solution = extractSolution(issue.detail);
        if (solution) {
            lines.push('');
            lines.push('**推荐修复**:');
            lines.push('');
            const maxLen = 800;
            const displaySolution = solution.length > maxLen
                ? solution.slice(0, maxLen) + '\n\n...（解决方案较长，详见原始报告）'
                : solution;
            lines.push(displaySolution);
        }
        lines.push('');
    });

    // 来源报告列表
    lines.push('---');
    lines.push('');
    lines.push('## 四、来源报告清单');
    lines.push('');
    lines.push('以下旧报告已在汇总文件生成后被删除：');
    lines.push('');
    for (const f of files) {
        lines.push(`- ${path.basename(f)}`);
    }
    lines.push('');

    fs.writeFileSync(OUTPUT_FILE, lines.join('\n'), 'utf-8');

    console.log(`\n汇总完成: ${OUTPUT_FILE}`);
    console.log(`共汇总 ${sortedIssues.length} 个未修复问题（来自 ${files.length} 份报告）`);

    // 删除旧报告文件（调试期间注释掉）
    // for (const f of files) {
    //     try {
    //         fs.unlinkSync(f);
    //         console.log(`已删除: ${f}`);
    //     } catch (e) {
    //         console.error(`删除失败 ${f}: ${e.message}`);
    //     }
    // }
}

main();
