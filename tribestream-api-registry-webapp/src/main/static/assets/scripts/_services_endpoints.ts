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
        .factory('tribeLinkHeaderService', [
            function() {
                return {
                    parseLinkHeader: function (linkHeader) {
                        if (!linkHeader) {
                            return {};
                        }
                        // Split parts by comma
                        let parts = linkHeader.split(',');
                        let links = {};
                        // Parse each part into a named link
                        _.each(parts, function(p) {
                            let section = p.split(';');
                            if (section.length != 2) {
                              throw new Error("section could not be split on ';'");
                            }
                            var url = section[0].replace(/<(.*)>/, '$1').trim();
                            var name = section[1].replace(/rel="(.*)"/, '$1').trim();
                            links[name] = url;
                        });
                        return links;
                    }
                };
            }
        ])
        .factory('tribeEndpointsService', [
            '$location', '$resource', '$http', 'tribeErrorHandlerService', '$sessionStorage', '$filter', 'tribeLinkHeaderService',
            function ($location, $resource, $http, tribeErrorHandlerService, $sessionStorage, $filter, tribeLinkHeaderService) {
                var httpListCall = function (url, params, successCallback, errorCallback) {
                    $http({
                        url: url,
                        method: 'GET',
                        params: params
                    }).then(
                        successCallback,
                        tribeErrorHandlerService.ensureErrorHandler(errorCallback)
                    );
                };
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
                                httpListCall('api/registry', params, function (rawData) {
                                    var data = rawData.data;
                                    var compiledResults = [];
                                    for (let rawEndpoint of data.results) {
                                        compiledResults.push(rawEndpoint);
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
                    getApplicationDetails: function (applicationId) {
                        return {
                            then: function (successCallback, errorCallback) {
                                $http.get('api/application/' + applicationId)
                                    .then(function (data) {
                                        if (data && data.data && data.data.swagger) {
                                            // we will have at most one result. only one application queried.
                                            successCallback(data);
                                        }
                                    }, tribeErrorHandlerService.ensureErrorHandler(errorCallback));
                            }
                        };
                    },
                    getDetails: function (app, endpointId) {
                        return {
                            then: function (successCallback, errorCallback) {
                                if (endpointId) {
                                    $http.get(`api/application/${app}/endpoint/${endpointId}`)
                                        .then(function (data) {
                                            successCallback(data);
                                        }, tribeErrorHandlerService.ensureErrorHandler(errorCallback));
                                } else {
                                    var newEntry = {};
                                    successCallback(newEntry);
                                }
                            }
                        };
                    },
                    getEndpointHistory: function (url) {
                        return {
                            then: function (successCallback, errorCallback) {
                                if (url) {
                                    $http.get(url)
                                        .then(function (data) {
                                            successCallback(data);
                                        }, tribeErrorHandlerService.ensureErrorHandler(errorCallback));
                                } else {
                                    // TODO: What to do here?
                                }
                            }
                        };
                    },
                    getHistoricEndpoint: function(historyItem) {
                        return {
                            then: function(successCallback, errorCallback) {
                                $http.get(historyItem.link)
                                    .then(function (data) {
                                        successCallback(data);
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
                    },
                    saveEndpoint(applicationId, endpointId, endpoint) {
                        return {
                            then: function (successCallback, errorCallback) {
                                $http.put(`api/application/${applicationId}/endpoint/${endpointId ? endpointId : ''}`, endpoint)
                                    .then(
                                        function (data) {
                                            if (data && data.data && data.data.operation) {
                                                // we will have at most one result. only one application queried.
                                                successCallback(data);
                                            }
                                        },
                                        tribeErrorHandlerService.ensureErrorHandler(errorCallback)
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
