import {useState, useEffect, useCallback} from "react";
import { useNavigate } from "react-router-dom";
import "./signup.css";

function Signup() {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({ name: false, email: false, password: false });
    const [signupError, setSignupError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    // 验证电子邮件格式
    const isValidEmail = (email) => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    };

    // 验证单个字段
    const validateField =useCallback ((name, value) => {
        switch (name) {
            case 'name':
                if (!value.trim()) {
                    return "Name can't be empty";
                }
                return "";
            case 'email':
                if (!value.trim()) {
                    return "Email can't be empty";
                } else if (!isValidEmail(value.trim())) {
                    return "Invalid email address";
                }
                return "";
            case 'password':
                if (!value) {
                    return "Password can't be empty";
                } else if (value.length < 6) {
                    return "Password must be at least 6 characters";
                }
                return "";
            default:
                return "";
        }
    },[]);

    // 验证整个表单
    const validateForm = () => {
        const newErrors = {
            name: validateField('name', name),
            email: validateField('email', email),
            password: validateField('password', password)
        };

        setErrors(newErrors);
        return !newErrors.name && !newErrors.email && !newErrors.password;
    };

    // 当输入字段变化时验证
    useEffect(() => {
        if (touched.name) {
            const nameError = validateField('name', name);
            setErrors(prev => ({ ...prev, name: nameError }));
        }
    }, [name, touched.name,validateField]);

    useEffect(() => {
        if (touched.email) {
            const emailError = validateField('email', email);
            setErrors(prev => ({ ...prev, email: emailError }));
        }
    }, [email, touched.email,validateField]);

    useEffect(() => {
        if (touched.password) {
            const passwordError = validateField('password', password);
            setErrors(prev => ({ ...prev, password: passwordError }));
        }
    }, [password, touched.password,validateField]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSignupError('');

        // 标记所有字段为已触碰
        setTouched({ name: true, email: true, password: true });

        // 表单验证
        if (!validateForm()) {
            return;
        }

        setIsLoading(true);

        const userData = {
            name: name.trim(),
            email: email.trim(),
            passwordHash: password.trim() // 在后端，变量名是 passwordHash
        };

        try {
            const response = await fetch(
                `${process.env.REACT_APP_API_URL}/signup`, {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json',
                    },
                    body: JSON.stringify(userData)
                });

            if (response.ok) {
                navigate("/login");
            } else {
                // 处理注册失败
                const errorData = await response.json().catch(() => ({}));
                setSignupError(errorData.message || 'Registration failed. Please try again.');
            }
        } catch (error) {
            console.error('Signup failed:', error);
            setSignupError('Registration failed. Please try again later.');
        } finally {
            setIsLoading(false);
        }
    };

    // 处理输入变化
    const handleChange = (e) => {
        const { name, value } = e.target;

        if (name === 'name') {
            setName(value);
        } else if (name === 'email') {
            setEmail(value);
        } else if (name === 'password') {
            setPassword(value);
        }

        if (signupError) {
            setSignupError('');
        }
    };

    // 处理失去焦点事件
    const handleBlur = (e) => {
        const { name } = e.target;
        setTouched(prev => ({ ...prev, [name]: true }));
    };

    return (
        <div className="signup-container">
            <form className="form-container" onSubmit={handleSubmit}>
                <div className="form-header">
                    <h1>Create Your Account</h1>
                    <p>Fill out the form to get started</p>
                </div>

                {signupError && (
                    <div className="error-message global-error">{signupError}</div>
                )}

                <label htmlFor="name">
                    Full Name
                    <input
                        className={`signup-input ${errors.name && touched.name ? 'input-error' : ''}`}
                        id="name"
                        name="name"
                        type="text"
                        placeholder="Rui Peng"
                        value={name}
                        onChange={handleChange}
                        onBlur={handleBlur}
                        disabled={isLoading}
                    />
                    {errors.name && touched.name && (
                        <div className="error-message">{errors.name}</div>
                    )}
                </label>

                <label htmlFor="email">
                    Email Address
                    <input
                        className={`signup-input ${errors.email && touched.email ? 'input-error' : ''}`}
                        id="email"
                        name="email"
                        type="email"
                        placeholder="yourname@example.com"
                        value={email}
                        onChange={handleChange}
                        onBlur={handleBlur}
                        disabled={isLoading}
                    />
                    {errors.email && touched.email && (
                        <div className="error-message">{errors.email}</div>
                    )}
                </label>

                <label htmlFor="password">
                    Password
                    <input
                        className={`signup-input ${errors.password && touched.password ? 'input-error' : ''}`}
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
                    className="signup-submit"
                    type="submit"
                    disabled={isLoading}
                >
                    {isLoading ? 'Creating Account...' : 'Sign Up'}
                </button>

                <div className="login-link">
                    <a href="/login">Already have an account? Sign in</a>
                </div>
            </form>
        </div>
    );
}

export default Signup;