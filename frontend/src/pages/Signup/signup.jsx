import {useState} from "react";
import {useNavigate} from "react-router-dom";
import signupCss from  "./signup.css"

function Signup(){
    const[username,setUsername]=useState('');
    const[password,setPassword]=useState('');
    const[firstname,setFirstname]=useState('');
    const[lastname,setLastname]=useState('');

    const navigate =useNavigate();

    const handleSubmit = async (e)=>{
        e.preventDefault();
        if (!username || !password || !firstname || !lastname) {
            alert('you have to fill all the fields！');
            return;
        }

        const userData = {
            username: username.trim(),
            password: password.trim(),
            firstname: firstname.trim(),
            lastname: lastname.trim()
        };

        console.log('the sending data:', userData);

        try {
            const response = await fetch(
                'http://localhost:8080/signup', {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json',
                    },
                    body: JSON.stringify(userData)
                });

            //get the authentication response
            if (response.ok){
                const data = await response.json();
                localStorage.setItem("token","");
                navigate("/login")
            }

        }catch (error){
            console.error('Signup failed:', error);
        }

    }


    return(
        <div className="signup-container">
            <form className="form-container" onSubmit={handleSubmit}>
                <label id="username"> username:
                    <br/>
                    <input className="signup-input"
                           type="text"
                           value={username}
                           onChange={
                               (e) => setUsername(e.target.value)
                           }/>
                </label>
                <label>password:
                    <br/>
                    <input className="signup-input" id="password"
                           type="password"
                           value={password}
                           onChange={
                               (e) => setPassword(e.target.value)
                           }/>
                </label>
                <label>firstname:
                    <br/>
                    <input className="signup-input" id="firstname"
                           type="text"
                           value={firstname}
                           onChange={
                               (e) => setFirstname(e.target.value)
                           }/>
                </label>
                <label>lastname:
                    <br/>
                    <input className="signup-input" id="lastname"
                           type="text"
                           value={lastname}
                           onChange={
                               (e) => setLastname(e.target.value)
                           }/>
                </label>
                <button className="signup-submit" type="submit">Sign in</button>
            </form>

        </div>
    );

}

export default Signup;