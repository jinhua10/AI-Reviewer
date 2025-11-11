#!/bin/bash

# 简单配置测试脚本

echo "=== 测试配置加载 ==="

# 编译项目
echo "编译项目..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi

echo "✅ 编译成功"

# 测试配置加载
echo "测试配置加载..."
java -cp target/classes -Djava.util.logging.level=INFO top.yumbo.ai.reviewer.HackathonReviewer 2>&1 | head -20

echo "=== 测试完成 ==="
