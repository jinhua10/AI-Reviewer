"""
数据模型
定义任务和状态
"""
from enum import Enum
from datetime import datetime
from typing import List, Optional

class TaskStatus(Enum):
    """任务状态枚举"""
    TODO = "待办"
    IN_PROGRESS = "进行中"
    DONE = "已完成"
    CANCELLED = "已取消"

class Task:
    """任务模型"""

    def __init__(self, title: str, description: str = ""):
        self.id: Optional[int] = None
        self.title: str = title
        self.description: str = description
        self.status: TaskStatus = TaskStatus.TODO
        self.created_at: datetime = datetime.now()
        self.updated_at: datetime = datetime.now()

    def to_dict(self) -> dict:
        """转换为字典"""
        return {
            'id': self.id,
            'title': self.title,
            'description': self.description,
            'status': self.status.value,
            'created_at': self.created_at.isoformat(),
            'updated_at': self.updated_at.isoformat()
        }

    def update_status(self, new_status: TaskStatus):
        """更新任务状态"""
        self.status = new_status
        self.updated_at = datetime.now()

    def __repr__(self):
        return f'<Task {self.id}: {self.title}>'

class TaskRepository:
    """任务仓储"""

    def __init__(self):
        self._tasks: List[Task] = []
        self._next_id: int = 1

    def add(self, task: Task) -> Task:
        """添加任务"""
        task.id = self._next_id
        self._next_id += 1
        self._tasks.append(task)
        return task

    def get_all(self) -> List[Task]:
        """获取所有任务"""
        return self._tasks.copy()

    def get_by_id(self, task_id: int) -> Optional[Task]:
        """根据ID获取任务"""
        for task in self._tasks:
            if task.id == task_id:
                return task
        return None

    def delete(self, task_id: int) -> bool:
        """删除任务"""
        task = self.get_by_id(task_id)
        if task:
            self._tasks.remove(task)
            return True
        return False

# 全局任务仓储实例
task_repository = TaskRepository()

