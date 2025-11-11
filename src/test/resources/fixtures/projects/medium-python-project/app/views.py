"""
视图函数
处理HTTP请求
"""
from flask import Blueprint, jsonify, request
from .models import Task, TaskStatus, task_repository

bp = Blueprint('tasks', __name__, url_prefix='/api/tasks')

@bp.route('/', methods=['GET'])
def get_tasks():
    """获取所有任务"""
    tasks = task_repository.get_all()
    return jsonify([task.to_dict() for task in tasks])

@bp.route('/<int:task_id>', methods=['GET'])
def get_task(task_id):
    """获取单个任务"""
    task = task_repository.get_by_id(task_id)
    if task:
        return jsonify(task.to_dict())
    return jsonify({'error': 'Task not found'}), 404

@bp.route('/', methods=['POST'])
def create_task():
    """创建任务"""
    data = request.get_json()

    if not data or 'title' not in data:
        return jsonify({'error': 'Title is required'}), 400

    task = Task(
        title=data['title'],
        description=data.get('description', '')
    )
    task_repository.add(task)

    return jsonify(task.to_dict()), 201

@bp.route('/<int:task_id>', methods=['PUT'])
def update_task(task_id):
    """更新任务"""
    task = task_repository.get_by_id(task_id)
    if not task:
        return jsonify({'error': 'Task not found'}), 404

    data = request.get_json()

    if 'title' in data:
        task.title = data['title']
    if 'description' in data:
        task.description = data['description']
    if 'status' in data:
        try:
            status = TaskStatus(data['status'])
            task.update_status(status)
        except ValueError:
            return jsonify({'error': 'Invalid status'}), 400

    return jsonify(task.to_dict())

@bp.route('/<int:task_id>', methods=['DELETE'])
def delete_task(task_id):
    """删除任务"""
    if task_repository.delete(task_id):
        return '', 204
    return jsonify({'error': 'Task not found'}), 404

@bp.route('/health', methods=['GET'])
def health_check():
    """健康检查"""
    return jsonify({'status': 'healthy', 'service': 'task-api'})

