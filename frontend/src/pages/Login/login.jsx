import {useState} from "react";
import {useNavigate} from "react-router-dom";
import loginCss from "./login.css"

function Login(){
    const[username,setUsername]=useState('');
    const[password,setPassword]=useState('');
    const navigate =useNavigate();

    const handleSubmit = async (e)=>{

        e.preventDefault();


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
                credentials: 'include'
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