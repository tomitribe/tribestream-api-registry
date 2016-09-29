///<reference path="../../bower_components/DefinitelyTyped/angularjs/angular.d.ts"/>

angular.module('tribe-main', [
    'website-components',
    'website-services',
    'website-messages',
    'ngRoute',
    'ngStorage',
    'foundation',
    'tribe-app',
    'tribe-authentication',
    'tribe-endpoints',
    'tribe-endpoints-details'
])

    .config([
        '$locationProvider', '$routeProvider', '$httpProvider',
        function ($locationProvider, $routeProvider, $httpProvider) {
            // important so that we can intercept any 401 and fire
            // an authentication process
            $httpProvider.interceptors.push('httpInterceptor');

            $locationProvider.html5Mode({
                enabled: true,
                requireBase: true
            });
            $routeProvider
                .when('/', {
                    templateUrl: 'app/templates/page_endpoints.html'
                })
                .when('/showcase', {
                    templateUrl: 'app/templates/page_components.html',
                    controller: ['$scope', ($scope) => {
                        $scope.toUppercase = (item) => {
                            if (!item) {
                                return null;
                            }
                            if (item.text) {
                                return item.text.toUpperCase();
                            }
                            return item.toUpperCase();
                        };
                    }]
                })
                .when('/see/:aggregatedId', {
                    templateUrl: 'app/templates/page_see.html',
                    controller: ['$scope', '$routeParams', function ($scope, $routeParams) {
                        $scope.aggregatedId = $routeParams.aggregatedId;
                    }]
                })
                .when('/application/:app', {
                    templateUrl: 'app/templates/page_application_details.html',
                    controller: ['$scope', '$routeParams', function ($scope, $routeParams) {
                        $scope.app = $routeParams.app;
                    }]
                })
                .when('/application/:app/endpoint/:endpointId?', {
                    templateUrl: 'app/templates/page_endpoints_details.html',
                    controller: ['$scope', '$routeParams', function ($scope, $routeParams) {
                        $scope.applicationId = $routeParams.app;
                        $scope.endpointId = $routeParams.endpointId;
                    }]
                })
                .when('/login', {
                    templateUrl: 'app/templates/page_login.html'
                })
                .otherwise({
                    controller: ['$scope', '$location', function ($scope, $location) {
                        $scope.path = $location.path();
                    }],
                    templateUrl: 'app/templates/page_not_implemented.html'
                });
        }
    ])

    // should never be used cause we force the user to being logged to use the console
    .factory('httpInterceptor', ['$q', '$window', '$location', function ($q, $window, $location) {
        return {
            'responseError': function (response) {
                if (response.status === 401) {
                    $location.url('/login');
                }
                return $q.reject(response);
            }
        };
    }])

    .run(['$rootScope', function ($rootScope) {
        $rootScope.baseFullPath = angular.element('head base').first().attr('href');
    }])

    .run(['tribeAuthorizationService', '$sessionStorage', function (Authorization, $sessionStorage) {
        if ($sessionStorage.tribe == null) {
            $sessionStorage.tribe = {};
        } else {
            Authorization.restoreSession();
        }
    }])

    .run(['tribeAuthorizationService', '$sessionStorage', '$location', function (Authorization, $sessionStorage, $location) {
        // redirect to login in case the session storage is empty.
        if (!$sessionStorage.tribe.isConnected) {
            // save target path to be used once we successfuly log in.
            Authorization.targetPath = $location.path();
            $location.path("/login");
        }
    }])

    .run(function () {
        // placeholder
    });
