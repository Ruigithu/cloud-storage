
const apiRequest = async (url, options = {}) => {
    const token = localStorage.getItem('token');

    if (token) {
        options.headers = {
            "Content-Type": "application/json",
            ...options.headers,
            'Authorization': `Bearer ${token}`
        };
    }

    if (options.body instanceof FormData) {
        delete options.headers["Content-Type"];
    }

    const response = await fetch(url, options);

    if (response.status === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('isLoggedIn');
        console.log("401");
        window.location.href = '/login';
        return null;
    }

    return response;
};

export default apiRequest;