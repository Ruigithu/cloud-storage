export const saveAuthData = (token,username) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', username);
};