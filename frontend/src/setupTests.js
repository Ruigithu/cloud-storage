import '@testing-library/jest-dom';

if (typeof window !== 'undefined') {
    window.URL.createObjectURL = jest.fn(() => 'blob:test-url');
    window.URL.revokeObjectURL = jest.fn();

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

global.IS_REACT_ACT_ENVIRONMENT = true;