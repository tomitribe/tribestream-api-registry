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
import './_services_header_providers.ts';
import './app.ts';
import './app_messages.ts';
import './auth.ts';
import './endpoints.ts';
import './endpoints_details.ts';

require("../styles/app.sass");

module tribe_main {

// https://webpack.github.io/docs/list-of-plugins.html#defineplugin
declare var PRODUCTION: boolean;

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
        '$locationProvider', '$routeProvider', '$httpProvider', '$logProvider',
        function ($locationProvider, $routeProvider, $httpProvider, $logProvider) {
            $logProvider.debugEnabled(false); // GUI is really too slow, let's use a debugger if needed

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
                    template: require('../templates/page_application_details.jade'),
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
    .factory('httpInterceptor', ['$q', '$window', '$location', 'currentAuthProvider',
        function ($q, $window, $location, currentAuthProvider) {
            return {
                'request': function(config) {
                    if (config.url != 'api/security/oauth2' && currentAuthProvider.isActive()) {
                        return currentAuthProvider.get().getAuthorizationHeader().then(function(token) {
                            config.headers['Authorization'] = token;
                            return config;
                        });
                    }
                    return config;
                },
                'responseError': function (response) {
                    if (response.status === 401) {
                        $location.url('/login');
                    }
                    return $q.reject(response);
                }
            };
        }
    ])

    .run(['$rootScope', function ($rootScope) {
        $rootScope.baseFullPath = angular.element('head base').first().attr('href');
    }])

    .run(['tribeAuthorizationService', 'currentAuthProvider', function (Authorization, currentProvider) {
        if (Authorization.isConnected()) {
            const provider = Authorization.restoreSession();
            if (provider) {
                currentProvider.set(provider);
            }
        }
    }])

    .run(['tribeAuthorizationService', '$location', function (Authorization, $location) {
        // redirect to login in case the session storage is empty.
        if (!Authorization.isConnected()) {
            // save target path to be used once we successfuly log in.
            Authorization.targetPath = $location.path();
            $location.path("/login");

        } else if ($location.path() === "/login") {
            $location.path("/");
        }
    }])

    .config(['$logProvider', function ($logProvider) {
        if (PRODUCTION) {
            $logProvider.debugEnabled(false);
        }
    }]);

}
