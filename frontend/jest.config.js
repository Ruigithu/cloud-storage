module.exports = {
    collectCoverage: true,
    coverageDirectory: 'coverage',
    collectCoverageFrom: [
        'src/**/*.{js,jsx,ts,tsx}',
        '!src/**/*.test.{js,jsx}',
        '!src/index.js',
        '!src/reportWebVitals.js'
    ]
};