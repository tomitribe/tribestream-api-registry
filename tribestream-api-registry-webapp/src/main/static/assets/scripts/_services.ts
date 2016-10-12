angular.module('website-services', [
    'ngRoute',
    'ngResource',
    'ngCookies',
    'ngStorage',
    'website-browser',
    'tribe-alerts'
])

    .factory('tribeFilterService', ['$location',
        function ($location) {
            return {
                filterByCategory: function (app, category) {
                    $location.search({
                        a: app,
                        c: category
                    });
                    $location.path('/');
                },
                filterByRole: function (app, role) {
                    $location.search({
                        a: app,
                        r: role
                    });
                    $location.path('/');
                },
                filterByTag: function (app, tag) {
                    $location.search({
                        a: app,
                        t: tag
                    });
                    $location.path('/');
                }
            };
        }
    ])

    .factory('tribeAuthorizationService', ['Base64', '$cookieStore', '$http', '$sessionStorage',
        function (Base64, $cookieStore, $http, $sessionStorage) {
            var asBasic = function (token) {
                return 'Basic ' + token;
            };
            return {
                token: function () {
                    return $sessionStorage.tribe ? asBasic($sessionStorage.tribe.authtoken) : undefined;
                },
                login: function (credentials) {
                    if ($sessionStorage.tribe && $sessionStorage.tribe.authtoken) {
                        this.clearCredentials();
                    }
                    // we should instead send the Credential object in REQUEST_BODY mode
                    return $http.post('api/login',
                        $.param({username: credentials.username, password: credentials.password}),
                        {headers: {'Content-Type': 'application/x-www-form-urlencoded'}});
                },
                setCredentials: function (username, password) {
                    var encoded = Base64.encode(username + ':' + password);
                    $http.defaults.headers.common['Authorization'] = asBasic(encoded);
                    $sessionStorage.tribe.authtoken = encoded;
                    $sessionStorage.tribe.username = username;
                },
                restoreSession: function () {
                    var encoded = $sessionStorage.tribe.authtoken;
                    if (encoded != null) {
                        $http.defaults.headers.common['Authorization'] = asBasic(encoded);
                    }
                },
                clearCredentials: function () {
                    try {
                        document.execCommand('ClearAuthenticationCache');
                    } catch (e) {
                        // chrome does not support it
                    }
                    $sessionStorage.tribe = {};
                    delete $http.defaults.headers.common['Authorization'];
                }
            };
        }
    ])

    .factory('tribeServerService', ['$resource', '$http', 'tribeErrorHandlerService',
        function ($resource, $http, tribeErrorHandlerService) {
            var resource = $resource('api/server/info', null, {
                info: {method: 'GET', params: {}, isArray: false}
            });
            return {
                getInfo: function () {
                    return {
                        then: function (successCallback, errorCallback) {
                            resource.info({},
                                successCallback,
                                tribeErrorHandlerService.ensureErrorHandler(errorCallback)
                            );
                        }
                    };
                }
            };
        }
    ])

    .run(function () {
        // placeholder
    });