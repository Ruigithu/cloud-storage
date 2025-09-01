import {useState} from "react";
import {useNavigate} from "react-router-dom";
import   "./signup.css"

function Signup(){
    const[passwordHash,setPasswordHash]=useState('');
    const[name,setName]=useState('');
    const[email,setEmail]=useState('');

    const navigate =useNavigate();

    const handleSubmit = async (e)=>{
        e.preventDefault();
        if (!name || !passwordHash || !email) {
            alert('you have to fill all the fields！');
            return;
        }

        const userData = {
            name: name.trim(),
            email: email.trim(),
            passwordHash: passwordHash.trim()
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

            //get the authentication response
            if (response.ok){
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
                <label id="name"> name:
                    <br/>
                    <input className="signup-input"
                           type="text"
                           value={name}
                           onChange={
                               (e) => setName(e.target.value)
                           }/>
                </label>

                <label>email:
                    <br/>
                    <input className="signup-input" id="firstname"
                           type="text"
                           value={email}
                           onChange={
                               (e) => setEmail(e.target.value)
                           }/>
                </label>
                <label>password:
                    <br/>
                    <input className="signup-input" id="password"
                           type="password"
                           value={passwordHash}
                           onChange={
                               (e) => setPasswordHash(e.target.value)
                           }/>
                </label>
                <button className="signup-submit" type="submit">Sign up</button>
            </form>

        </div>
    );

}

export default Signup;