///<reference path="../../bower_components/DefinitelyTyped/angularjs/angular.d.ts"/>

angular.module('tribe-endpoints', [
    'website-services'
])

    .directive('appSee', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_see.html',
            scope: {
                aggregatedId: '='
            },
            controller: [
                '$timeout', '$scope', 'tribeEndpointsService',
                function ($timeout, $scope, srv) {
                    $scope.$watch('aggregatedId', function () {
                        if (!$scope.aggregatedId) {
                            return;
                        }
                        srv.getSeeContent($scope.aggregatedId).then(function (data) {
                            $timeout(function () {
                                $scope.$apply(function () {
                                    $scope.see = data;
                                });
                            });
                        });
                    });
                }
            ]
        };
    }])

    .directive('appApplicationDetails', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_application_details.html',
            scope: {
                app: '=application'
            },
            controller: [
                '$timeout', '$scope', 'tribeEndpointsService', 'tribeFilterService', 'tribeLinkHeaderService',
                function ($timeout, $scope, srv, tribeFilterService, tribeLinkHeaderService) {
                    var getDetails = function (applicationId) {
                        srv.getApplicationDetails(applicationId).then(function (data) {
                            $timeout(function () {
                                $scope.$apply(function () {
                                    $scope.details = data.data;
                                });
                            });
                        });
                    };
                    srv.getApplicationDetailsFromName($scope.app).then(function (response) {
                        $timeout(function () {
                            $scope.$apply(function () {
                                let data = response.data;
                                $scope.swagger = data.swagger;
                                let endpoints = []
                                if (data.swagger.paths) {
                                    for (let pathName in data.swagger.paths) {
                                        let ops = data.swagger.paths[pathName];
                                        for (let opname in ops) {
                                            if (opname.match('^x-.*')) {
                                                continue;
                                            }
                                            let links = tribeLinkHeaderService.parseLinkHeader(response.headers('link'));
                                            let link = links[opname.toUpperCase() + ' ' + pathName];
                                            let endpointId = link.substring(link.lastIndexOf('/') + 1);
                                            let operationObject = {
                                                path: pathName,
                                                operation: opname,
                                                summary: ops[opname].summary,
                                                description: ops[opname].description,
                                                id: endpointId,
                                                humanReadablePath: ops[opname]['x-tribestream-api-registry']['human-readable-path']
                                            };
                                            endpoints.push(operationObject);
                                        }
                                    }
                                }
                                $scope.endpoints = endpoints;
                                $scope.categories = data.categories;
                                $scope.tags = data.tags;
                                $scope.roles = data.roles;
                                $scope.applicationName = data.humanReadableName;
                            });
                        });
                    });
                    $scope.filterByCategory = function (category) {
                        tribeFilterService.filterByCategory($scope.details.name, category);
                    };
                    $scope.filterByRole = function (role) {
                        tribeFilterService.filterByRole($scope.details.name, role);
                    };
                    $scope.filterByTag = function (tag) {
                        tribeFilterService.filterByTag($scope.details.name, tag);
                    };
                }
            ]
        };
    }])

    .directive('appEndpoints', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints.html',
            scope: {},
            controller: [
                '$timeout', '$scope', 'tribeEndpointsService',
                function ($timeout, $scope, srv) {
                    srv.list().then(function (data) {
                        $timeout(function () {
                            $scope.$apply(function () {
                                $scope.total = data.total;
                                $scope.endpoints = data.endpoints;
                                $scope.applications = data.applications;
                                $scope.categories = data.categories;
                                $scope.tags = data.tags;
                                $scope.roles = data.roles;
                            });
                        });
                    });
                }
            ]
        };
    }])

    .directive('appEndpointsHeader', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_header.html',
            scope: {
                total: '=',
                endpoints: '='
            }
        };
    }])

    .directive('appEndpointsHeaderCreateBtn', ['$document', function ($document) {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_header_create_btn.html',
            scope: {
                endpoints: '='
            },
            controller: ['$scope', '$timeout', function ($scope, $timeout) {
                $scope.$watch('endpoints', function () {
                    $timeout(function () {
                        $scope.$apply(function () {
                            var applicationsMap = _.groupBy($scope.endpoints, function (endpoint) {
                                return endpoint.applicationId;
                            });
                            var applications = [];
                            _.each(applicationsMap, function (endpoints, applicationId) {
                                applications.push({
                                    applicationId: applicationId,
                                    applicationName: endpoints[0]['applicationName'],
                                    name: endpoints[0].application,
                                    endpoints: endpoints
                                });
                            });
                            $scope.applications = applications;
                        });
                    });
                });
            }],
            link: function (scope, el, attrs, controller) {
                var valueDiv = el.find('.button-applications');
                valueDiv.detach();
                var body = $document.find('body');
                var clear = function () {
                    el.removeClass('visible');
                    valueDiv.detach();
                };
                var elWin = $document;
                el.find('div.trigger').on('click', function () {
                    if (el.hasClass('visible')) {
                        valueDiv.detach();
                        el.removeClass('visible');
                        valueDiv.off('scroll', clear);
                    } else {
                        var pos = el.find('> div').offset();
                        valueDiv.css({
                            top: `${pos.top + el.find('> div').outerHeight()}px`,
                            left: `${pos.left}px`
                        });
                        body.append(valueDiv);
                        el.addClass('visible');
                        elWin.on('scroll', clear);
                    }
                });
                scope.$on('$destroy', function () {
                    valueDiv.remove();
                    elWin.off('scroll', clear);
                });
            }
        };
    }])

    .directive('appEndpointsListApplication', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_list_application.html',
            scope: {
                'application': '='
            },
            controller: ['$scope', '$timeout', function ($scope, $timeout) {
                $scope.pageSize = 5;
                $scope.$watch('application', function () {
                    if (!$scope.application) {
                        return;
                    }
                    $timeout(function () {
                        $scope.$apply(function () {
                            if ($scope.application.endpoints) {
                                $scope.endpoints = $scope.application.endpoints.slice(0, $scope.pageSize);
                                if ($scope.endpoints.length < $scope.application.endpoints.length) {
                                    $scope.showAll = true;
                                }
                            } else {
                                $scope.endpoints = [];
                            }
                        });
                    });
                });
                $scope.showAllEndpoints = function () {
                    $scope.endpoints = $scope.application.endpoints;
                    $scope.showAll = false;
                };
            }]
        };
    }])

    .directive('appEndpointsList', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_list.html',
            scope: {
                'total': '=',
                'endpoints': '='
            },
            controller: ['$timeout', '$scope',
                function ($timeout, $scope) {
                    $scope.$watch('endpoints', function () {
                        $timeout(function () {
                            $scope.$apply(function () {
                                var applicationsMap = _.groupBy($scope.endpoints, function (endpoint) {
                                    return endpoint.applicationId;
                                });
                                var applications = [];
                                _.each(applicationsMap, function (endpoints, applicationId) {
                                    applications.push({
                                        applicationId: applicationId,
                                        applicationName: endpoints[0]['applicationName'],
                                        name: endpoints[0].application,
                                        version: endpoints[0].applicationVersion,
                                        endpoints: endpoints
                                    });
                                });
                                $scope.applications = applications;
                            });
                        });
                    });
                }
            ]
        };
    }])

    .directive('appEndpointsListFilterEntries', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_list_filter_entries.html',
            scope: {
                'key': '@',
                'list': '=',
                'title': '@'
            },
            controller: ['$scope', '$location', '$route',
                function ($scope, $location, $route) {
                    $scope.removeFilter = function (entry) {
                        var rawQuery = $location.search();
                        var query = rawQuery[$scope.key];
                        if (query) {
                            query = query.split(',');
                            query = _.filter(query, function (qEntry) {
                                return qEntry !== entry;
                            });
                            if (query.length) {
                                rawQuery[$scope.key] = query.join(',');
                            } else {
                                delete rawQuery[$scope.key];
                            }
                            $location.search(rawQuery);
                            $route.reload();
                        }
                    };
                }
            ]
        };
    }])

    .directive('appEndpointsFilterBubble', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_filter_bubble.html',
            scope: {
                'name': '=',
                'length': '=',
                'qfield': '='
            },
            controller: ['$location', '$route', '$scope', function ($location, $route, $scope) {
                this.updateQuery = function () {
                    var values = $location.search();
                    var currentQuery = [];
                    if (values[$scope.qfield]) {
                        currentQuery = values[$scope.qfield].split(',');
                    }
                    currentQuery.push($scope.name);
                    values[$scope.qfield] = _.uniq(currentQuery).join(',');
                    $location.search(values);
                    $route.reload();
                };
            }],
            link: function (scope, el, attrs, controller) {
                el.on('click', function () {
                    controller.updateQuery();
                });
            }
        };
    }])

    .directive('appEndpointsFilter', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_filter.html',
            scope: {
                'title': '@',
                'list': '=',
                'qfield': '@'
            }
        };
    }])

    .directive('appEndpointsQueryInput', ['$timeout', function ($timeout) {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_query_input.html',
            scope: {},
            controller: [
                '$scope', '$location', '$route',
                function ($scope, $location, $route) {
                    var currentQuery = $location.search().q;
                    if (currentQuery) {
                        $scope.value = currentQuery;
                    } else {
                        $scope.value = '';
                    }
                    this.updateQuery = function () {
                        var values = $location.search();
                        values.q = $scope.value.trim();
                        if (values.q === '') {
                            delete values.q;
                        }
                        $location.search(values);
                        $route.reload();
                    };
                    $scope.updateQuery = this.updateQuery;
                }
            ],
            link: function (scope, el, attrs, controller) {
                var update = function (event) {
                    controller.updateQuery();
                    event.preventDefault();
                };
                el.find('input').bind("keydown keypress", function (event) {
                    if (event.which === 13) {
                        update(event);
                    }
                });
                $timeout(function () {
                    el.find('input').focus();
                });
            }
        };
    }])

    .directive('appEndpointsSelectedFilter', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_selected_filter.html',
            scope: {},
            controller: ['$timeout', '$location', '$scope', function ($timeout, $location, $scope) {
                var params = $location.search();
                $timeout(function () {
                    $scope.$apply(function () {
                        if (params.a) {
                            $scope.selectedApps = params.a.split(',');
                        }
                        if (params.c) {
                            $scope.selectedCategories = params.c.split(',');
                        }
                        if (params.t) {
                            $scope.selectedTags = params.t.split(',');
                        }
                        if (params.r) {
                            $scope.selectedRoles = params.r.split(',');
                        }
                    });
                });
            }]
        };
    }])

    .run(function () {
        // placeholder
    });
