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
            controller: ['$timeout', '$scope', 'tribeServerService', function ($timeout, $scope, server) {
                $scope.now = Date.now();
                server.getInfo().then(function (data) {
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.version = {
                                app: data.applicationVersion,
                                server: data.serverVersion
                            };
                        });
                    });
                });
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