#!/bin/bash

# 黑客松AI评审工具 - 快速评审脚本 (Linux/macOS版)
# 用于快速评审本地的源码项目

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示帮助信息
show_help() {
    echo "🏆 黑客松AI评审工具 - 快速评审脚本"
    echo
    echo "用法: $0 <项目路径> [评审模式]"
    echo
    echo "评审模式:"
    echo "  QUICK     - 快速评审 (10秒)"
    echo "  DETAILED  - 详细评审 (30秒)"
    echo "  EXPERT    - 专家评审 (60秒)"
    echo
    echo "示例:"
    echo "  $0 /home/user/MyProject QUICK"
    echo "  $0 ./MyProject DETAILED"
    echo "  $0 /path/to/project    # 默认QUICK模式"
    echo
    echo "环境要求:"
    echo "  • JDK 17+"
    echo "  • DEEPSEEK_API_KEY 环境变量"
    echo "  • Maven (用于编译)"
    echo
}

# 检查参数
if [ $# -eq 0 ] || [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    show_help
    exit 0
fi

PROJECT_PATH="$1"
REVIEW_MODE="${2:-QUICK}"

# 检查项目路径
if [ ! -d "$PROJECT_PATH" ]; then
    log_error "项目路径不存在: $PROJECT_PATH"
    exit 1
fi

# 检查Java环境
if ! command -v java &> /dev/null; then
    log_error "未找到Java环境，请安装JDK 17+"
    log_info "下载地址: https://adoptium.net/"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    log_error "需要JDK 17+，当前版本: $JAVA_VERSION"
    exit 1
fi

# 检查API密钥
if [ -z "$DEEPSEEK_API_KEY" ]; then
    log_error "未设置DEEPSEEK_API_KEY环境变量"
    log_info "请运行: export DEEPSEEK_API_KEY='your-api-key-here'"
    exit 1
fi

log_success "环境检查通过"
log_info "项目路径: $PROJECT_PATH"
log_info "评审模式: $REVIEW_MODE"
log_info "API密钥: 已设置"

# 编译项目 (如果需要)
if [ ! -f "target/classes/top/yumbo/ai/reviewer/HackathonCLI.class" ]; then
    log_info "编译评审工具..."
    if ! mvn clean compile -q; then
        log_error "编译失败"
        exit 1
    fi
    log_success "编译完成"
fi

# 开始评审
log_info "开始评审项目..."
echo "⏳ 评审中，请稍候..."
echo

# 运行评审
if java -cp target/classes top.yumbo.ai.reviewer.HackathonCLI review "$PROJECT_PATH" "$REVIEW_MODE"; then
    log_success "评审完成！"

    # 检查报告文件
    REPORT_FILES=$(ls hackathon-*-report.md 2>/dev/null || true)
    if [ -n "$REPORT_FILES" ]; then
        echo
        log_info "评审报告已生成:"
        echo "$REPORT_FILES"
        echo
        log_info "提示: 打开上述文件查看详细评审报告"
    fi

    echo
    echo "🎉 评审完成！感谢使用黑客松AI评审工具。"
    echo
    echo "💡 更多功能:"
    echo "  • 查看排行榜: java -cp target/classes top.yumbo.ai.reviewer.HackathonCLI leaderboard"
    echo "  • 查看统计: java -cp target/classes top.yumbo.ai.reviewer.HackathonCLI stats"
    echo "  • 运行演示: java -cp target/classes top.yumbo.ai.reviewer.HackathonDemo"
    echo
    echo "📚 更多信息请查看: QUICK-START-GUIDE.md"
else
    log_error "评审失败，请检查错误信息"
    exit 1
fi
