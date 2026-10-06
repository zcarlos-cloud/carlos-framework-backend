# =============================================================================
# Carlos Framework - 批量创建 GitHub Issues 脚本 (PowerShell 版本)
# 用于将 TODO.md 中的任务批量创建为 GitHub Issues
# =============================================================================

# 颜色定义
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Cyan"

# 配置
$Repo = if ($env:GITHUB_REPO) { $env:GITHUB_REPO } else { "yourusername/carlos-framework" }
$Token = $env:GITHUB_TOKEN

# 检查依赖
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Host "错误: 请先安装 GitHub CLI (gh)" -ForegroundColor $Red
    Write-Host "安装指南: https://cli.github.com/"
    exit 1
}

# 检查登录状态
$authStatus = gh auth status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: 请先登录 GitHub CLI" -ForegroundColor $Red
    Write-Host "运行: gh auth login"
    exit 1
}

Write-Host "========================================" -ForegroundColor $Blue
Write-Host "  Carlos Framework 待办事项导入工具" -ForegroundColor $Blue
Write-Host "========================================" -ForegroundColor $Blue
Write-Host ""

# 功能函数
function Print-Usage {
    @"
用法: .\create-todos.ps1 [选项] [命令]

命令:
    create-all          创建所有待办事项（交互式确认）
    create-labels       创建任务管理所需的标签
    list-todos          列出 TODO.md 中的所有任务
    help                显示此帮助信息

选项:
    -Repo               指定仓库 (默认: $Repo)
    -Yes                自动确认，不询问
    -Help               显示帮助

环境变量:
    GITHUB_TOKEN        GitHub 访问令牌
    GITHUB_REPO         目标仓库 (格式: owner/repo)

示例:
    .\create-todos.ps1 create-labels                    # 创建标签
    .\create-todos.ps1 create-all                       # 创建所有待办（交互式）
    .\create-todos.ps1 create-all -Yes                  # 创建所有待办（自动确认）
    .\create-todos.ps1 -Repo myorg/carlos create-all    # 指定仓库

"@
}

# 创建标签
function Create-Labels {
    Write-Host "正在创建标签..." -ForegroundColor $Blue

    $labels = @{
        "todo" = "c2e0c6"
        "task" = "0e8a16"
        "priority-critical" = "b60205"
        "priority-high" = "d93f0b"
        "priority-medium" = "fbca04"
        "priority-low" = "0e8a16"
        "status-todo" = "c5def5"
        "status-doing" = "fef2c0"
        "status-done" = "c2e0c6"
        "category-core" = "0052cc"
        "category-test" = "d4c5f9"
        "category-docs" = "0075ca"
        "category-perf" = "bfd4f2"
        "category-security" = "e99695"
    }

    foreach ($label in $labels.Keys) {
        $color = $labels[$label]
        $result = gh label create $label --color $color --repo $Repo 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✓ 创建标签: $label" -ForegroundColor $Green
        } else {
            Write-Host "  ⚠ 标签已存在或创建失败: $label" -ForegroundColor $Yellow
        }
    }

    Write-Host ""
    Write-Host "标签创建完成！" -ForegroundColor $Green
}

# 列出所有待办
function List-Todos {
    Write-Host "TODO.md 中的任务列表:" -ForegroundColor $Blue
    Write-Host ""

    if (-not (Test-Path "TODO.md")) {
        Write-Host "错误: 找不到 TODO.md 文件" -ForegroundColor $Red
        exit 1
    }

    # 解析 TODO.md 并输出任务
    $content = Get-Content "TODO.md"
    $category = ""
    foreach ($line in $content) {
        if ($line -match "^## (.+)") {
            $category = $matches[1].Trim()
        }
        elseif ($line -match "^- \[ \] (.+)") {
            $task = $matches[1].Trim()
            Write-Host "[$category] $task"
        }
    }

    Write-Host ""
    Write-Host "提示: 使用 'create-all' 命令批量创建 Issues" -ForegroundColor $Yellow
}

# 创建单个 Issue
function Create-Issue($Title, $Body, $Labels) {
    if (-not $Yes) {
        $response = Read-Host "创建 Issue: $Title? (y/n/q)"
        if ($response -eq "q" -or $response -eq "Q") {
            Write-Host "已取消" -ForegroundColor $Yellow
            exit 0
        }
        if ($response -ne "y" -and $response -ne "Y") {
            Write-Host "跳过: $Title" -ForegroundColor $Yellow
            return
        }
    }

    $result = gh issue create --repo $Repo --title $Title --body $Body --label $Labels 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ 已创建: $Title" -ForegroundColor $Green
    } else {
        Write-Host "✗ 失败: $Title" -ForegroundColor $Red
        Write-Host $result
    }
}

# 批量创建所有待办
function Create-All {
    Write-Host "准备批量创建 Issues..." -ForegroundColor $Blue
    Write-Host "目标仓库: $Repo" -ForegroundColor $Yellow
    Write-Host ""

    if (-not $Yes) {
        Write-Host "此操作将为 TODO.md 中的任务创建 GitHub Issues" -ForegroundColor $Yellow
        $response = Read-Host "是否继续? (y/N)"
        if ($response -ne "y" -and $response -ne "Y") {
            Write-Host "已取消"
            exit 0
        }
    }

    # 核心功能
    Write-Host ""
    Write-Host "=== 创建高优先级任务 ===" -ForegroundColor $Blue

    Create-Issue "[TASK] 完善 carlos-spring-boot-starter-oauth2 模块文档" `
        "## 任务描述`n`n完善 OAuth2 模块的使用文档，包括：`n`n- 配置说明`n- 使用示例`n- 常见问题`n`n## 完成标准`n`n- [ ] 编写 README.md`n- [ ] 添加配置示例`n- [ ] 编写集成指南" `
        "task,priority-high,category-docs"

    Create-Issue "[TASK] 补充 carlos-spring-boot-starter-datascope 数据权限模块单元测试" `
        "## 任务描述`n`n为数据权限模块添加完整的单元测试，确保数据范围控制功能正常。`n`n## 完成标准`n`n- [ ] 测试覆盖率 >= 80%`n- [ ] 覆盖各种数据权限场景`n- [ ] 通过所有测试用例" `
        "task,priority-high,category-test"

    Create-Issue "[TASK] 提升 carlos-commons 模块单元测试覆盖率至 80%+" `
        "## 任务描述`n`n提升 commons 模块的测试覆盖率。`n`n## 完成标准`n`n- [ ] carlos-spring-boot-core >= 80%`n- [ ] carlos-utils >= 80%`n- [ ] carlos-excel >= 80%" `
        "task,priority-high,category-test"

    # 文档任务
    Write-Host ""
    Write-Host "=== 创建文档任务 ===" -ForegroundColor $Blue

    Create-Issue "[TASK] 更新各 Starter 模块的 README.md 文档" `
        "## 任务描述`n`n为所有 Starter 模块更新 README 文档。`n`n## 完成标准`n`n- [ ] 每个 Starter 都有完整的 README`n- [ ] 包含使用示例`n- [ ] 包含配置说明" `
        "task,priority-medium,category-docs"

    Create-Issue "[TASK] 创建快速开始指南（Quick Start）" `
        "## 任务描述`n`n为新用户创建快速上手指南。`n`n## 完成标准`n`n- [ ] 环境准备`n- [ ] 第一个项目创建`n- [ ] 常见问题" `
        "task,priority-medium,category-docs"

    # 性能优化任务
    Write-Host ""
    Write-Host "=== 创建性能优化任务 ===" -ForegroundColor $Blue

    Create-Issue "[TASK] 优化 carlos-spring-boot-starter-disruptor 高性能队列配置" `
        "## 任务描述`n`n优化 Disruptor 模块的配置和性能。`n`n## 完成标准`n`n- [ ] 性能基准测试`n- [ ] 配置优化`n- [ ] 使用文档" `
        "task,priority-medium,category-perf"

    Create-Issue "[TASK] 审查 carlos-excel 大数据量导出性能" `
        "## 任务描述`n`n评估并优化 Excel 模块的大数据导出性能。`n`n## 完成标准`n`n- [ ] 性能测试`n- [ ] 内存优化`n- [ ] 流式导出实现" `
        "task,priority-medium,category-perf"

    # 安全任务
    Write-Host ""
    Write-Host "=== 创建安全任务 ===" -ForegroundColor $Blue

    Create-Issue "[TASK] 审查所有模块的依赖安全漏洞" `
        "## 任务描述`n`n使用依赖检查工具扫描安全漏洞。`n`n## 完成标准`n`n- [ ] 运行 OWASP 依赖检查`n- [ ] 修复高危漏洞`n- [ ] 更新安全策略" `
        "task,priority-high,category-security"

    Create-Issue "[TASK] 补充 SQL 注入防护测试用例" `
        "## 任务描述`n`n添加 SQL 注入防护的测试用例。`n`n## 完成标准`n`n- [ ] 各种注入场景测试`n- [ ] 参数绑定验证`n- [ ] 安全扫描通过" `
        "task,priority-high,category-security"

    Write-Host ""
    Write-Host "批量创建完成！" -ForegroundColor $Green
}

# 主逻辑
param(
    [string]$Repo = $Repo,
    [switch]$Yes,
    [switch]$Help
)

if ($Help) {
    Print-Usage
    exit 0
}

# 解析命令
$Command = $args[0]

switch ($Command) {
    "create-all" { Create-All }
    "create-labels" { Create-Labels }
    "list-todos" { List-Todos }
    "help" { Print-Usage }
    default { Print-Usage }
}
