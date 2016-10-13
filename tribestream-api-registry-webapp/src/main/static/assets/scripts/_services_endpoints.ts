module services_endpoints {

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
                        _.each(parts, function (p: string) {
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
                                _.each(rawParams, function (value, key: string) {
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
                                var removeSelected = (list: { text:string }[], qparam): { text:string }[] => {
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
                    getApplicationDetails: function (applicationLink) {
                        return {
                            then: function (successCallback, errorCallback) {
                                $http.get(applicationLink)
                                    .then(function (data) {
                                        if (data && data.data && data.data.swagger) {
                                            // we will have at most one result. only one application queried.
                                            successCallback(data);
                                        }
                                    }, tribeErrorHandlerService.ensureErrorHandler(errorCallback));
                            }
                        };
                    },
                    getApplicationDetailsFromName: function (applicationName) {
                        return {
                            then: function (successCallback, errorCallback) {
                                $http.get('api/ui/application/' + applicationName)
                                    .then(function (data) {
                                        if (data && data.data && data.data.swagger) {
                                            // we will have at most one result. only one application queried.
                                            successCallback(data);
                                        }
                                    }, tribeErrorHandlerService.ensureErrorHandler(errorCallback));
                            }
                        };
                    },
                    getDetailsFromMetadata: function (request) {
                        return {
                            then: function (successCallback, errorCallback) {
                                if (request && request.endpointPath) {
                                    const params = request.version ? {version: request.version} : {};
                                    $http.get(`api/ui/endpoint/${request.applicationName}/${request.verb || '-'}/${request.endpointPath}`, {params : params})
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
                    getHistory: function (url) {
                        return {
                            then: (successCallback, errorCallback) => {
                                if (url) {
                                    $http.get(url)
                                        .then((data) => {
                                            successCallback(data);
                                        }, tribeErrorHandlerService.ensureErrorHandler(errorCallback));
                                } else {
                                    // TODO: What to do here?
                                }
                            }
                        };
                    },
                    getHistoricItem: function(historyItem) {
                        return {
                            promise() {
                              return $http.get(historyItem.link + '&payload=true');
                            },
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
                    saveApplication(applicationLink, application) {
                        return {
                            then: (successCallback, errorCallback) => {
                                $http.put(applicationLink, {swagger: application})
                                    .then(
                                    (data) => {
                                        if (data && data.data && data.data.swagger) {
                                            successCallback(data);
                                        }
                                    },
                                    tribeErrorHandlerService.ensureErrorHandler(errorCallback)
                                );
                            }
                        };
                    },
                    createApplication(applicationsLink, application) {
                        return {
                            then: function (successCallback, errorCallback) {
                                $http.post(applicationsLink, {swagger: application})
                                    .then(
                                    function (data) {
                                        if (data && data.data && data.data.swagger) {
                                            // we will have at most one result. only one application queried.
                                            successCallback(data);
                                        }
                                    },
                                    tribeErrorHandlerService.ensureErrorHandler(errorCallback)
                                );
                            }
                        };
                    },
                    saveEndpoint(endpointLink, endpoint) {
                        return {
                            then: function (successCallback, errorCallback) {
                                $http.put(endpointLink, endpoint)
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
                    },
                    createEndpoint(endpointsLink, endpoint) {
                        return {
                            then: function (successCallback, errorCallback) {
                                $http.post(endpointsLink, endpoint)
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
                    },
                    delete(endpointLink) {
                        return {
                            then: (successCallback, errorCallback) => {
                                $http.delete(endpointLink).then(
                                    (response) => {
                                        successCallback(response);
                                    },
                                    tribeErrorHandlerService.ensureErrorHandler(errorCallback)
                                )
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
