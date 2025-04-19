import { createSlice } from '@reduxjs/toolkit';
const initialState = {
    userInfo: null,
};
const userSlice = createSlice({
    name: 'user',
    initialState,
    reducers: {
        setUserInfo: (state, action) => {
            state.userInfo = action.payload; // update
        },
        clearUserInfo: (state) => {
            state.userInfo = null; // clean
        }
    }
});

// export actions
export const {
    setUserInfo,
    clearUserInfo } = userSlice.actions;
// export reducer
export default userSlice.reducer;
