var webpack = require('webpack');
var webpackMerge = require('webpack-merge');
var commonConfig = require('./webpack.common.js');
var helpers = require('./helpers');

var docBase = (process.env.DOC_BASE || helpers.root('target/apache-tomee/webapps/registry')) + '/app';

console.log('Using base=' + docBase);

module.exports = webpackMerge(commonConfig, {
    devtool: 'source-map',

    output: {
        path: docBase,
        filename: '[name].js',
        chunkFilename: '[id].chunk.js'
    },

    plugins: [
        new webpack.DefinePlugin({
            PRODUCTION: JSON.stringify(false)
        })
    ]
});
