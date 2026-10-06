#!/usr/bin/env node
/**
 * 生成《开发工具平台》内置模板种子 SQL。
 *
 * 扫描 carlos-tools 的模板资源目录（codege/templates 与 projectge/project-templates），
 * 产出 docs/db/devkit_seed_templates.sql —— 幂等，可重复执行。
 *
 * 用法（在 carlos-framework-backend 目录下）：
 *   node docs/db/gen-devkit-template-seed.mjs
 *
 * 注意：
 *   * 模板正文用 PostgreSQL dollar-quoting（$devkit$...$devkit$）包裹，
 *     避免 FreeMarker 的 ${...} 与 SQL 字符串拼接冲突；
 *   * file_path 取相对 template/ 的路径（引擎按此路径物化到沙箱）。
 */

import { readFileSync, writeFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { join, relative, basename, extname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { dirname } from 'node:path';

const HERE = dirname(fileURLToPath(import.meta.url));
const BACKEND_ROOT = join(HERE, '..', '..');
const TOOLS_RES = join(BACKEND_ROOT, 'carlos-integration', 'carlos-tools', 'src', 'main', 'resources');
const OUTPUT = join(HERE, 'devkit_seed_templates.sql');

/** 模板组静态元数据：目录 → { groupCode, groupName, templateType, techStack, dbType, sortNum } */
const GROUP_META = {
  'codege/templates/cloud-mybatis': {
    groupCode: 'cloud-mybatis', groupName: '微服务 MyBatis-Plus', templateType: 1,
    techStack: 'MYBATIS', dbType: 'ANY', sortNum: 1
  },
  'codege/templates/cloud-ddd-mybatis': {
    groupCode: 'cloud-ddd-mybatis', groupName: '微服务 DDD 四层', templateType: 1,
    techStack: 'DDD_MYBATIS', dbType: 'ANY', sortNum: 2
  },
  'codege/templates/cloud-elasticsearch': {
    groupCode: 'cloud-elasticsearch', groupName: '微服务 Elasticsearch', templateType: 1,
    techStack: 'ELASTICSEARCH', dbType: 'ANY', sortNum: 3
  },
  'codege/templates/cloud-mongo': {
    groupCode: 'cloud-mongo', groupName: '微服务 MongoDB', templateType: 1,
    techStack: 'MONGO', dbType: 'ANY', sortNum: 4
  },
  'projectge/project-templates/carlos-springboot': {
    groupCode: 'carlos-springboot', groupName: 'Spring Boot 单体工程', templateType: 2,
    techStack: 'SPRINGBOOT', dbType: null, sortNum: 5
  },
  'projectge/project-templates/carlos-springcloud': {
    groupCode: 'carlos-springcloud', groupName: 'Spring Cloud 微服务工程', templateType: 2,
    techStack: 'SPRINGCLOUD', dbType: null, sortNum: 6
  }
};

const GROUP_ID_BASE = 2110000000000010000n;
const TEMPLATE_ID_BASE = 2110000000000020000n;

/** 按标签取出 template.xml 里某个子节点的所有 <name> */
function readFieldNames(xml, tag) {
  const block = xml.match(new RegExp(`<${tag}>([\\s\\S]*?)</${tag}>`));
  if (!block) {
    return [];
  }
  return [...block[1].matchAll(/<name>([\s\S]*?)<\/name>/g)].map((m) => m[1].trim()).filter(Boolean);
}

/** 取 template.xml 的某个顶层标量 */
function readScalar(xml, tag) {
  const matched = xml.match(new RegExp(`<${tag}>([\\s\\S]*?)</${tag}>`));
  return matched ? matched[1].trim() : '';
}

/** 递归列出目录下全部文件（返回相对 baseDir 的 posix 路径） */
function walk(dir, baseDir = dir) {
  const result = [];
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) {
      result.push(...walk(full, baseDir));
    } else {
      result.push(relative(baseDir, full).split('\\').join('/'));
    }
  }
  return result;
}

/** 判断是否为二进制内容 */
function isBinary(buf) {
  const limit = Math.min(buf.length, 4096);
  for (let i = 0; i < limit; i++) {
    if (buf[i] === 0) {
      return true;
    }
  }
  return false;
}

/** SQL 字符串字面量转义（用于 group_name / description 等短字段） */
function quote(value) {
  if (value === null || value === undefined) {
    return 'NULL';
  }
  return `'${String(value).replace(/'/g, "''")}'`;
}

/** 为正文选择可用的 dollar-quoting 标签 */
function dollarTag(content) {
  let tag = 'devkit';
  while (content.includes(`$${tag}$`)) {
    tag += 'x';
  }
  return `$${tag}$`;
}

const groupRows = [];
const templateRows = [];
let groupIndex = 0;
let templateIndex = 0;

for (const [relDir, meta] of Object.entries(GROUP_META)) {
  const absDir = join(TOOLS_RES, relDir);
  if (!existsSync(absDir)) {
    console.warn(`[skip] 模板目录不存在: ${absDir}`);
    continue;
  }
  const xmlPath = join(absDir, 'template.xml');
  const xml = existsSync(xmlPath) ? readFileSync(xmlPath, 'utf8') : '';
  const describe = readScalar(xml, 'describe');
  const logicDeleteFields = readFieldNames(xml, 'logicDeleteFields').join(',');
  const versionFields = readFieldNames(xml, 'versionFields').join(',');
  const commonFields = readFieldNames(xml, 'commonFields').join(',');

  const groupId = GROUP_ID_BASE + BigInt(groupIndex + 1);
  groupIndex += 1;

  groupRows.push(
    `${groupId}, ${quote(meta.groupCode)}, ${quote(meta.groupName)}, ${quote(describe)}, ` +
    `${meta.templateType}, ${quote(meta.techStack)}, ${quote(meta.dbType)}, 'template', ` +
    `${quote(logicDeleteFields)}, ${quote(versionFields)}, ${quote(commonFields)}, ` +
    `TRUE, 1, ${meta.sortNum}, ${quote('内置模板，来源于 carlos-tools')}`
  );

  const templateRootDir = join(absDir, 'template');
  if (!existsSync(templateRootDir)) {
    console.warn(`[warn] 缺少 template/ 目录: ${templateRootDir}`);
    continue;
  }
  const files = walk(templateRootDir).sort();
  for (const relPath of files) {
    const absFile = join(templateRootDir, relPath);
    const buf = readFileSync(absFile);
    const binary = isBinary(buf);
    const content = binary ? buf.toString('base64') : buf.toString('utf8');
    const encoding = binary ? 'BASE64' : 'TEXT';
    const isFtl = extname(relPath) === '.ftl';
    const fileType = isFtl ? 1 : 2;
    const fileName = isFtl ? basename(relPath, '.ftl') : basename(relPath);
    const loopFlag = isFtl && fileName.includes('[for]');
    const templateId = TEMPLATE_ID_BASE + BigInt(templateIndex + 1);
    templateIndex += 1;

    templateRows.push(
      `${templateId}, ${groupId}, ${quote(relPath)}, ${quote(fileName)}, ` +
      `${quote(extname(relPath).replace('.', ''))}, ${fileType}, ${loopFlag ? 'TRUE' : 'FALSE'}, ` +
      `${dollarTag(content)}${content}${dollarTag(content)}, ${quote(encoding)}, ${buf.length}, ` +
      `${quote(createHash('md5').update(buf).digest('hex'))}, 0, 1`
    );
  }
}

const header = `-- ============================================================================
-- 开发工具平台（carlos-devkit）内置模板种子脚本（自动生成，请勿手改）
-- ----------------------------------------------------------------------------
-- 由 docs/db/gen-devkit-template-seed.mjs 从 carlos-tools 模板资源扫描生成。
-- 重新生成：node docs/db/gen-devkit-template-seed.mjs
--
-- 幂等：按 group_code / (group_id, file_path) 判重，可重复执行。
-- 模板正文用 dollar-quoting 包裹，避免 FreeMarker \${...} 与 SQL 拼接冲突。
-- id 号段：模板组 211000000000001xxxx，模板文件 211000000000002xxxx。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 模板组
-- ----------------------------------------------------------------------------
INSERT INTO dev_template_group (id, group_code, group_name, description, template_type, tech_stack,
                                db_type, template_root, logic_delete_fields, version_fields, common_fields,
                                built_in, status, sort_num, remark)
SELECT v.*
FROM (VALUES
${groupRows.map((row) => `    (${row})`).join(',\n')}
) AS v(id, group_code, group_name, description, template_type, tech_stack, db_type, template_root,
       logic_delete_fields, version_fields, common_fields, built_in, status, sort_num, remark)
WHERE NOT EXISTS (SELECT 1 FROM dev_template_group g WHERE g.group_code = v.group_code AND g.is_deleted = 0);

-- ----------------------------------------------------------------------------
-- 2. 模板文件
-- ----------------------------------------------------------------------------
INSERT INTO dev_template (id, group_id, file_path, file_name, file_ext, file_type, loop_flag,
                          content, content_encoding, file_size, md5, sort_num, status)
SELECT v.*
FROM (VALUES
${templateRows.map((row) => `    (${row})`).join(',\n')}
) AS v(id, group_id, file_path, file_name, file_ext, file_type, loop_flag,
       content, content_encoding, file_size, md5, sort_num, status)
WHERE NOT EXISTS (SELECT 1 FROM dev_template t
                  WHERE t.group_id = v.group_id AND t.file_path = v.file_path AND t.is_deleted = 0);
`;

writeFileSync(OUTPUT, header, 'utf8');
console.log(`已生成 ${OUTPUT}`);
console.log(`模板组 ${groupRows.length} 个，模板文件 ${templateRows.length} 个`);
