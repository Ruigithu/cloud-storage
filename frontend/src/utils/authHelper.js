export const saveAuthData = (token,username) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', username);
};

export const clearAuthData = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
};

export const getUser = () =>  localStorage.getItem('user');
export const getToken = () => localStorage.getItem('token');

export const isAuthenticated = () => !!getToken();