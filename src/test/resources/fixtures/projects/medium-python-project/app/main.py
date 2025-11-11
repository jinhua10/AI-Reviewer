"""
Flask应用主程序
简单的任务管理系统
"""
from app import create_app
from app.models import Task, TaskStatus

app = create_app()

@app.shell_context_processor
def make_shell_context():
    """Flask Shell上下文"""
    return {
        'app': app,
        'Task': Task,
        'TaskStatus': TaskStatus
    }

if __name__ == '__main__':
    print("=== 任务管理系统启动 ===")
    print("访问 http://127.0.0.1:5000")
    app.run(debug=True, host='0.0.0.0', port=5000)

