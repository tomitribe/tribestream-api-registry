angular.module('tribe-app', [
    'website-services'
])

    .directive('app', [function () {
        return {
            restrict: 'A',
            template: require('../templates/app.jade'),
            transclude: true,
            scope: {}
        };
    }])

    .directive('appFooter', [function () {
        return {
            restrict: 'A',
            scope: {},
            template: require('../templates/app_footer.jade'),
            controller: ['$timeout', '$scope', function ($timeout, $scope) {
                $scope.now = Date.now();
            }]
        };
    }])

    .directive('appHeader', [function () {
        return {
            restrict: 'A',
            scope: {},
            template: require('../templates/app_header.jade')
        };
    }])

    .run(function () {
        // placeholder
    });