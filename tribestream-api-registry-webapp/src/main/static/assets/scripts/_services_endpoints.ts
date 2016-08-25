///<reference path="../../bower_components/DefinitelyTyped/angularjs/angular.d.ts"/>
///<reference path="../../bower_components/DefinitelyTyped/underscore/underscore.d.ts"/>

module services {

    angular.module('website-services-endpoints', [
        'ngRoute',
        'ngResource',
        'ngCookies',
        'ngStorage',
        'tribe-alerts'
    ])

        .factory('tribeEndpointsService', [
            '$location', '$resource', '$http', 'tribeErrorHandlerService', '$sessionStorage',
            function ($location, $resource, $http, tribeErrorHandlerService, $sessionStorage) {
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
                var loadedRawEndpoints = [];
                var loadedEndpointDetails = [];
                return {
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
                                    var compiledResults = [];
                                    for (let rawEndpoint of data.results) {
                                        var existing = _.find(loadedRawEndpoints, function (existinRaw) {
                                            return existinRaw.endpointId === rawEndpoint.endpointId;
                                        });
                                        if (existing) {
                                            compiledResults.push(existing);
                                        } else {
                                            loadedRawEndpoints.push(rawEndpoint);
                                            compiledResults.push(rawEndpoint);
                                        }
                                    }
                                    successCallback({
                                        total: data.total,
                                        endpoints: compiledResults,
                                        applications: removeSelected(data.applications, 'a'),
                                        categories: removeSelected(data.categories, 'c'),
                                        tags: removeSelected(data.tags, 't'),
                                        roles: removeSelected(data.roles, 'r')
                                    });
                                }, errorCallback);
                            }
                        };
                    },
                    getDetails: function (app, httpMethod, path) {
                        return {
                            then: function (successCallback, errorCallback) {
                                if (httpMethod && path) {
                                    var existingEntry = _.find(loadedEndpointDetails, function (entry) {
                                        return entry.name === app && entry.methodName === httpMethod && entry.mapping === `/${path}`;
                                    });
                                    if (existingEntry) {
                                        successCallback(existingEntry);
                                    } else {
                                        $http.get(`api/alias/registry/endpoint/${app}/${httpMethod}/${path}`)
                                            .then(function (data) {
                                                loadedEndpointDetails.push(data.data);
                                                successCallback(data.data);
                                            }, tribeErrorHandlerService.ensureErrorHandler(errorCallback));
                                    }

                                } else {
                                    var newEntry = {};
                                    loadedEndpointDetails.push(newEntry);
                                    successCallback(newEntry);
                                }
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
