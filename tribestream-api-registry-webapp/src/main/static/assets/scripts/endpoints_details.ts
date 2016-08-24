///<reference path="../../bower_components/DefinitelyTyped/angularjs/angular.d.ts"/>

angular.module('tribe-endpoints-details', [
    'website-services'
])

    .directive('appEndpointsDetailsHeader', ['$window', '$timeout', function ($window, $timeout) {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_details_header.html',
            scope: {
                'app': '=application',
                'method': '=',
                'path': '=',
                'endpoint': '='
            },
            controller: ['$scope', '$timeout', function ($scope, $timeout) {
                $scope.$watch('endpoint.resourceUrl', function () {
                    if (!$scope.$eval('endpoint.resourceUrl')) {
                        return;
                    }
                    var aux = angular.element('<a href="' + $scope.endpoint.resourceUrl + '"></a>').get(0);
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.endpointProtocol = aux.protocol.replace(/:$/, '');
                            $scope.resourceUrl = `${$scope.endpoint.resourceUrl}`;
                        });
                    });
                });
            }]
        };
    }])

    .factory('tribeEndpointDetailsTocService', [
        function () {
            var data = {
                selectedAnchor: null,
                anchors: []
            };
            return {
                getData: function () {
                    return data;
                },
                setAnchor: function (title, isSubmenu, el) {
                    data.anchors.push({
                        title: title,
                        submenu: isSubmenu,
                        el: el
                    });
                },
                clearAnchors: function () {
                    data.anchors = [];
                }
            };
        }
    ])

    .directive('appEndpointsDetailsToc', ['tribeEndpointDetailsTocService', '$window', function (srv, $window) {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_details_toc.html',
            scope: {},
            controller: ['tribeEndpointDetailsTocService', '$scope', '$document', function (srv, $scope, $document) {
                $scope.anchors = srv.getData().anchors;
                $scope.getIndex = function (anchor) {
                    var tags = $document.find('article.app-ep-details-body *[app-endpoints-details-toc-anchor]')
                    return tags.index(anchor.el);
                };
                this.clearAnchors = function () {
                    srv.clearAnchors();
                };
            }],
            link: function (scope, el, attrs, controller) {
                el.find('div.collapse-icon').on('click', function () {
                    el.toggleClass('collapsed');
                });
                el.find('li[data-app-endpoints-details-toc-item]').on('click', function () {
                    el.removeClass('collapsed');
                });
                scope.$on('$destroy', function () {
                    controller.clearAnchors();
                });
            }
        };
    }])

    .directive('appEndpointsDetailsTocItem', ['$timeout', '$window', function ($timeout, $window) {
        return {
            restrict: 'A',
            scope: {
                anchor: '=appEndpointsDetailsTocItem'
            },
            templateUrl: 'app/templates/app_endpoints_details_toc_item.html',
            controller: ['$scope', 'tribeEndpointDetailsTocService', function ($scope, srv) {
                $scope.tocData = srv.getData();
                this.selectMe = function () {
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.tocData.selectedAnchor = $scope.anchor;
                        });
                    });
                };
            }],
            link: function (scope, el, attrs, controller) {
                el.on('click', function () {
                    controller.selectMe();
                    var winEl = angular.element('div[data-app-endpoints-details] > div');
                    var calculateScroll = function () {
                        var target = scope.anchor.el;
                        var elOffset = target.offset().top;
                        var elHeight = target.height();
                        var windowHeight = $(window).height();
                        if (elHeight < windowHeight) {
                            return elOffset - ((windowHeight / 2) - (elHeight / 2));
                        }
                        else {
                            return elOffset;
                        }
                    };
                    winEl.animate({
                        scrollTop: calculateScroll()
                    }, function () {
                        scope.anchor.el.focus();
                        if (scope.anchor.el.is(':focus')) {
                            return;
                        }
                        scope.anchor.el.find('*').each(function (kidindex, rawKid) {
                            var kid = angular.element(rawKid);
                            kid.focus();
                            if (kid.is(':focus')) {
                                return false;
                            }
                        });
                    })
                });
                scope.$watch('tocData.selectedAnchor', function () {
                    var selected = scope.$eval('tocData.selectedAnchor');
                    if (selected && selected === scope.anchor) {
                        el.find('h4').addClass('selected');
                    } else {
                        el.find('h4').removeClass('selected');
                    }
                });
            }
        };
    }])

    .directive('appEndpointsDetailsTocAnchor', ['$timeout', function ($timeout) {
        return {
            restrict: 'A',
            scope: {
                title: '@appEndpointsDetailsTocAnchor',
                submenu: '@'
            },
            controller: ['tribeEndpointDetailsTocService', '$scope', function (srv, $scope) {
                $scope.data = srv.getData();
                this.registerAnchor = function (el) {
                    srv.setAnchor($scope.title, $scope.submenu, el);
                };
                this.setSelectedAnchor = function (el) {
                    $timeout(function () {
                        $scope.$apply(function () {
                            var anchors = $scope.data.anchors;
                            $scope.data.selectedAnchor = anchors.find(function (item) {
                                return item.el === el;
                            });
                        });
                    });
                };
            }],
            link: function (scope, el, attrs, controller) {
                $timeout(function () {
                    controller.registerAnchor(el);
                    var callback = function () {
                        controller.setSelectedAnchor(el);
                    };
                    el.find('*').on('focus', callback);
                    el.find('*').on('click', callback);
                });
            }
        };
    }])

    .directive('appEndpointsDetailsParameters', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_details_parameters.html',
            scope: {
                'endpoint': '='
            },
            controller: ['$scope', '$timeout', function ($scope, $timeout) {
                $scope.$watch('endpoint.uri.path', function () {
                    var path = $scope.$eval('endpoint.uri.path');
                    if (!path) {
                        return;
                    }
                    var params = path.match(/:[a-zA-Z0-9_]+/g);
                    if (params) {
                        params = _.map(params, function (value) {
                            return value.substring(1);
                        });
                    }
                    if (!params) {
                        params = [];
                    }
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.pathParams = params;
                        });
                    });
                });
                $scope.$watch('endpoint.params', function () {
                    var params = $scope.$eval('endpoint.params');
                    if (!params) {
                        return;
                    }
                    $timeout(function () {
                        $scope.$apply(function () {
                            _.each(params, function (p) {
                                if (!p.sampleValues) {
                                    p.sampleValues = [];
                                }
                            });
                            $scope.params = params;
                        });
                    });
                });
                $scope.removeParam = function (p) {
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.endpoint.params = _.without($scope.endpoint.params, p);
                        });
                    });
                };
                $scope.addParam = function () {
                    var params = $scope.$eval('endpoint.params');
                    if (!params) {
                        params = [];
                    }
                    $timeout(function () {
                        $scope.$apply(function () {
                            params.unshift({
                                type: 'string',
                                style: 'query',
                                sampleValues: [],
                                required: false
                            });
                            $scope.params = params;
                        });
                    });
                };
            }]
        };
    }])

    .directive('appEndpointsDetailsResourceInformation', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_details_resource_information.html',
            scope: {
                'endpoint': '='
            },
            controller: ['$scope', '$timeout', function ($scope, $timeout) {
                $scope.requestFormatsOptions = [
                    'text/plain', 'application/json', 'application/xml'
                ];
                $scope.responseFormatsOptions = [
                    'text/plain', 'application/json', 'application/xml'
                ];
                $scope.statusOptions = ['PROPOSAL', 'STUB', 'DRAFT', 'TEST', 'VALIDATION', 'ACCEPTED', 'CONFIDENTIAL'];
                $scope.rateUnits = ['SECONDS', 'MINUTES', 'HOURS', 'DAYS'];
                $scope.$watch('endpoint', function () {
                    if (!$scope.endpoint) {
                        return;
                    }
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.requiresHttps = false;
                            $scope.requiresClientCertificate = false;
                            var detailsData = $scope.endpoint;
                            if (detailsData.webAppSecurity && detailsData.webAppSecurity.transportGuarantee) {
                                $scope.requiresHttps = detailsData.webAppSecurity.transportGuarantee === 'CONFIDENTIAL';
                                $scope.requiresClientCertificate = detailsData.webAppSecurity.transportGuarantee === 'INTEGRAL';
                            }
                            $scope.requiresAuthentication = false;
                            if (detailsData.roles) {
                                $scope.requiresAuthentication = detailsData.roles.length > 0;
                            }
                            $scope.rateLimited = undefined;
                            if (detailsData.throttlings) {
                                $scope.rateLimited = _.keys(detailsData.throttlings).length > 0;
                            }
                            $scope.wadlUrl = detailsData.resourceUrl + "?_wadlx&_methodx=" + detailsData.methodName;
                            if (detailsData.metadata && detailsData.metadata.status) {
                                $scope.status = [detailsData.metadata.status];
                            }
                            $scope.authMethod = [];
                            if (detailsData.webAppSecurity && detailsData.webAppSecurity.authMethod) {
                                $scope.authMethod.push(detailsData.webAppSecurity.authMethod);
                            }

                        });
                    });
                    $scope.addRate = function () {
                        $timeout(function () {
                            $scope.$apply(function () {
                                if (!$scope.endpoint.rates) {
                                    $scope.endpoint.rates = [];
                                }
                                $scope.endpoint.rates.push({});
                            });
                        });
                    };
                    $scope.removeRate = function (rate) {
                        $timeout(function () {
                            $scope.$apply(function () {
                                if (!$scope.endpoint.throttlings) {
                                    return;
                                }
                                if (!$scope.endpoint.throttlings.user) {
                                    return;
                                }
                                $scope.endpoint.throttlings.user = _.without($scope.endpoint.throttlings.user, rate);
                            });
                        });
                    };
                });
            }]
        };
    }])

    .directive('appEndpointsDetailsResponseRequest', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_details_response_request.html',
            scope: {
                'endpoint': '='
            },
            controller: ['$scope', '$timeout', function ($scope, $timeout) {
                $scope.$watch('endpoint', function () {
                    if (!$scope.endpoint) {
                        return;
                    }
                    $timeout(function () {
                        $scope.$apply(function () {
                            if (!$scope.endpoint.errors) {
                                $scope.endpoint.errors = [];
                            }
                            if (!$scope.endpoint.expectedValues) {
                                $scope.endpoint.expectedValues = [];
                            }
                        });
                    });
                });
                $scope.removeErrorCode = function (code) {
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.endpoint.errors = _.without($scope.endpoint.errors, code);
                        });
                    });
                };
                $scope.addErrorCode = function () {
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.endpoint.errors.push({
                                statusCode: 0,
                                errorCode: 0,
                                message: '',
                                description: ''
                            });
                        });
                    });
                };
                $scope.removeExpectedValue = function (value) {
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.endpoint.expectedValues = _.without($scope.endpoint.expectedValues, value);
                        });
                    });
                };
                $scope.addExpectedValue = function () {
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.endpoint.expectedValues.push({
                                name: '',
                                values: ''
                            });
                        });
                    });
                };
            }]
        };
    }])

    .directive('appEndpointsDetailsSee', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_details_see.html',
            scope: {
                'endpoint': '='
            },
            controller: ['$scope', '$timeout', function ($scope, $timeout) {
                $scope.addLink = function () {
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.endpoint.metadata = $scope.endpoint.metadata || {};
                            $scope.endpoint.metadata.sees = $scope.endpoint.metadata.sees || [];
                            $scope.endpoint.metadata.sees.push({});
                        });
                    });
                };
                $scope.removeLink = function (link) {
                    $timeout(function () {
                        $scope.$apply(function () {
                            if ($scope.endpoint.metadata && $scope.endpoint.metadata.sees) {
                                $scope.endpoint.metadata.sees = _.without($scope.endpoint.metadata.sees, link);
                            }
                        });
                    });
                };
            }]
        };
    }])

    .directive('appEndpointsDetails', [function () {
        return {
            restrict: 'A',
            templateUrl: 'app/templates/app_endpoints_details.html',
            scope: {
                'app': '=application',
                'method': '=',
                'path': '='
            },
            controller: [
                '$scope', 'tribeEndpointsService', 'tribeFilterService', '$timeout',
                function ($scope, srv, tribeFilterService, $timeout) {
                    srv.getDetails($scope.app, $scope.method, $scope.path).then(function (detailsData) {
                        $timeout(function () {
                            $scope.$apply(function () {
                                $scope.endpoint = detailsData;
                            });
                        });
                    });
                }
            ]
        };
    }])

    .run(function () {
        // placeholder
    });