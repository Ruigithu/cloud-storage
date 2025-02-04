// src/store/userSlice.js
import { createSlice } from '@reduxjs/toolkit';

const initialState = {
    userInfo: null, // 存储用户信息
};

const userSlice = createSlice({
    name: 'user',
    initialState,
    reducers: {
        setUserInfo: (state, action) => {
            state.userInfo = action.payload; // 更新用户信息
        },
        clearUserInfo: (state) => {
            state.userInfo = null; // 退出登录时清除用户信息
        }
    }
});

// 导出 actions
export const { setUserInfo, clearUserInfo } = userSlice.actions;

// 导出 reducer
export default userSlice.reducer;
