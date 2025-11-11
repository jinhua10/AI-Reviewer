import React from 'react';

/**
 * 任务列表组件
 */
function TaskList({ tasks, onDelete }) {
  if (tasks.length === 0) {
    return <div className="empty-state">暂无任务</div>;
  }

  return (
    <ul className="task-list">
      {tasks.map(task => (
        <TaskItem key={task.id} task={task} onDelete={onDelete} />
      ))}
    </ul>
  );
}

/**
 * 任务项组件
 */
function TaskItem({ task, onDelete }) {
  return (
    <li className="task-item">
      <div className="task-content">
        <h3>{task.title}</h3>
        {task.description && <p>{task.description}</p>}
        <span className="task-status">{task.status}</span>
      </div>
      <button
        onClick={() => onDelete(task.id)}
        className="delete-button"
      >
        删除
      </button>
    </li>
  );
}

export default TaskList;

