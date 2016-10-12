var webpack = require('webpack');
var webpackMerge = require('webpack-merge');
var commonConfig = require('./webpack.common.js');

module.exports = webpackMerge(commonConfig, {
    devtool: 'none',

    plugins: [
        new webpack.NoErrorsPlugin(),
        new webpack.optimize.DedupePlugin(),
        //new webpack.optimize.UglifyJsPlugin({ // https://github.com/angular/angular/issues/10618
        //    mangle: false
        //}),

        new webpack.DefinePlugin({
            PRODUCTION: JSON.stringify(true)
        })
    ]
});
