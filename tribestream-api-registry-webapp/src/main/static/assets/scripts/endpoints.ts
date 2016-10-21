angular.module('tribe-endpoints', [
    'website-services'
])

    .directive('appSee', [function () {
        return {
            restrict: 'A',
            template: require('../templates/app_see.jade'),
            scope: {
                aggregatedId: '='
            },
            controller: [
                '$timeout', '$scope', 'tribeEndpointsService',
                function ($timeout, $scope, srv) {
                    $scope.$watch('aggregatedId', function () {
                        if (!$scope['aggregatedId']) {
                            return;
                        }
                        srv.getSeeContent($scope['aggregatedId']).then(function (data) {
                            $timeout(function () {
                                $scope.$apply(function () {
                                    $scope['see'] = data;
                                });
                            });
                        });
                    });
                }
            ]
        };
    }])

    .directive('appApplicationDetails', ['$timeout', '$window', function ($timeout, $window) {
        return {
            restrict: 'A',
            template: require('../templates/app_application_details.jade'),
            scope: {
                app: '=application'
            },
            controller: [
                '$timeout', '$scope', '$filter', '$location', 'tribeEndpointsService', 'tribeFilterService', 'tribeLinkHeaderService', 'systemMessagesService',
                function ($timeout, $scope, $filter, $location, srv, tribeFilterService, tribeLinkHeaderService, systemMessagesService) {
                    if (!!$scope.app) {
                      srv.getApplicationDetailsFromName($scope.app).then(function (response) {
                          $timeout(function () {
                              $scope.$apply(function () {
                                  let data = response.data;
                                  $scope.swagger = data.swagger;
                                  $scope.humanReadableName = data.humanReadableName;
                                  let endpoints = []
                                  let links = tribeLinkHeaderService.parseLinkHeader(response['data']['swagger']['x-tribestream-api-registry']['links']);
                                  $scope.applicationLink = links['self'];
                                  $scope.applicationsLink = null;
                                  $scope.historyLink = links['history'];
                                  $scope.endpointsLink = links['endpoints'];
                                  if (data.swagger.paths) {
                                      for (let pathName in data.swagger.paths) {
                                          let ops = data.swagger.paths[pathName];
                                          for (let opname in ops) {
                                              if (opname.match('^x-.*')) {
                                                  continue;
                                              }
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
                    } else {
                      // New application
                      $scope.applicationLink = null;
                      $scope.applicationsLink = 'api/application';
                      $scope.historyLink = null;
                      $scope.endpointsLink = null;
                    }
                    $scope.reloadHistory = () => {
                      if($scope.historyLink) {
                          srv.getHistory($scope.historyLink).then((response) => {
                              let links = tribeLinkHeaderService.parseLinkHeader(response['data']['links']);
                              for (let entry of response['data']['items']) {
                                  entry.link = links[`revision ${entry.revisionId}`];
                              }
                              $timeout(() => $scope.$apply(() => $scope.history = response['data']['items']));
                          });
                      }
                    }
                    $scope.save = () => {
                      if ($scope.applicationLink) {
                        srv.saveApplication($scope.applicationLink, $scope.swagger).then(
                          (saveResponse) => {
                            systemMessagesService.info("Saved application details!");
                            $scope.reloadHistory();
                          }
                        );
                      } else {
                        srv.createApplication($scope.applicationsLink, $scope.swagger).then(
                          function (saveResponse) {
                            systemMessagesService.info("Created application details!");
                            $timeout(() => {
                              $scope.$apply(() => {
                                $scope.swagger = saveResponse.data.swagger;
                                $scope.humanReadableName = saveResponse.data.humanReadableName;
                                let links = tribeLinkHeaderService.parseLinkHeader(saveResponse['data']['swagger']['x-tribestream-api-registry']['links']);
                                $scope.applicationLink = links['self'];
                                $scope.applicationsLink = null;
                                $scope.historyLink = links['history'];
                                $scope.endpointsLink = links['endpoints'];
                                $scope.reloadHistory();
                              });
                            });
                          }
                        );
                      }
                    };
                    $scope.delete = () => {
                      srv.delete($scope.applicationLink).then((response) => {
                          systemMessagesService.info("Deleted application!");
                          $location.path("/");
                      });
                    };
                    // Triggered by selecting one revision, will load it and show it
                    $scope.showHistoricApplication = (historyItem) => {
                      $timeout(() => {
                        $scope.$apply(() => {
                          $scope.history = null;
                        });
                      });
                      srv.getHistoricItem(historyItem).then((response) => {
                        $timeout(() => {
                          $scope.$apply(() => {
                            let detailsData = response.data;
                            $scope.historyItem = historyItem;
                            $scope.swagger = detailsData.swagger;
                            $scope.humanReadableName = detailsData.humanReadableName;
                          });
                        });
                      });
                    };
                }
            ],
            link: (scope, el) => $timeout(() => {
                el.find('div.history').on('click', () => {
                    var winEl = angular.element($window);
                    var calculateScroll = () => {
                        var target = el.find('div[data-app-application-details-history]');
                        return target.offset().top;
                    };
                    winEl.scrollTop(calculateScroll());
                });
            })
        };
    }])

    .directive('appApplicationDetailsHistory', [function() {
        return {
            restrict: 'A',
            template: require('../templates/app_application_details_history.jade'),
            scope: true,
            controller: [
                '$scope', 'tribeEndpointsService', 'tribeFilterService', '$timeout', '$filter', '$log', 'systemMessagesService', 'tribeLinkHeaderService',
                function ($scope, srv, tribeFilterService, $timeout, $filter, $log, systemMessagesService, tribeLinkHeaderService) {
                }
            ]
        };
    }])

    .directive('appEndpoints', [function () {
        return {
            restrict: 'A',
            template: require('../templates/app_endpoints.jade'),
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
            template: require('../templates/app_endpoints_header.jade'),
            scope: {
                total: '=',
                endpoints: '='
            }
        };
    }])

    .directive('appEndpointsHeaderCreateBtn', ['$document', '$timeout', function ($document, $timeout) {
        return {
            restrict: 'A',
            template: require('../templates/app_endpoints_header_create_btn.jade'),
            scope: {
                endpoints: '='
            },
            controller: ['$scope', '$timeout', function ($scope, $timeout) {
                $scope.$watch('endpoints', function () {
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.applications = ($scope.endpoints||[]).map((searchResult) => {
                                return {
                                    applicationId: searchResult.application.applicationId,
                                    applicationName: searchResult.application.applicationName,
                                    name: searchResult.application.application,
                                    version: searchResult.application.applicationVersion,
                                    endpoints: searchResult.endpoints
                                  };
                            });
                        });
                    });
                });
            }],
            link: (scope, el) => {
                let valueDiv = el.find('.button-applications');
                let clear = () => {
                    valueDiv.removeClass('visible');
                };
                let elWin = angular.element($document);
                el.on('click', () => {
                    if (valueDiv.hasClass('visible')) {
                        valueDiv.removeClass('visible');
                        elWin.off('click', clear);
                    } else {
                        valueDiv.addClass('visible');
                        $timeout(() => elWin.one('click', clear));
                    }
                });
                scope.$on('$destroy', () => elWin.off('click', clear));
            }
        };
    }])

    .directive('appEndpointsListApplication', [function () {
        return {
            restrict: 'A',
            template: require('../templates/app_endpoints_list_application.jade'),
            scope: {
                'application': '='
            },
            controller: ['$scope', '$timeout', function ($scope, $timeout) {
                $scope.pageSize = 5;
                $scope.$watch('application', function () {
                    if (!$scope['application']) {
                        return;
                    }
                    $timeout(function () {
                        $scope.$apply(function () {
                            if ($scope['application'].endpoints) {
                                $scope.endpoints = $scope['application'].endpoints.slice(0, $scope.pageSize);
                                if ($scope.endpoints.length < $scope['application'].endpoints.length) {
                                    $scope.showAll = true;
                                }
                            } else {
                                $scope.endpoints = [];
                            }
                        });
                    });
                });
                $scope.showAllEndpoints = function () {
                    $scope.endpoints = $scope['application'].endpoints;
                    $scope.showAll = false;
                };
            }]
        };
    }])

    .directive('appEndpointsList', [function () {
        return {
            restrict: 'A',
            template: require('../templates/app_endpoints_list.jade'),
            scope: {
                'total': '=',
                'endpoints': '='
            },
            controller: ['$timeout', '$scope',
                function ($timeout, $scope) {
                    $scope.$watch('endpoints', function () {
                        $timeout(function () {
                            $scope.$apply(function () {

                              $scope.applications = ($scope.endpoints||[]).map((searchResult) => {
                                  return {
                                      applicationId: searchResult.application.applicationId,
                                      applicationName: searchResult.application.applicationName,
                                      name: searchResult.application.application,
                                      version: searchResult.application.applicationVersion,
                                      endpoints: searchResult.endpoints
                                    };
                              });
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
            template: require('../templates/app_endpoints_list_filter_entries.jade'),
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
            template: require('../templates/app_endpoints_filter_bubble.jade'),
            scope: {
                'name': '=',
                'length': '=',
                'qfield': '='
            },
            controller: ['$location', '$route', '$scope', function ($location, $route, $scope) {
                this['updateQuery'] = function () {
                    var values = $location.search();
                    var currentQuery = [];
                    if (values[$scope.qfield]) {
                        currentQuery = values[$scope.qfield].split(',');
                    }
                    currentQuery.push($scope['name']);
                    values[$scope.qfield] = _.uniq(currentQuery).join(',');
                    $location.search(values);
                    $route.reload();
                };
            }],
            link: function (scope, el, attrs, controller) {
                el.on('click', function () {
                    controller['updateQuery']();
                });
            }
        };
    }])

    .directive('appEndpointsFilter', [function () {
        return {
            restrict: 'A',
            template: require('../templates/app_endpoints_filter.jade'),
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
            template: require('../templates/app_endpoints_query_input.jade'),
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
                    this['updateQuery'] = function () {
                        var values = $location.search();
                        values.q = $scope.value.trim();
                        if (values.q === '') {
                            delete values.q;
                        }
                        $location.search(values);
                        $route.reload();
                    };
                    $scope['updateQuery'] = this['updateQuery'];
                }
            ],
            link: function (scope, el, attrs, controller) {
                var update = function (event) {
                    controller['updateQuery']();
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
            template: require('../templates/app_endpoints_selected_filter.jade'),
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
