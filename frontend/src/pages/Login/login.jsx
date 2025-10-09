import {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import  "./login.css"
import {ERROR_MESSAGES, validateField, validateLoginForm} from "../../utils/validators";
import {loginAPI} from "../../services/authService";
import {saveAuthData} from "../../utils/authHelper";

function Login(){
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({email: false, password: false});
    const [loginError, setLoginError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();


    const validateForm = () => {
        const newErrors = validateLoginForm(email, password);
        setErrors(newErrors);
        return !newErrors.email && !newErrors.password;
    };


    const handleLoginSuccess = (data) => {
        if (data.token) {
            saveAuthData(data.token,data.email);
            navigate("/home");
        }
    };


    const performLogin = async () => {
        const credentials = {
            email: email.trim(),
            password: password.trim()
        };

        try {
            const data = await loginAPI(credentials);
            handleLoginSuccess(data);
        } catch (error) {
            console.error('Login failed:', error);
            setLoginError(error.message || ERROR_MESSAGES.LOGIN_FAILED);
        }
    };


    const handleSubmit = async (e) => {
        e.preventDefault();

        clearLoginErrorIfExists();
        setTouched({email: true, password: true});

        if (!validateForm()) {
            return;
        }

        setIsLoading(true);
        await performLogin();
        setIsLoading(false);
    };

    const clearLoginErrorIfExists = () => {
        if (loginError) {
            setLoginError('');
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        name === 'email' ? setEmail(value) : setPassword(value);
        clearLoginErrorIfExists();
    };

    const handleBlur = (e) => {
        const { name } = e.target;
        setTouched(prev => ({...prev, [name]: true}));
    };


    useEffect(() => {
        if (touched.email) {
            const usernameError = validateField('email', email);
            setErrors(prev => ({...prev, email: usernameError}));
        }
    }, [email, touched.email]);

    useEffect(() => {
        if (touched.password) {
            const passwordError = validateField('password', password);
            setErrors(prev => ({...prev, password: passwordError}));
        }
    }, [password, touched.password]);


    const renderErrorMessage = (fieldName) => {
        return errors[fieldName] && touched[fieldName] && (
            <div className="error-message">{errors[fieldName]}</div>
        );
    };

    const getInputClassName = (fieldName) => {
        return `signin-input ${errors[fieldName] && touched[fieldName] ? 'input-error' : ''}`;
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

                <label>
                    Email Address
                    <input
                        className={getInputClassName('email')}
                        name="email"
                        type="email"
                        placeholder="yourname@example.com"
                        value={email}
                        onChange={handleChange}
                        onBlur={handleBlur}
                        disabled={isLoading}
                    />
                    {renderErrorMessage('email')}
                </label>

                <label>
                    Password
                    <input
                        className={getInputClassName('password')}
                        name="password"
                        type="password"
                        placeholder="••••••••"
                        value={password}
                        onChange={handleChange}
                        onBlur={handleBlur}
                        disabled={isLoading}
                    />
                    {renderErrorMessage('password')}
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