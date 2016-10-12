import './_components.ts';
import './_components_field_actions.ts';
import './_components_filters.ts';
import './_components_markdown.ts';
import './_components_multiselect.ts';
import './_components_singleselect.ts';
import './_components_textfield.ts';
import './_service_alerts.ts';
import './_services.ts';
import './_services_browser.ts';
import './_services_endpoints.ts';
import './app.ts';
import './app_messages.ts';
import './auth.ts';
import './endpoints.ts';
import './endpoints_details.ts';

require("../styles/app.sass");

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
                    template: require('../templates/page_endpoints.jade')
                })
                .when('/showcase', {
                    template: require('../templates/page_components.jade'),
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
                    template: require('../templates/page_see.jade'),
                    controller: ['$scope', '$routeParams', function ($scope, $routeParams) {
                        $scope.aggregatedId = $routeParams.aggregatedId;
                    }]
                })
                .when('/application/:applicationName*', {
                    template: require('../templates/page_application_details.jade'),
                    controller: ['$scope', '$routeParams', function ($scope, $routeParams) {
                        $scope.app = $routeParams.applicationName;
                    }]
                })
                .when('/application', {
                    templateUrl: 'app/templates/page_application_details.html',
                    controller: ['$scope', '$routeParams', function ($scope, $routeParams) {
                        //$scope.app = $routeParams.applicationName;
                    }]
                })
                .when('/endpoint/:application/:verb/:endpoint*', {
                    template: require('../templates/page_endpoints_details.jade'),
                    controller: ['$scope', '$routeParams', function ($scope, $routeParams) {
                        $scope.requestMetadata = {
                          applicationName: $routeParams.application,
                          verb: $routeParams.verb,
                          endpointPath: $routeParams.endpoint,
                          version: $routeParams.version
                        };
                    }]
                })
                .when('/endpoint/:application', {
                    template: require('../templates/page_endpoints_details.jade'),
                    controller: ['$scope', '$routeParams', function ($scope, $routeParams) {
                        $scope.requestMetadata = {
                          applicationName: $routeParams.application,
                          version: $routeParams.version
                        };
                    }]
                })
                .when('/login', {
                    template: require('../templates/page_login.jade')
                })
                .otherwise({
                    controller: ['$scope', '$location', function ($scope, $location) {
                        $scope.path = $location.path();
                    }],
                    template: require('../templates/page_not_implemented.jade')
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
