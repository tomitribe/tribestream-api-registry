var webpack = require('webpack');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var EncodingPlugin = require('webpack-encoding-plugin');
var helpers = require('./helpers');
var precss = require('precss');
var autoprefixer = require('autoprefixer');

module.exports = {
    entry: {
        'polyfills': './assets/scripts/polyfills.ts',
        'vendor': './assets/scripts/vendor.ts',
        'app': './assets/scripts/_main.ts'
    },

    output: {
        path: helpers.root('target/static-resources/app'),
        filename: '[name].js',
        publicPath: 'app/',
        chunkFilename: '[id].chunk.js'
    },

    htmlLoader: {
        minimize: false
    },

    resolve: {
        extensions: ['', '.js', '.ts']
    },

    externals: {
        "marked": "marked"
    },

    module: {
        // TODO
        //preLoaders: [
        //    {
        //        test: /\.ts$/,
        //        loader: "tslint-loader"
        //    }
        //],
        loaders: [
            {
                test: /\.ts$/,
                loaders: ['awesome-typescript-loader']
            },
            {
                test: /\.jade$/,
                loader: 'pug-html-loader'
            },
            {
                test: /\.(png|jpe?g|gif|svg|woff|woff2|ttf|eot|ico)$/,
                loader: 'file?name=assets/[name]_[hash].[ext]'
            },
            {
                test: /\.(png|jpe?g|gif|svg|woff|woff2|ttf|eot|ico)\?.*$/,
                loader: 'file?name=assets/[name]_[hash].[ext]'
            },
            {
                test: /\.sass$/,
                loader: "style-loader!css-loader!postcss-loader!sass-loader"
            },
            {
                test: /\.css$/,
                loader: "style-loader!css-loader"
            }
        ]
    },

    postcss: function () {
        return [precss, autoprefixer];
    },

    tslint: {
        // tslint errors are displayed by default as warnings
        // set emitErrors to true to display them as errors
        emitErrors: false,

        // tslint does not interrupt the compilation by default
        // if you want any file with tslint errors to fail
        // set failOnHint to true
        failOnHint: true,

        // These options are useful if you want to save output to files
        // for your continuous integration server
        fileOutput: {
            // The directory where each file's report is saved
            dir: "../../../target/ts-lint-reports/",

            // The extension to use for each report's filename. Defaults to "txt"
            ext: "xml",

            // If true, all files are removed from the report directory at the beginning of run
            clean: true,

            // A string to include at the top of every report file.
            // Useful for some report formats.
            header: "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<checkstyle version=\"5.7\">",

            // A string to include at the bottom of every report file.
            // Useful for some report formats.
            footer: "</checkstyle>"
        }
    },

    plugins: [
        new EncodingPlugin({
            encoding: 'utf-8'
        }),
        new webpack.optimize.CommonsChunkPlugin({
            name: ['app', 'vendor', 'polyfills']
        }),
        new HtmlWebpackPlugin({
            // disable default loader and simply load the raw jsp text
            template: '!!raw!../webapp/index.jsp',
            // replace the original index.jsp by this one manipulated file
            filename: '../index.jsp',
            favicon: './assets/images/favicon.png',
            chunks: ['app', 'vendor', 'polyfills'],
            hash: true
        })
    ]
};
