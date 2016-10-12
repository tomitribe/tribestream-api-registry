var webpackConfig = require('./webpack.config.test.js');

module.exports = {
    basePath: '',

    frameworks: ['jasmine'],

    files: [
        {pattern: './karma-test-shim.js', watched: false}
    ],

    preprocessors: {
        './karma-test-shim.js': ['webpack', 'sourcemap']
    },

    webpack: webpackConfig,

    webpackMiddleware: {
        stats: 'errors-only'
    },

    webpackServer: {
        noInfo: true
    },

    reporters: ['progress'],
    port: 9876,
    colors: true,
    browsers: [
        'PhantomJS'
        // 'Chrome'
    ]
};
