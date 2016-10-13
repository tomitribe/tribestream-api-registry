var helpers = require('./helpers');

module.exports = {
    devtool: 'inline-source-map',

    resolve: {
        extensions: ['', '.ts', '.js']
    },

    module: {
        loaders: [
            {
                test: /\.ts$/,
                loaders: ['awesome-typescript-loader', 'angular2-template-loader']
            },
            {
                test: /\.pug$/,
                loader: 'pug-html-loader'
            },
            {
                test: /\.(png|jpe?g|gif|svg|woff|woff2|ttf|eot|ico)$/,
                loader: 'file?name=assets/[hash].[name].[ext]'
            },
            {
                test: /\.(png|jpe?g|gif|svg|woff|woff2|ttf|eot|ico)\?.*$/,
                loader: 'file?name=assets/[hash].[name].[ext]'
            },
            {
                test: /\.sass$/,
                include: [helpers.root('src', 'main', 'static', 'src', 'app')],
                loader: "raw-loader!postcss-loader!sass-loader"
            },
            {
                test: /\.sass\?global$/,
                include: [helpers.root('src', 'main', 'static', 'src', 'app')],
                loader: "style-loader!css-loader!postcss-loader!sass-loader"
            },
            {
                test: /\.css$/,
                include: [helpers.root('src', 'main', 'static', 'src', 'app')],
                loader: "raw-loader"
            },
            {
                test: /\.css$/,
                include: [helpers.root('src', 'main', 'static', 'bower_components')],
                loader: "style-loader!css-loader"
            }
        ]
    }
};
