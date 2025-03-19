
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Home from "./pages/Home/home";
import Login from "./pages/Login/login"
import Signup from "./pages/Signup/signup";
import MyShared from "./components/SideBar/SideBarNav/MySharred/MyShared";
import Bin from "./components/SideBar/SideBarNav/Bin/Bin";
import EditorPage from "./components/Tool/QuillEditor/EditPage";
import ShareHandler from "./pages/Share/ShareHandler";
import ShareCanceled from "./pages/Share/ShareCanceled";
 function App() {
    return(
     <BrowserRouter>
         <Routes>
             <Route path="/home" element={<Home />} />
             <Route path="/login" element={<Login/>}/>
             <Route path="/signup" element={<Signup/>}/>
             <Route path='' element={<Login/>}/>
             <Route path="/my-shared" element={<MyShared />} />
             <Route path="/bin" element={<Bin />} />
             <Route path="/editor/:fileId" element={<EditorPage/>}/>
             <Route path="/share/:shareId" element={<ShareHandler/>}/>
             <Route path="/shareCanceled" element={<ShareCanceled/>}/>


         </Routes>
     </BrowserRouter>
    );
 }

export default App;
