// @ts-ignore
import React from 'react';
// @ts-ignore
import ReactDOM from 'react-dom/client';
import App from './App';


// get root element
const rootElement = document.getElementById('root');
const root = ReactDOM.createRoot(rootElement); // use createRoot to create root instance

// render  application
root.render(
    <React.StrictMode>
       <App />
    </React.StrictMode>
);



