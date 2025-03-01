import {useState} from "react";
import {useNavigate} from "react-router-dom";
import loginCss from "./login.css"

function Login(){
    const[username,setUsername]=useState('');
    const[password,setPassword]=useState('');
    const navigate =useNavigate();

    const handleSubmit = async (e)=>{
//form tag has the default action like
        e.preventDefault();

// URLSearchParams is a inner web api,it is kind of using the key-value
//form to store the data
//example:{username: "Alice"
//         password: "12345"}

        const formData = new URLSearchParams();
        formData.append('username', username.trim());
        formData.append('password', password.trim());

        try {
            const response = await fetch(`${process.env.REACT_APP_API_URL}/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Accept': 'application/json'  // 明确指定需要JSON响应
                },
                body: formData.toString(),
                credentials: 'include'  // 保留这个设置，它确实必要
            });

            //get the authentication response
            if (response.ok){
                    navigate("/home");

            }

        }catch (error){
            console.error('Login failed:', error);
        }

    }


    return(
        <div className="login-container">
            <form className="form-container" onSubmit={handleSubmit}>
                <label id="username"> username:
                    <br/>
                    <input className="signin-input"
                           type="text"
                        //when we bind input value to the const username,
                        // ensuring that the content of the input field is synchronized with username.
                        //When the user types in the input field,
                        //the onChange event updates the value of username, and in turn,
                        //the update of username causes the content of the input field to be updated.
                           value={username}
                           onChange={
                               (e) => setUsername(e.target.value)
                           }/>
                </label>
                <label>password:
                    <br/>
                    <input className="signin-input" id="password"
                           type="password"
                           value={password}
                           onChange={
                               (e) => setPassword(e.target.value)
                           }/>
                </label>
                <button className="signin-submit" type="submit">Sign in</button>
                <br/>
                <div><a href="/signup">not have an account?click here to signup</a></div>
            </form>
        </div>
    );

}
export  default Login;