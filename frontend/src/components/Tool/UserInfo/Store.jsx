// src/store/index.js
import { configureStore } from '@reduxjs/toolkit';
import userReducer from './userSlice'; // 导入 userSlice

const store = configureStore({
    reducer: {
        user: userReducer, // 添加 user 相关的 reducer
    }
});

export default store;
