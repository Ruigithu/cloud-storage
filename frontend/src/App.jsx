
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Home from "./pages/Home/home";
import Login from "./pages/Login/login"
import Signup from "./pages/Signup/signup";

 function App() {
    return(
     <BrowserRouter>
         <Routes>
             <Route path="/home" element={<Home />} />
             <Route path="/login" element={<Login/>}/>
             <Route path="/signup" element={<Signup/>}/>
         </Routes>
     </BrowserRouter>
    );
 }

export default App;
