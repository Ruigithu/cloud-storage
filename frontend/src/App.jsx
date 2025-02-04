
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Home from "./pages/Home/home";
import Login from "./pages/Login/login"
import Signup from "./pages/Signup/signup";
import MyShared from "./components/SideBar/SideBarNav/MySharred/MySharred";
import Collaboration from "./components/SideBar/SideBarNav/Collaboration/Collaboration";
import Bin from "./components/SideBar/SideBarNav/Bin/Bin";
 function App() {
    return(
     <BrowserRouter>
         <Routes>
             <Route path="/home" element={<Home />} />
             <Route path="/login" element={<Login/>}/>
             <Route path="/signup" element={<Signup/>}/>
             <Route path='' element={<Login/>}/>
             <Route path="/my-sharred" element={<MyShared />} />
             <Route path="/collaboration" element={<Collaboration />} />
             <Route path="/bin" element={<Bin />} />
         </Routes>
     </BrowserRouter>
    );
 }

export default App;
