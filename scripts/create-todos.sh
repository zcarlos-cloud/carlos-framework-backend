#!/bin/bash
# =============================================================================
# Carlos Framework - 批量创建 GitHub Issues 脚本
# 用于将 TODO.md 中的任务批量创建为 GitHub Issues
# =============================================================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
REPO="${GITHUB_REPO:-yourusername/carlos-framework}"
TOKEN="${GITHUB_TOKEN}"

# 检查依赖
if ! command -v gh &> /dev/null; then
    echo -e "${RED}错误: 请先安装 GitHub CLI (gh)${NC}"
    echo "安装指南: https://cli.github.com/"
    exit 1
fi

# 检查登录状态
if ! gh auth status &> /dev/null; then
    echo -e "${RED}错误: 请先登录 GitHub CLI${NC}"
    echo "运行: gh auth login"
    exit 1
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Carlos Framework 待办事项导入工具${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 功能函数
print_usage() {
    cat << EOF
用法: $0 [选项] [命令]

命令:
    create-all          创建所有待办事项（交互式确认）
    create-labels       创建任务管理所需的标签
    list-todos          列出 TODO.md 中的所有任务
    help                显示此帮助信息

选项:
    -r, --repo          指定仓库 (默认: $REPO)
    -y, --yes           自动确认，不询问
    -h, --help          显示帮助

环境变量:
    GITHUB_TOKEN        GitHub 访问令牌
    GITHUB_REPO         目标仓库 (格式: owner/repo)

示例:
    $0 create-labels                    # 创建标签
    $0 create-all                       # 创建所有待办（交互式）
    $0 create-all -y                    # 创建所有待办（自动确认）
    $0 -r myorg/carlos create-all       # 指定仓库

EOF
}

# 创建标签
create_labels() {
    echo -e "${BLUE}正在创建标签...${NC}"
    
    declare -A labels=(
        ["todo"]="c2e0c6"
        ["task"]="0e8a16"
        ["priority-critical"]="b60205"
        ["priority-high"]="d93f0b"
        ["priority-medium"]="fbca04"
        ["priority-low"]="0e8a16"
        ["status-todo"]="c5def5"
        ["status-doing"]="fef2c0"
        ["status-done"]="c2e0c6"
        ["category-core"]="0052cc"
        ["category-test"]="d4c5f9"
        ["category-docs"]="0075ca"
        ["category-perf"]="bfd4f2"
        ["category-security"]="e99695"
    )
    
    for label in "${!labels[@]}"; do
        color="${labels[$label]}"
        if gh label create "$label" --color "$color" --repo "$REPO" 2>/dev/null; then
            echo -e "  ${GREEN}✓${NC} 创建标签: $label"
        else
            echo -e "  ${YELLOW}⚠${NC} 标签已存在: $label"
        fi
    done
    
    echo ""
    echo -e "${GREEN}标签创建完成！${NC}"
}

# 列出所有待办
list_todos() {
    echo -e "${BLUE}TODO.md 中的任务列表:${NC}"
    echo ""
    
    if [[ ! -f "TODO.md" ]]; then
        echo -e "${RED}错误: 找不到 TODO.md 文件${NC}"
        exit 1
    fi
    
    # 解析 TODO.md 并输出任务
    awk '
        /^## / { category=$0; gsub(/^## /, "", category) }
        /^- \[ \] / { 
            task=$0; 
            gsub(/^- \[ \] /, "", task);
            printf "[%s] %s\n", category, task 
        }
    ' TODO.md | head -50
    
    echo ""
    echo -e "${YELLOW}提示: 使用 'create-all' 命令批量创建 Issues${NC}"
}

# 创建单个 Issue
create_issue() {
    local title="$1"
    local body="$2"
    local labels="$3"
    
    if [[ "$AUTO_CONFIRM" != "true" ]]; then
        read -p "创建 Issue: $title? (y/n/q) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Qq]$ ]]; then
            echo -e "${YELLOW}已取消${NC}"
            exit 0
        fi
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo -e "${YELLOW}跳过: $title${NC}"
            return
        fi
    fi
    
    if gh issue create \
        --repo "$REPO" \
        --title "$title" \
        --body "$body" \
        --label "$labels" 2>/dev/null; then
        echo -e "${GREEN}✓ 已创建:${NC} $title"
    else
        echo -e "${RED}✗ 失败:${NC} $title"
    fi
}

# 批量创建所有待办
create_all() {
    echo -e "${BLUE}准备批量创建 Issues...${NC}"
    echo -e "目标仓库: ${YELLOW}$REPO${NC}"
    echo ""
    
    if [[ "$AUTO_CONFIRM" != "true" ]]; then
        echo -e "${YELLOW}此操作将为 TODO.md 中的任务创建 GitHub Issues${NC}"
        read -p "是否继续? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "已取消"
            exit 0
        fi
    fi
    
    # 核心功能
    echo -e "\n${BLUE}=== 创建高优先级任务 ===${NC}"
    
    create_issue "[TASK] 完善 carlos-spring-boot-starter-oauth2 模块文档" \
        "## 任务描述\n\n完善 OAuth2 模块的使用文档，包括：\n\n- 配置说明\n- 使用示例\n- 常见问题\n\n## 完成标准\n\n- [ ] 编写 README.md\n- [ ] 添加配置示例\n- [ ] 编写集成指南" \
        "task,priority-high,category-docs"
    
    create_issue "[TASK] 补充 carlos-spring-boot-starter-datascope 数据权限模块单元测试" \
        "## 任务描述\n\n为数据权限模块添加完整的单元测试，确保数据范围控制功能正常。\n\n## 完成标准\n\n- [ ] 测试覆盖率 >= 80%\n- [ ] 覆盖各种数据权限场景\n- [ ] 通过所有测试用例" \
        "task,priority-high,category-test"
    
    create_issue "[TASK] 提升 carlos-commons 模块单元测试覆盖率至 80%+" \
        "## 任务描述\n\n提升 commons 模块的测试覆盖率。\n\n## 完成标准\n\n- [ ] carlos-spring-boot-core >= 80%\n- [ ] carlos-utils >= 80%\n- [ ] carlos-excel >= 80%" \
        "task,priority-high,category-test"
    
    echo -e "\n${BLUE}=== 创建文档任务 ===${NC}"
    
    create_issue "[TASK] 更新各 Starter 模块的 README.md 文档" \
        "## 任务描述\n\n为所有 Starter 模块更新 README 文档。\n\n## 完成标准\n\n- [ ] 每个 Starter 都有完整的 README\n- [ ] 包含使用示例\n- [ ] 包含配置说明" \
        "task,priority-medium,category-docs"
    
    create_issue "[TASK] 创建快速开始指南（Quick Start）" \
        "## 任务描述\n\n为新用户创建快速上手指南。\n\n## 完成标准\n\n- [ ] 环境准备\n- [ ] 第一个项目创建\n- [ ] 常见问题" \
        "task,priority-medium,category-docs"
    
    echo -e "\n${BLUE}=== 创建性能优化任务 ===${NC}"
    
    create_issue "[TASK] 优化 carlos-spring-boot-starter-disruptor 高性能队列配置" \
        "## 任务描述\n\n优化 Disruptor 模块的配置和性能。\n\n## 完成标准\n\n- [ ] 性能基准测试\n- [ ] 配置优化\n- [ ] 使用文档" \
        "task,priority-medium,category-perf"
    
    create_issue "[TASK] 审查 carlos-excel 大数据量导出性能" \
        "## 任务描述\n\n评估并优化 Excel 模块的大数据导出性能。\n\n## 完成标准\n\n- [ ] 性能测试\n- [ ] 内存优化\n- [ ] 流式导出实现" \
        "task,priority-medium,category-perf"
    
    echo -e "\n${BLUE}=== 创建安全任务 ===${NC}"
    
    create_issue "[TASK] 审查所有模块的依赖安全漏洞" \
        "## 任务描述\n\n使用依赖检查工具扫描安全漏洞。\n\n## 完成标准\n\n- [ ] 运行 OWASP 依赖检查\n- [ ] 修复高危漏洞\n- [ ] 更新安全策略" \
        "task,priority-high,category-security"
    
    create_issue "[TASK] 补充 SQL 注入防护测试用例" \
        "## 任务描述\n\n添加 SQL 注入防护的测试用例。\n\n## 完成标准\n\n- [ ] 各种注入场景测试\n- [ ] 参数绑定验证\n- [ ] 安全扫描通过" \
        "task,priority-high,category-security"
    
    echo ""
    echo -e "${GREEN}批量创建完成！${NC}"
}

# 主逻辑
AUTO_CONFIRM="false"
COMMAND=""

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -r|--repo)
            REPO="$2"
            shift 2
            ;;
        -y|--yes)
            AUTO_CONFIRM="true"
            shift
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        create-all|create-labels|list-todos|help)
            COMMAND="$1"
            shift
            ;;
        *)
            echo -e "${RED}未知选项: $1${NC}"
            print_usage
            exit 1
            ;;
    esac
done

# 执行命令
case "$COMMAND" in
    create-all)
        create_all
        ;;
    create-labels)
        create_labels
        ;;
    list-todos)
        list_todos
        ;;
    help|"")
        print_usage
        ;;
    *)
        echo -e "${RED}未知命令: $COMMAND${NC}"
        print_usage
        exit 1
        ;;
esac
