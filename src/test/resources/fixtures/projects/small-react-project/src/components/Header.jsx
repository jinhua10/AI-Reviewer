import React, { useState } from 'react';

/**
 * 头部组件
 * 包含标题和添加任务表单
 */
function Header({ onAddTask }) {
  const [inputValue, setInputValue] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (inputValue.trim()) {
      onAddTask(inputValue);
      setInputValue('');
    }
  };

  return (
    <header className="header">
      <h1>📝 任务管理</h1>
      <form onSubmit={handleSubmit} className="add-task-form">
        <input
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          placeholder="添加新任务..."
          className="task-input"
        />
        <button type="submit" className="add-button">
          添加
        </button>
      </form>
    </header>
  );
}

export default Header;

