# Task Management API

一个使用Flask构建的简单任务管理RESTful API，用于测试代码分析功能。

## 功能特性

- ✅ RESTful API设计
- ✅ CRUD操作（创建、读取、更新、删除）
- ✅ 任务状态管理
- ✅ 清晰的代码结构
- ✅ 蓝图模式

## 项目结构

```
app/
├── __init__.py      # 应用初始化
├── main.py          # 主程序入口
├── models.py        # 数据模型
├── views.py         # 视图函数
└── config.py        # 配置文件
```

## API端点

- `GET /api/tasks` - 获取所有任务
- `GET /api/tasks/<id>` - 获取单个任务
- `POST /api/tasks` - 创建任务
- `PUT /api/tasks/<id>` - 更新任务
- `DELETE /api/tasks/<id>` - 删除任务
- `GET /api/tasks/health` - 健康检查

## 安装运行

```bash
# 安装依赖
pip install -r requirements.txt

# 运行应用
python -m app.main
```

## 代码质量

- ✅ 分层架构（模型-视图-控制器）
- ✅ 类型提示
- ✅ 文档字符串
- ✅ 错误处理
- ✅ RESTful设计原则

## 测试用途

本项目作为AI代码审查工具的测试项目，用于验证：
- Python代码分析
- Flask框架识别
- API设计评估
- 代码质量评分

