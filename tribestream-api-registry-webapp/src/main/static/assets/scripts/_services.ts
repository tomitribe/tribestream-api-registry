///<reference path="../../bower_components/DefinitelyTyped/angularjs/angular.d.ts"/>
///<reference path="../../bower_components/DefinitelyTyped/underscore/underscore.d.ts"/>

module services {

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

        .factory('tribeEndpointsService', [
            '$location', '$resource', '$http', 'tribeErrorHandlerService',
            function ($location, $resource, $http, tribeErrorHandlerService) {
                var httpListCall = function (params, successCallback, errorCallback) {
                    $http({
                        url: 'api/registry',
                        method: 'GET',
                        params: params
                    }).then(
                        successCallback,
                        tribeErrorHandlerService.ensureErrorHandler(errorCallback)
                    );
                };
                return {
                    getDetails: function (app, httpMethod, path) {
                        return {
                            then: function (successCallback, errorCallback) {
                                $http.get('api/alias/registry/endpoint/' + app + '/' + httpMethod + '/' + path)
                                    .then(function (data) {
                                        successCallback(data.data);
                                    }, tribeErrorHandlerService.ensureErrorHandler(errorCallback));
                            }
                        };
                    },
                    list: function () {
                        return {
                            then: function (successCallback, errorCallback) {
                                var params = {};
                                var rawParams = $location.search();
                                _.each(rawParams, function (value, key) {
                                    if ('a' === key) {
                                        params['app'] = value.split(',');
                                    } else if ('c' === key) {
                                        params['category'] = value.split(',');
                                    } else if ('t' === key) {
                                        params['tag'] = value.split(',');
                                    } else if ('r' === key) {
                                        params['role'] = value.split(',');
                                    } else if ('q' === key) {
                                        params['query'] = value;
                                    }
                                });
                                var removeSelected = function (list, qparam) {
                                    var param = rawParams[qparam];
                                    if (!param) {
                                        return list;
                                    }
                                    param = param.split(',');
                                    return _.filter(list, function (entry) {
                                        return !_.some(param, function (pEntry) {
                                            return entry.text === pEntry;
                                        });
                                    });
                                };
                                httpListCall(params, function (rawData) {
                                    var data = rawData.data;
                                    successCallback({
                                        total: data.total,
                                        endpoints: data.results,
                                        applications: removeSelected(data.applications, 'a'),
                                        categories: removeSelected(data.categories, 'c'),
                                        tags: removeSelected(data.tags, 't'),
                                        roles: removeSelected(data.roles, 'r')
                                    });
                                }, errorCallback);
                            }
                        };
                    },
                    listByApp: function (appName) {
                        return {
                            then: function (successCallback, errorCallback) {
                                httpListCall({
                                    'app': appName
                                }, function (rawData) {
                                    var data = rawData.data;
                                    successCallback({
                                        total: data.total,
                                        endpoints: data.results,
                                        categories: data.categories,
                                        tags: data.tags,
                                        roles: data.roles
                                    });
                                }, errorCallback);
                            }
                        };
                    },
                    getApplicationDetails: function (deployableId) {
                        return {
                            then: function (successCallback, errorCallback) {
                                $http.get('api/id/registry/application/' + deployableId)
                                    .then(function (data) {
                                        if (data.data && data.data.details && data.data.details.length) {
                                            // we will have at most one result. only one application queried.
                                            successCallback(data.data.details[0]);
                                        }
                                    }, tribeErrorHandlerService.ensureErrorHandler(errorCallback));
                            }
                        };
                    },
                    getSeeContent: function (aggregateId) {
                        return {
                            then: function (successCallback, errorCallback) {
                                $http.get('api/id/registry/see/' + aggregateId)
                                    .then(function (data) {
                                        successCallback(data.data);
                                    }, tribeErrorHandlerService.ensureErrorHandler(errorCallback)
                                );
                            }
                        };
                    }
                };
            }
        ]
    )

        .run(function () {
            // placeholder
        });
}
