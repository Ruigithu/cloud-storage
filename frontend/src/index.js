import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { Provider } from "react-redux";
import store from "./components/Tool/UserInfo/Store";


// get root element
const rootElement = document.getElementById('root');
const root = ReactDOM.createRoot(rootElement); // use createRoot to create root instance

// render  application
root.render(
    <React.StrictMode>
        <Provider store={store}>
            <App />
        </Provider>

    </React.StrictMode>
);



