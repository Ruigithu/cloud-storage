import {useState, useEffect} from "react";
import { useNavigate } from "react-router-dom";
import "./signup.css";
import {ERROR_MESSAGES, validateField, validateSignUpForm} from "../../utils/validators";
import {signupAPI} from "../../services/authService";

function Signup() {
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({ username: false, email: false, password: false });
    const [signupError, setSignupError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();


    const validateForm = () => {
        const newErrors = validateSignUpForm(username,email,password);
        setErrors(newErrors);
        return !newErrors.username &&!newErrors.email && !newErrors.password;
    };


    useEffect(() => {
        if (touched.username) {
            const nameError = validateField('username', username);
            setErrors(prev => ({ ...prev, username: nameError }));
        }
    }, [username, touched.username]);

    useEffect(() => {
        if (touched.email) {
            const emailError = validateField('email', email);
            setErrors(prev => ({ ...prev, email: emailError }));
        }
    }, [email, touched.email]);

    useEffect(() => {
        if (touched.password) {
            const passwordError = validateField('password', password);
            setErrors(prev => ({ ...prev, password: passwordError }));
        }
    }, [password, touched.password]);

    const handleSignupSuccess = (data) => {
        console.log('Signup successful:', data);
        navigate("/login");
    };


    const performSignup = async () => {
        const userData = {
            email: email.trim(),
            name:username.trim(),
            password: password.trim()
        };

        try {
            const data = await signupAPI(userData);
            handleSignupSuccess(data);
        } catch (error) {
            console.error('Signup failed:', error);
            setSignupError(error.message || ERROR_MESSAGES.SIGNUP_FAILED);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        setSignupError('');
        setTouched({
            username: true,
            email: true,
            password: true
        });

        if (!validateForm()) {
            return;
        }

        setIsLoading(true);
        await performSignup();
        setIsLoading(false);
    };

    const clearSignupErrorIfExists = () => {
        if (signupError) {
            setSignupError('');
        }
    };


    const handleChange = (e) => {
        const { name, value } = e.target;
        if (name === 'username') {
            setUsername(value);
        } else if (name === 'email') {
            setEmail(value);
        } else if (name === 'password') {
            setPassword(value);
        }

        clearSignupErrorIfExists();
    };

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

                <label>
                    Full Name
                    <input
                        className={`signup-input ${errors.name && touched.name ? 'input-error' : ''}`}
                        name="username"
                        type="text"
                        placeholder="Rui Peng"
                        value={username}
                        onChange={handleChange}
                        onBlur={handleBlur}
                        disabled={isLoading}
                    />
                    {errors.username && touched.username && (
                        <div className="error-message">{errors.username}</div>
                    )}
                </label>

                <label>
                    Email Address
                    <input
                        className={`signup-input ${errors.email && touched.email ? 'input-error' : ''}`}
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

                <label>
                    Password
                    <input
                        className={`signup-input ${errors.password && touched.password ? 'input-error' : ''}`}
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