angular.module('website-components-filters', [

])
    .filter('uriencode', ['$window', function ($window) {
        return $window.encodeURIComponent;
    }])

    .filter('pathencode', [function () {
        return function (input) {
            // The root path is the most simple case
            if (input === '/') {
                return '/';
            }
            return input.split('/')
                .map((part) => {
                    return part.match('\{.*\}') ? ':' + part.slice(1, -1) : part
                })
                .join('/');
        }
    }])

    .filter('tribeHtml', ['$sce', function ($sce) {
        return function (input) {
            return $sce.trustAsHtml(input);
        }
    }])

    .filter('tribeHtmlText', [function () {
        return function (input) {
            var el = angular.element('<div></div>');
            el.append(input);
            return el.text();
        }
    }]);
