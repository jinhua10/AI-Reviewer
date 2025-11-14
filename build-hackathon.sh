#!/bin/bash

# 黑客松评审工具构建脚本
# 使用此脚本快速构建 hackathon-reviewer.jar

echo "=========================================="
echo "  黑客松评审工具 - 构建脚本"
echo "=========================================="
echo ""

# 检查 Maven 是否安装
if ! command -v mvn &> /dev/null; then
    echo "❌ 错误: Maven 未安装或不在 PATH 中"
    echo "请先安装 Maven: https://maven.apache.org/download.cgi"
    exit 1
fi

echo "✅ Maven 版本:"
mvn -version
echo ""

# 选择构建模式
echo "选择构建模式:"
echo "  1) 快速构建（默认，跳过测试）"
echo "  2) 完整构建（包含测试）"
echo "  3) 生产构建（包含源码和文档）"
echo ""
read -p "请选择 [1-3, 默认: 1]: " choice
choice=${choice:-1}

echo ""
echo "开始构建..."
echo ""

case $choice in
    1)
        echo "📦 快速构建模式..."
        mvn clean package -f hackathon-pom.xml -Pquick
        ;;
    2)
        echo "📦 完整构建模式..."
        mvn clean package -f hackathon-pom.xml
        ;;
    3)
        echo "📦 生产构建模式..."
        mvn clean package -f hackathon-pom.xml -Pproduction
        ;;
    *)
        echo "❌ 无效选择，使用默认快速构建"
        mvn clean package -f hackathon-pom.xml -Pquick
        ;;
esac

# 检查构建结果
if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "  ✅ 构建成功！"
    echo "=========================================="
    echo ""
    echo "📦 输出文件:"
    echo "  - target/hackathon-reviewer.jar"
    echo ""
    echo "📏 文件大小:"
    ls -lh target/hackathon-reviewer.jar | awk '{print "  - " $9 ": " $5}'
    echo ""
    echo "🚀 使用方法:"
    echo "  java -jar target/hackathon-reviewer.jar --help"
    echo ""
    echo "📋 示例命令:"
    echo "  # 评审本地项目"
    echo "  java -jar target/hackathon-reviewer.jar \\"
    echo "    -d /path/to/project \\"
    echo "    -t \"Team Name\" \\"
    echo "    -o score.json"
    echo ""
    echo "  # 评审 GitHub 项目"
    echo "  java -jar target/hackathon-reviewer.jar \\"
    echo "    --github-url https://github.com/user/repo \\"
    echo "    -t \"Team Name\" \\"
    echo "    -o score.json"
    echo ""
    echo "  # 评审 ZIP 文件"
    echo "  java -jar target/hackathon-reviewer.jar \\"
    echo "    -z project.zip \\"
    echo "    -t \"Team Name\" \\"
    echo "    -o score.json"
    echo ""
    echo "  # 评审 S3 项目"
    echo "  java -jar target/hackathon-reviewer.jar \\"
    echo "    -s projects/team-name/ \\"
    echo "    -t \"Team Name\" \\"
    echo "    -o score.json"
    echo ""
else
    echo ""
    echo "=========================================="
    echo "  ❌ 构建失败"
    echo "=========================================="
    echo ""
    echo "请检查错误信息并修复后重试"
    exit 1
fi

