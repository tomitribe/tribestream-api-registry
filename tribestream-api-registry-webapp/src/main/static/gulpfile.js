var gulp = require('gulp');
var ts = require('gulp-typescript');
var tslint = require('gulp-tslint');
var sourcemaps = require('gulp-sourcemaps');
var uglify = require('gulp-uglify');
var concat = require('gulp-concat');
var del = require('del');
var gulpsync = require('gulp-sync')(gulp);
var jade = require('gulp-pug');
var sass = require('gulp-sass');
var es = require('event-stream');
var autoprefixer = require('gulp-autoprefixer');
var angularTemplateCache = require('gulp-angular-templatecache');
var cssWrap = require('gulp-css-wrap');
var merge = require('merge-stream');

gulp.task('css', gulpsync.sync(['images', 'css-build', 'css-third-party', 'css-third-party-resources']));
gulp.task('images', function () {
    return gulp.src('./assets/**/*.{gif,jpg,png,svg}')
        .pipe(gulp.dest('../../../target/static-resources/app/'));
});
gulp.task('css-build', gulpsync.sync(['sass', 'autoprefixer', 'css-concat']));
gulp.task('css-third-party', function () {
    var regular = merge(
        gulp.src([
        './bower_components/lato/css/lato.min.css',
        './bower_components/montserrat-webfont/css/montserrat-webfont.min.css',
        './bower_components/open-sans/css/open-sans.min.css',
        './bower_components/normalize-css/normalize.css',
        './bower_components/font-awesome/css/font-awesome.min.css',
        './bower_components/codemirror/lib/codemirror.css'
        ]),
        // wrap simplemde and highlightjs because it changes existing codemirror code.
        gulp.src([
            './bower_components/simplemde/dist/simplemde.min.css',
            './bower_components/highlightjs/styles/default.css'
        ]).pipe(cssWrap({selector: 'div[data-tribe-markdown]'}))
    ).pipe(concat('_.css')).pipe(gulp.dest('../../../target/static-resources/app/third-party/styles/'));
    var mocha = gulp.src([
        './bower_components/mocha/mocha.css'
    ]).pipe(concat('_tests.css')).pipe(gulp.dest('../../../target/static-resources/app/third-party/styles/'));
    es.concat(regular, mocha)
});
gulp.task('css-third-party-resources', function () {
    var font = gulp.src([
        './bower_components/lato/font/**/*'
    ]).pipe(gulp.dest('../../../target/static-resources/app/third-party/font'));
    var fonts = gulp.src([
        './bower_components/montserrat-webfont/fonts/**/*',
        './bower_components/open-sans/fonts/**/*',
        './bower_components/font-awesome/fonts/*'
    ]).pipe(gulp.dest('../../../target/static-resources/app/third-party/fonts'));
    var foundationTemplates = gulp.src([
        './bower_components/foundation-apps/js/angular/components/**/*.html'
    ]).pipe(gulp.dest('../../../target/static-resources/components'));
    return es.concat(font, fonts, foundationTemplates);
});
gulp.task('sass', function () {
    return gulp.src('./assets/**/*.sass')
        .pipe(sass({
            outputStyle: 'compressed'
        }).on('error', sass.logError))
        .pipe(gulp.dest('../../../target/static-resources/app/'));
});
gulp.task('autoprefixer', function () {
    return gulp.src('../../../target/static-resources/app/styles/app.css')
        .pipe(autoprefixer({
            browsers: ['last 4 versions']
        }))
        .pipe(gulp.dest('../../../target/static-resources/app/styles/'));
});
gulp.task('css-concat', function () {
    return gulp.src(['../../../target/static-resources/app/styles/sprite.css', '../../../target/static-resources/app/styles/app.css'])
        .pipe(concat('_.css'))
        .pipe(gulp.dest('../../../target/static-resources/app/styles/'))
});

gulp.task('html', gulpsync.sync(['jade', 'html-to-js', 'copy-templates']));
gulp.task('jade', function () {
    return gulp.src('./assets/**/*.jade')
        .pipe(jade({
            locals: {}
        }))
        .pipe(gulp.dest('../../../target/static-templates/html'))
});
gulp.task('html-to-js', function () {
    return gulp.src('../../../target/static-templates/html/*/*.html')
        .pipe(angularTemplateCache({
            filename: '_templates.js',
            root: 'app/',
            module: 'tribe-main'
        }))
        .pipe(gulp.dest('../../../target/static-templates/'))
});
gulp.task('copy-templates', function () {
    var asScripts = gulp.src('../../../target/static-templates/_templates.js')
        .pipe(gulp.dest('../../../target/static-resources/app/scripts/'));

    var asTemplates = gulp.src('../../../target/static-templates/html/templates/*')
        .pipe(gulp.dest('../../../target/static-resources/app/templates/'));

    return es.concat(asScripts, asTemplates);
});

gulp.task('js', gulpsync.sync(['compile-ts', 'copy-ts', 'js-third-party']));
gulp.task('js-test', gulpsync.sync(['compile-ts-test', 'copy-ts-test', 'js-third-party-test']));
gulp.task('compile-ts-test', function () {
    return gulp.src('./../../test/static/**/*.ts')
        .pipe(sourcemaps.init())
        .pipe(ts({
            'target': 'es5',
            'sourceMap': true,
            'out': '_tests.js'
        }))
        .pipe(uglify({
            mangle: false // otherwhise the sourcemap/debugger does not work properly.
        }))
        .pipe(sourcemaps.write({includeContent: false}))
        .pipe(gulp.dest('../../../target/static-resources/app/scripts/'));
});
gulp.task('copy-ts-test', function () {
    var testCode = gulp.src('./../../test/static/**/*.ts')
        .pipe(gulp.dest('../../../target/static-resources/app/scripts/'));
    return es.concat(testCode);
});
gulp.task('js-third-party-test', function () {
    var _1 = gulp.src([
        './bower_components/mocha/mocha.js',
        './bower_components/chai/chai.js'
    ]).pipe(concat('_tests_1.js')).pipe(gulp.dest('../../../target/static-resources/app/third-party/'));
    var _2 = gulp.src([
        './bower_components/underscore/underscore-min.js',
        './bower_components/jquery/dist/jquery.min.js',
        './bower_components/js-base64/base64.min.js',
        './bower_components/angular/angular.min.js',
        './bower_components/angular-route/angular-route.min.js',
        './bower_components/foundation-apps/dist/js/foundation-apps.min.js',
        './bower_components/ngstorage/ngStorage.min.js',
        './bower_components/angular-cookies/angular-cookies.min.js',
        './bower_components/angular-resource/angular-resource.min.js',
        './bower_components/codemirror/lib/codemirror.js',
        './bower_components/angular-ui-codemirror/ui-codemirror.min.js',
        './bower_components/codemirror/mode/markdown/markdown.js',
        './bower_components/marked/lib/marked.js',
        './bower_components/angular-marked/dist/angular-marked.min.js',
        './bower_components/simplemde/dist/simplemde.min.js',
        './bower_components/highlightjs/highlight.pack.js',
        './bower_components/angular-mocks/angular-mocks.js'
    ]).pipe(concat('_tests_2.js')).pipe(gulp.dest('../../../target/static-resources/app/third-party/'));
    return es.concat(_1, _2);
});

gulp.task('lint-ts', function () {
    return gulp.src('./assets/**/*.ts')
        .pipe(tslint())
        .pipe(tslint.report('prose'));
});
gulp.task('compile-ts', function () {
    return gulp.src('./assets/**/*.ts')
        .pipe(sourcemaps.init())
        .pipe(ts({
            'target': 'es5',
            'sourceMap': true,
            'out': '_.js'
        }))
        .pipe(uglify({
            mangle: false // otherwhise the sourcemap/debugger does not work properly.
        }))
        .pipe(sourcemaps.write({includeContent: false}))
        .pipe(gulp.dest('../../../target/static-resources/app/scripts/'));
});
gulp.task('copy-ts', function () {
    return gulp.src('./assets/**/*.ts')
        .pipe(gulp.dest('../../../target/static-resources/app/'));
});

gulp.task('js-third-party', function () {
    return es.concat(gulp.src([
        './bower_components/underscore/underscore-min.js',
        './bower_components/jquery/dist/jquery.min.js',
        './bower_components/js-base64/base64.min.js',
        './bower_components/angular/angular.min.js',
        './bower_components/angular-route/angular-route.min.js',
        './bower_components/foundation-apps/dist/js/foundation-apps.min.js',
        './bower_components/ngstorage/ngStorage.min.js',
        './bower_components/angular-cookies/angular-cookies.min.js',
        './bower_components/angular-resource/angular-resource.min.js',
        './bower_components/codemirror/lib/codemirror.js',
        './bower_components/angular-ui-codemirror/ui-codemirror.min.js',
        './bower_components/codemirror/mode/markdown/markdown.js',
        './bower_components/marked/lib/marked.js',
        './bower_components/angular-marked/dist/angular-marked.min.js',
        './bower_components/simplemde/dist/simplemde.min.js',
        './bower_components/highlightjs/highlight.pack.js'
    ]).pipe(concat('_.js')).pipe(gulp.dest('../../../target/static-resources/app/third-party/')));
});

gulp.task('copy-all', function () {
    return gulp.src(['../../../target/static-resources/**/*'])
        .pipe(gulp.dest('../../../target/apache-tomee/webapps/registry/'));
});

gulp.task('clean', function (callback) {
    return del([
        '../../../target/static-resources/',
        '../../../target/apache-tomee/webapps/registry/app/',
        '../../../target/apache-tomee/webapps/registry/components/'
    ], {
        force: true
    }, callback);
});

gulp.task('build', gulpsync.sync(['clean', 'html', 'js', 'js-test', 'css', 'copy-all']));

gulp.task('default', gulpsync.sync(['build']), function () {
    gulp.watch(
        ['./assets/**/*', '../../test/**/*'],
        gulpsync.sync(['build'])
    );
});
