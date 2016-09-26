angular.module('website-components-filters', [])
    
    .filter('uriencode', ['$window', ($window) => $window.encodeURIComponent])

    .filter('pathencode', [() => (input) => {
        // The root path is the most simple case
        if (input === '/') {
            return '/';
        }
        return input.split('/')
            .map((part) => {
                return part.match('\{.*\}') ? ':' + part.slice(1, -1) : part
            })
            .join('/');
    }])

    .filter('tribeHtml', ['$sce', ($sce) => (input) => $sce.trustAsHtml(input)])

    .filter('tribeHtmlText', [() => (input) => {
        var el = angular.element('<div></div>');
        el.append(input);
        return el.text();
    }]);
