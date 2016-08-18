///<reference path="../../bower_components/DefinitelyTyped/angularjs/angular.d.ts"/>

angular.module('tribe-app', [
    'website-services'
])

    .directive('app', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app.html',
            transclude: true,
            scope: {}
        };
    }])

    .directive('appFooter', [function () {
        return {
            restrict: 'A',
            scope: {},
            templateUrl: 'app/templates/app_footer.html',
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
            templateUrl: 'app/templates/app_header.html'
        };
    }])

    .run(function () {
        // placeholder
    });