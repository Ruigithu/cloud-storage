// @ts-ignore
import React from 'react';
// @ts-ignore
import ReactDOM from 'react-dom/client';
import App from './App';


// 获取 root 节点
const rootElement = document.getElementById('root');
const root = ReactDOM.createRoot(rootElement); // 使用 createRoot 创建根实例

// 渲染应用
root.render(
    <React.StrictMode>
            <App />
    </React.StrictMode>
);
// script.js


