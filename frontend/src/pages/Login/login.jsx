import {useCallback, useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import  "./login.css"



function Login(){
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({username: false, password: false});
    const [loginError, setLoginError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    // 验证电子邮件格式
    const isValidEmail = (email) => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    };

    // 验证单个字段
    const validateField =useCallback( (name, value) => {
        switch (name) {
            case 'username':
                if (!value.trim()) {
                    return "email can't be empty";
                } else if (!isValidEmail(value.trim())) {
                    return "invalid email address";
                }
                return "";
            case 'password':
                if (!value) {
                    return "password can't be empty";
                } else if (value.length < 6) {
                    return "password at least has 6 characters";
                }
                return "";
            default:
                return "";
        }
    },[]);

    // 验证整个表单
    const validateForm = () => {
        const newErrors = {
            username: validateField('username', username),
            password: validateField('password', password)
        };

        setErrors(newErrors);
        return !newErrors.username && !newErrors.password;
    };

    // 当输入字段变化时验证
    useEffect(() => {
        if (touched.username) {
            const usernameError = validateField('username', username);
            setErrors(prev => ({...prev, username: usernameError}));
        }
    }, [username, touched.username,validateField]);

    useEffect(() => {
        if (touched.password) {
            const passwordError = validateField('password', password);
            setErrors(prev => ({...prev, password: passwordError}));
        }
    }, [password, touched.password,validateField]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoginError('');

        // 标记所有字段为已触碰
        setTouched({username: true, password: true});

        // 表单验证
        if (!validateForm()) {
            return;
        }

        setIsLoading(true);

        const formData = new URLSearchParams();
        formData.append('username', username.trim());
        formData.append('password', password.trim());

        try {
            const response = await fetch(`${process.env.REACT_APP_API_URL}/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Accept': 'application/json'
                },
                body: formData.toString(),
            });

            // 获取认证响应
            if (response.ok) {
                const data = await response.json();
                if (data.token) {
                    localStorage.setItem('token', data.token);
                    localStorage.setItem('isLoggedIn', 'true');
                    if (data.username) {
                        localStorage.setItem('username', data.username);
                    }
                    navigate("/home");
                }
            } else {
                // 处理认证失败
                const errorData = await response.json().catch(() => ({}));
                setLoginError(errorData.message || 'invalid password or email, please try again');
            }
        } catch (error) {
            console.error('Login failed:', error);
            setLoginError('login failed');
        } finally {
            setIsLoading(false);
        }
    }

    // 处理输入变化
    const handleChange = (e) => {
        const { name, value } = e.target;

        if (name === 'username') {
            setUsername(value);
        } else if (name === 'password') {
            setPassword(value);
        }

        if (loginError) {
            setLoginError('');
        }
    };

    // 处理失去焦点事件
    const handleBlur = (e) => {
        const { name } = e.target;
        setTouched(prev => ({...prev, [name]: true}));
    };

    return (
        <div className="login-container">
            <form className="form-container" onSubmit={handleSubmit}>
                <div className="form-header">
                    <h1>Welcome Back</h1>
                    <p>Sign in to your account</p>
                </div>

                {loginError && (
                    <div className="error-message global-error">{loginError}</div>
                )}

                <label htmlFor="username">
                    Email Address
                    <input
                        className={`signin-input ${errors.username && touched.username ? 'input-error' : ''}`}
                        id="username"
                        name="username"
                        type="email"
                        placeholder="yourname@example.com"
                        value={username}
                        onChange={handleChange}
                        onBlur={handleBlur}
                        disabled={isLoading}
                    />
                    {errors.username && touched.username && (
                        <div className="error-message">{errors.username}</div>
                    )}
                </label>

                <label htmlFor="password">
                    Password
                    <input
                        className={`signin-input ${errors.password && touched.password ? 'input-error' : ''}`}
                        id="password"
                        name="password"
                        type="password"
                        placeholder="••••••••"
                        value={password}
                        onChange={handleChange}
                        onBlur={handleBlur}
                        disabled={isLoading}
                    />
                    {errors.password && touched.password && (
                        <div className="error-message">{errors.password}</div>
                    )}
                </label>

                <button
                    className="signin-submit"
                    type="submit"
                    disabled={isLoading}
                >
                    {isLoading ? 'Signing in...' : 'Sign in'}
                </button>

                <div className="signup-link">
                    <a href="/signup">Don't have an account? Sign up</a>
                </div>
            </form>
        </div>
    );
}

export default Login;