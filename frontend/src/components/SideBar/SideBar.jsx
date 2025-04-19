import { NavLink } from 'react-router-dom';
import './SideBar.css'

function Sidebar() {
    const navItems = [
        { path: '/home', icon: <i className="fa-solid fa-house" style={{color: "#8a8a8a"}}></i>, text: 'Home' },
        {path: '/my-shared', icon: <i className="fa-solid fa-share" style={{color: "#8a8a8a"}}></i>, text: 'My Shared'},
        // {path: '/collaboration', icon: <i className="fa-solid fa-user-group" style={{color: "#8a8a8a"}}></i>, text: 'Collaboration' },
        { path: '/bin', icon: <i className="fa-solid fa-trash" style={{color: "#8a8a8a"}}></i>, text: 'Bin' },
    ];

    return (
        <div className="aside-nav">
            {navItems.map((item) => (
                <NavLink
                    key={item.path}
                    to={item.path}
                    className={({ isActive }) =>
                        `aside-button ${
                            isActive ? 'active' : ''
                        }`
                    }
                >
                    <div className="">
                        <span className="mr-2">{item.icon}</span>
                        <span>  </span>
                        <span>  </span>
                        <span>{item.text}</span>
                    </div>
                </NavLink>
            ))}
        </div>
    );
}
export default Sidebar