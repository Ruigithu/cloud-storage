// jest-dom adds custom jest matchers for asserting on DOM nodes
import '@testing-library/jest-dom';

// Set up global mocks for URL methods
if (typeof window !== 'undefined') {
    // Mock URL methods
    window.URL.createObjectURL = jest.fn(() => 'blob:test-url');
    window.URL.revokeObjectURL = jest.fn();

    // Any other browser APIs you need to mock
}
if (typeof window === 'undefined') {
    const { JSDOM } = require('jsdom');
    const dom = new JSDOM('<!doctype html><html><body></body></html>', {
        url: 'http://localhost/'
    });

    global.window = dom.window;
    global.document = dom.window.document;
    global.navigator = dom.window.navigator;
    global.HTMLElement = dom.window.HTMLElement;
    global.Element = dom.window.Element;
    global.Node = dom.window.Node;
}

// React 18 act environment
global.IS_REACT_ACT_ENVIRONMENT = true;