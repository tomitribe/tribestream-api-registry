import {TryMeService} from './tryme.service';

module endpointdetails {
let HistoryCommonController = require("./endpoints_common.ts").controllerEndpoint;

angular.module('tribe-endpoints-details', [
    'website-services',
    'website-services-endpoints',
    'ngDialog',
    'ngAnimate',
    'vAccordion',
    'ui.codemirror',
    'ui.select'
])

    .factory('appEndpointsDetailsHeaderService', [($location) => {
        return {
            getBaseUrl: (swagger) => {
                let basePath = swagger.basePath === '/' ? '' : swagger.basePath;
                return swagger.host + basePath;
            }
        };
    }])

    .service('TryMeService', ['$http', $http => new TryMeService($http)])

    .directive('appEndpointsDetailsHeader', ['$window', '$timeout', '$filter', function ($window, $timeout, $filter) {
        return {
            restrict: 'A',
            template: require('../templates/app_endpoints_details_header.jade'),
            scope: true,
            controller: ['$scope', '$timeout', 'appEndpointsDetailsHeaderService', function ($scope, $timeout, srv) {
                $scope.regex = '^(\\/|(\\/{_*\\-*[a-zA-Z0-9_-]{1,}}|\\/_*\\-*[a-zA-Z0-9_-]{1,})*)$';
                $scope.regexTip = `
                    <div class="endpoint-details-path-tip">
                        <p>To be considered valid, the path should follow these rules:</p>
                        <ul>
                            <li>Starts with a single "&#47;" </li>
                            <li>Contains only alphanumeric, "&#47;", "{", "}", "-" and "_" characters </li>
                            <li>Does not end with "&#47;" </li>
                            <li>Does not contain multiple "&#47;" characters in a row. </li>
                        </ul>
                    </div>
                `;
                $scope.toUppercase = (item) => {
                    if (!item) {
                        return null;
                    }
                    if (item.text) {
                        return item.text.toUpperCase();
                    }
                    return item.toUpperCase();
                };
                $scope.$watch('application', function () {
                    // Compute endpoint URL
                    if ($scope.application && $scope.application.swagger && $scope.application.swagger.host && $scope.application.swagger.basePath) {
                        $scope.$watch('endpoint.path', () => {
                            if($scope['endpoint'] && $scope['endpoint'].path) {
                                $scope.resourceUrl = srv.getBaseUrl($scope.application.swagger, $scope['endpoint'].path) + $scope['endpoint'].path;
                            }
                        });
                        $timeout(function () {
                            $scope.$apply(function () {
                                if (!!$scope.endpoint.operation.schemes && $scope.endpoint.operation.schemes[0]) {
                                    $scope.endpoint.endpointProtocol = $scope.endpoint.operation.schemes[0];
                                } else if ($scope.application && $scope.application.swagger && $scope.application.swagger.schemes) {
                                    $scope.endpoint.endpointProtocol = $scope.application.swagger.schemes[0];
                                } else {
                                  $scope.endpoint.endpointProtocol = 'http';
                                }
                                $scope.resourceUrl = srv.getBaseUrl($scope.application.swagger, $scope['endpoint'].path) + $scope['endpoint'].path;
                            });
                        });
                    }
                });
            }],
            link: (scope, el) => {
                scope.$on('$destroy', () => el.remove());
            }
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
            template: require('../templates/app_endpoints_details_toc.jade'),
            scope: {},
            controller: ['tribeEndpointDetailsTocService', '$scope', '$document', function (srv, $scope, $document) {
                $scope.anchors = srv.getData().anchors;
                $scope.getIndex = function (anchor) {
                    var tags = $document.find('article.app-ep-details-body *[app-endpoints-details-toc-anchor]')
                    return tags.index(anchor.el);
                };
                this['clearAnchors'] = function () {
                    srv['clearAnchors']();
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
                    controller['clearAnchors']();
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
            template: require('../templates/app_endpoints_details_toc_item.jade'),
            controller: ['$scope', 'tribeEndpointDetailsTocService', function ($scope, srv) {
                $scope.tocData = srv.getData();
                this['selectMe'] = function () {
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.tocData.selectedAnchor = $scope.anchor;
                        });
                    });
                };
            }],
            link: function (scope, el, attrs, controller) {
                el.on('click', function () {
                    controller['selectMe']();
                    var winEl = angular.element('div[data-app-endpoints-details] > div');
                    var calculateScroll = function () {
                        var target = scope['anchor'].el;
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
                        scope['anchor'].el.focus();
                        if (scope['anchor'].el.is(':focus')) {
                            return;
                        }
                        scope['anchor'].el.find('*').each(function (kidindex, rawKid) {
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
                    if (selected && selected === scope['anchor']) {
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
                this['registerAnchor'] = function (el) {
                    srv.setAnchor($scope.title, $scope.submenu, el);
                };
                this['setSelectedAnchor'] = function (el) {
                    $timeout(function () {
                        $scope.$apply(function () {
                            var anchors = $scope.data.anchors;
                            $scope['data'].selectedAnchor = anchors.find(function (item) {
                                return item.el === el;
                            });
                        });
                    });
                };
            }],
            link: function (scope, el, attrs, controller) {
                $timeout(function () {
                    controller['registerAnchor'](el);
                    var callback = function () {
                        controller['setSelectedAnchor'](el);
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
            template: require('../templates/app_endpoints_details_parameters.jade'),
            scope: true,
            controller: ['$scope', '$timeout', function ($scope, $timeout) {
                $scope.$watch('endpoint.uri.path', function () {
                    var path = $scope.$eval('endpoint.uri.path');
                    if (!path) {
                        return;
                    }
                    var params = path.match(/:[a-zA-Z0-9_]+/g);
                    if (params) {
                        params = _.map(params, function (value:string) {
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
                $scope.removeParam = (p) => $timeout(() => $scope.$apply(() => {
                    $scope['endpoint'].operation.parameters = _.without($scope['endpoint'].operation.parameters, p);
                }));
                $scope.addParam = function () {
                    var params = $scope.$eval('endpoint.operation.parameters');
                    if (!params) {
                        if (!$scope['endpoint'].operation) {
                            $scope['endpoint'].operation = {};
                        }
                        $scope['endpoint'].operation.parameters = [];
                        params = $scope['endpoint'].operation.parameters;
                    }
                    $timeout(function () {
                        $scope.$apply(function () {
                            params.unshift({
                                type: 'string',
                                style: 'query',
                                sampleValues: '',
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
            template: require('../templates/app_endpoints_details_resource_information.jade'),
            scope: true,
            controller: ['$scope', '$timeout', function ($scope, $timeout) {
                $scope.requestFormatsOptions = [
                    'text/plain', 'application/json', 'application/xml'
                ];
                $scope.responseFormatsOptions = [
                    'text/plain', 'application/json', 'application/xml'
                ];
                $scope.statusOptions = ['In Design', 'In Development', 'Released', 'Deprecated', 'Deferred'];
                $scope.rateUnits = ['SECONDS', 'MINUTES', 'HOURS', 'DAYS'];
                $scope.$watch('endpoint', function () {
                    if (!$scope['endpoint'] || !$scope['endpoint'].operation) {
                        return;
                    }
                    $scope.addRate = function () {
                        $timeout(function () {
                            $scope.$apply(function () {
                                if (!$scope['endpoint'].operation) {
                                    return;
                                }
                                if (!$scope['endpoint'].operation['x-tribestream-api-registry']) {
                                    $scope['endpoint'].operation['x-tribestream-api-registry'] = {};
                                }
                                if (!$scope['endpoint'].operation['x-tribestream-api-registry']['rates']) {
                                    $scope['endpoint'].operation['x-tribestream-api-registry']['rates'] = [];
                                }
                                $scope['endpoint'].operation['x-tribestream-api-registry']['rates'].push({});
                            });
                        });
                    };
                    $scope.removeRate = function (rate) {
                        $timeout(function () {
                            $scope.$apply(function () {
                                if (!$scope['endpoint'].operation) {
                                    return;
                                }
                                if (!$scope['endpoint'].operation['x-tribestream-api-registry']) {
                                    return;
                                }
                                if (!$scope['endpoint'].operation['x-tribestream-api-registry']['rates']) {
                                    return;
                                }
                                $scope['endpoint'].operation['x-tribestream-api-registry']['rates'] = _.without($scope['endpoint'].operation['x-tribestream-api-registry']['rates'], rate);
                            });
                        });
                    };
                });
                // set as empty list in case list is undefined
                let initList = (path, name) => {
                    $scope.$watch(path, () => {
                        let xapi = $scope.$eval(path);
                        if (!xapi) {
                            return;
                        }
                        if (!xapi[name]) {
                            xapi[name] = [];
                        }
                    });
                };
                initList("endpoint.operation['x-tribestream-api-registry']", 'roles');
                initList("endpoint.operation['x-tribestream-api-registry']", 'categories');
                initList("endpoint.operation", 'tags');
            }],
            link: (scope, el) => {
                scope.$on('$destroy', () => el.remove());
            }
         };
    }])

    .directive('appEndpointsDetailsResponseRequest', [function () {
        return {
            restrict: 'A',
            template: require('../templates/app_endpoints_details_response_request.jade'),
            scope: true,
            controller: ['$scope', '$timeout', function ($scope, $timeout) {
                $scope.$watch('endpoint.operation', function () {
                    $timeout(function () {
                        $scope.$apply(function () {
                            // TODO: This MUST go somewhere else, both properties
                            if (!$scope['endpoint'].operation) {
                                $scope['endpoint'].operation = {};
                            }
                            if (!$scope['endpoint'].operation['x-tribestream-api-registry']) {
                                $scope['endpoint'].operation['x-tribestream-api-registry'] = {};
                            }
                            if (!$scope['endpoint'].operation['x-tribestream-api-registry']['response-codes']) {
                                $scope['endpoint'].operation['x-tribestream-api-registry']['response-codes'] = [];
                            }
                            if (!$scope['endpoint'].operation['x-tribestream-api-registry']['expected-values']) {
                                $scope['endpoint'].operation['x-tribestream-api-registry']['expected-values'] = [];
                            }
                        });
                    });
                });
                $scope.removeErrorCode = function (code) {
                    $timeout(function () {
                        $scope.$apply(function () {
                            if (!$scope['endpoint'].operation) {
                                return;
                            }
                            if (!$scope['endpoint'].operation['x-tribestream-api-registry']) {
                                return;
                            }
                            if (!$scope['endpoint'].operation['x-tribestream-api-registry']['response-codes']) {
                                return;
                            }
                            $scope['endpoint'].operation['x-tribestream-api-registry']['response-codes'] = _.without(
                                $scope['endpoint'].operation['x-tribestream-api-registry']['response-codes'],
                                code
                            );
                        });
                    });
                };
                $scope.addErrorCode = function () {
                    $timeout(function () {
                        $scope.$apply(function () {
                            if (!$scope['endpoint'].operation) {
                                $scope['endpoint'].operation = {};
                            }
                            if (!$scope['endpoint'].operation['x-tribestream-api-registry']) {
                                $scope['endpoint'].operation['x-tribestream-api-registry'] = {};
                            }
                            if (!$scope['endpoint'].operation['x-tribestream-api-registry']['response-codes']) {
                                $scope['endpoint'].operation['x-tribestream-api-registry']['response-codes'] = [];
                            }
                            $scope['endpoint'].operation['x-tribestream-api-registry']['response-codes'].push({
                                http_status: 0,
                                error_code: 0,
                                message: '',
                                description: ''
                            });
                        });
                    });
                };
                $scope.removeExpectedValue = function (value) {
                    $timeout(function () {
                        $scope.$apply(function () {
                            if (!$scope['endpoint'].operation) {
                                return;
                            }
                            if (!$scope['endpoint'].operation['x-tribestream-api-registry']) {
                                return;
                            }
                            if (!$scope['endpoint'].operation['x-tribestream-api-registry']['expected-values']) {
                                return;
                            }
                            $scope['endpoint'].operation['x-tribestream-api-registry']['expected-values'] = _.without(
                                $scope['endpoint'].operation['x-tribestream-api-registry']['expected-values'],
                                value
                            );
                        });
                    });
                };
                $scope.addExpectedValue = function () {
                    $timeout(function () {
                        $scope.$apply(function () {
                            if (!$scope['endpoint'].operation) {
                                $scope['endpoint'].operation = {};
                            }
                            if (!$scope['endpoint'].operation['x-tribestream-api-registry']) {
                                $scope['endpoint'].operation['x-tribestream-api-registry'] = {};
                            }
                            if (!$scope['endpoint'].operation['x-tribestream-api-registry']['expected-values']) {
                                $scope['endpoint'].operation['x-tribestream-api-registry']['expected-values'] = [];
                            }
                            $scope['endpoint'].operation['x-tribestream-api-registry']['expected-values'].push({
                                name: '',
                                values: ''
                            });
                        });
                    });
                };
            }],
            link: (scope, el) => {
                scope.$on('$destroy', () => el.remove());
            }
        };
    }])

    .directive('appEndpointsDetailsSee', ['$timeout', function ($timeout) {
        return {
            restrict: 'A',
            template: require('../templates/app_endpoints_details_see.jade'),
            scope: {
                'endpoint': '=',
                'onEditModeOn': '&',
                'onEditModeOff': '&'
            },
            controller: ['$scope', function ($scope) {
                this['addLink'] = function () {
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope['endpoint'].operation['x-tribestream-api-registry'] = $scope['endpoint'].operation['x-tribestream-api-registry'] || {};
                            $scope['endpoint'].operation['x-tribestream-api-registry'].sees = $scope['endpoint'].operation['x-tribestream-api-registry'].sees || [];
                            $scope['endpoint'].operation['x-tribestream-api-registry'].sees.push({});
                        });
                    });
                };
                $scope.removeLink = function (link) {
                    $timeout(function () {
                        $scope.$apply(function () {
                            if (!$scope['endpoint'].operation
                                || !$scope['endpoint'].operation['x-tribestream-api-registry']
                                || !$scope['endpoint'].operation['x-tribestream-api-registry'].sees) {
                                return;
                            }
                            $scope['endpoint'].operation['x-tribestream-api-registry'].sees = _.without($scope['endpoint'].operation['x-tribestream-api-registry'].sees, link);
                        });
                    });
                };
            }],
            link: function (scope, el, attrs, controller) {
                el.find('div.add-link').on('click', function () {
                    controller['addLink']();
                    $timeout(function () {
                        var newItem = el.find('i[data-tribe-editable-text] > div').last();
                        newItem.focus();
                    }, 500); // TODO: please find a better way to do this after the meeting.
                });

            }
        };
    }])

    .directive('appEndpointsDetailsHistory', [function() {
        return {
            restrict: 'A',
            template: require('../templates/app_endpoints_details_history.jade'),
            scope: true,
            controller: [
                '$scope', 'tribeEndpointsService', 'tribeFilterService', '$timeout', '$filter', '$log', 'systemMessagesService', 'tribeLinkHeaderService', '$q',
                HistoryCommonController
            ]
        };
    }])

    .directive('appEndpointsDetails', ['$window', '$timeout', function ($window, $timeout) {
  return {
    restrict: 'A',
      template: require('../templates/app_endpoints_details.jade'),
    scope: {
      'requestMetadata': '='
    },
    controller: [
      '$scope', 'tribeEndpointsService', 'tribeFilterService', '$timeout', '$filter', '$log', '$location', 'systemMessagesService', 'tribeLinkHeaderService', 'ngDialog', 'TryMeService',
      function ($scope, srv, tribeFilterService, $timeout, $filter, $log, $location, systemMessagesService, tribeLinkHeaderService, ngDialog, tryMeService) {
        $scope.isSaveable = () => {
          return !$scope['isOnEdit'] && $scope['endpoint'] && !!$scope['endpoint']['httpMethod'] && !!$scope['endpoint']['path'] && !!$scope['endpoint']['path'].trim();
        };

        const sampleValue = type => {
          switch(type) {
            case 'boolean':
              return 'true';
            case 'integer':
            case 'long':
            case 'int32':
            case 'int64':
            case 'number':
              return '10';
            case 'float':
            case 'double':
              return '10.0';
            default:
              return 'value';
          }
        };

        $scope.tryMeModal = () => {
          let newScope = $scope.$new();
          const swagger = $scope.application.swagger;
          const url = ($scope.endpoint.endpointProtocol || 'http') + '://' + swagger.host + (swagger.basePath === '/' ? '' : swagger.basePath) + $scope.endpoint.path;
          const parameters = (($scope.endpoint.operation || {}).parameters || {});
          const querySample = parameters.filter(p => p['in'] === 'query' && !!p['name'])
            .reduce((acc, param) => acc + (!!acc ? '&' : '?') + param['name'] + '=' + sampleValue(param['type'] || 'string'), '');

          // TODO: using $scope.application.swagger definitions, for now just hardcode something
          const payload = $scope.endpoint.httpMethod === 'post' || $scope.endpoint.httpMethod === 'put' ? '{}' : undefined;

          newScope.endpoint = $scope.endpoint;
          newScope.payloadOptions = {lineNumbers: true, mode: 'javascript'};
          newScope.request = {
            ignoreSsl: url.indexOf('https') == 0,
            method: $scope.endpoint.httpMethod.toUpperCase(),
            url: url + querySample,
            payload: payload,
            digest: {
              header: 'Digest'
            }
          };

          // we pre-fill all headers of the operation + accept/content-type if consumes/produces are there
          newScope.headers = parameters.filter(p => p['in'] === 'header' && !!p['name'])
            .filter(p => 'content-type' !== p['name'] && 'accept' !== p['name'])
            .map(p => {
              return { name: p['name'], value: sampleValue(p['type'] || 'string') };
            });
          if ($scope.endpoint.operation.consumes && $scope.endpoint.operation.consumes.length) {
            newScope.headers.push({ name: 'Content-Type', value: $scope.endpoint.operation.consumes[0] });
          }
          if ($scope.endpoint.operation.produces && $scope.endpoint.operation.produces.length) {
            newScope.headers.push({ name: 'Accept', value: $scope.endpoint.operation.produces[0] });
          }
          newScope.headerOptions = [ 'Content-Type', 'Accept' ];
          newScope.headers.forEach(h => newScope.headerOptions.push(h.name));

          newScope.onHeaderChange = (name, header) => {
            if (!name) {
              if (!!header.$$proposals) {
                header.$$proposals = [];
              }
              return;
            }
            if ((name == 'Content-Type' || name == 'Accept') && !header.$$proposals) {
              header.$$proposals = ['application/json', 'application/xml', 'application/x-www-form-urlencoded', 'text/plain'];
            } else if (!!this.$$proposals) {
              header.$$proposals = [];
            }
          };
          newScope.addHeader = () => {
            newScope.headers.push({});
          };

          newScope.addDigest = () => {
            let digestScope = $scope.$new();
            digestScope.digest = {
              header: 'Digest',
              algorithm: 'sha-256'
            };
            digestScope.digestAlgorithmOptions = ['md2', 'md4', 'md5', 'sha-1', 'sha-224', 'sha-256', 'sha-384', 'sha-512']
              .map(name => {
                return {text: name, value:name};
              });
            ngDialog.open({ template: require('../templates/try_me_digest.jade'), plain: true, scope: digestScope }).closePromise.then(digest => {
              if ('$closeButton' === digest.value) {
                return;
              }
              newScope.request.digest = digest.value;
            });
          };

          newScope.addOAuth2 = () => {
            let oauth2Scope = $scope.$new();
            oauth2Scope.oauth2 = {
              header: 'Authorization',
              grantType: 'password'
            };
            ngDialog.open({ template: require('../templates/try_me_oauth2.jade'), plain: true, scope: oauth2Scope }).closePromise.then(oauth2 => {
              if ('$closeButton' === oauth2.value) {
                return;
              }
              tryMeService.getOAuth2Header(oauth2.value, newScope.request.ignoreSsl)
                .success(result => newScope.headers.push(result))
                .error(err => systemMessagesService.error('Can\'t get the OAuth2 token with these informations'));
            });
          };

          newScope.addSignature = () => {
            let signatureScope = $scope.$new();
            signatureScope.signatureAlgorithmOptions = [
              'hmac-sha1', 'hmac-sha224', 'hmac-sha256', 'hmac-sha384', 'hmac-sha512',
              'rsa-sha1', 'rsa-sha256', 'rsa-sha384', 'rsa-sha512',
              'dsa-sha1', 'dsa-sha224', 'dsa-sha256'
            ].map(name => { return {text:name, value:name}; });
            signatureScope.signature = {
              header: 'Authorization',
              headers: ['(request-target)'],
              algorithm: 'hmac-sha256',
              method: newScope.request.method,
              url: newScope.request.url,
              requestHeaders: newScope.headers.reduce((accumulator, e) => {
                accumulator[e.name] = e.value;
                return accumulator;
              }, {})
            };
            signatureScope.headerOptions = newScope.headers.map(h => h.name);
            signatureScope.headerOptions.push('(request-target)');
            ngDialog.open({ template: require('../templates/try_me_signature.jade'), plain: true, scope: signatureScope }).closePromise.then(signature => {
              if ('$closeButton' === signature.value) {
                return;
              }
              // remove if any header is null
              signature.value.headers = signature.value.headers.filter(h => !!h);
              tryMeService.getSignatureHeader(signature.value)
                .success(result => newScope.headers.push(result))
                .error(err => systemMessagesService.error('Can\'t get the Signature header with these informations'));
            });
          };

          newScope.addBasic = () => {
            let basicScope = $scope.$new();
            basicScope.basic = {
              header: 'Authorization'
            };
            ngDialog.open({ template: require('../templates/try_me_basic.jade'), plain: true, scope: basicScope }).closePromise.then(basic => {
              if ('$closeButton' === basic.value) {
                return;
              }
              tryMeService.getBasicHeader(basic.value)
                .success(result => newScope.headers.push(result))
                .error(err => systemMessagesService.error('Can\'t get the Basic header with these informations'));
            });
          };

          newScope.addDate = () => {
            newScope.headers.push({name: 'Date', value: new Date().toUTCString()});
          };

          newScope.tryIt = () => {
            // convert headers, better than watching it which would be slow for no real reason
            newScope.request.headers = newScope.headers.filter(h => !!h.name && !!h.value).reduce((accumulator, e) => {
              accumulator[e.name] = e.value;
              return accumulator;
            }, {});
            tryMeService.request(newScope.request)
              .success(result => {
                let responseScope = $scope.$new();
                responseScope.payloadOptions = newScope.payloadOptions;
                responseScope.response = result;
                responseScope.response.headers = Object.keys(responseScope.response.headers).map(key => {
                   return {name: key, value: responseScope.response.headers[key]};
                });
                ngDialog.open({ template: require('../templates/try_me_response.jade'), plain: true, scope: responseScope });
              })
              .error(err => systemMessagesService.error('Can\'t execute the request, check the information please'));
          };

          ngDialog.open({ template: require('../templates/try_me.jade'), plain: true, scope: newScope, width: '80%' });
        };

        $scope['onEditCount'] = {};
        $scope['onEditModeOn'] = (uniqueId) => $timeout(() => $scope.$apply(() => {
            $scope['onEditCount'][uniqueId] = {};
            $scope['isOnEdit'] = true;
            $log.info(`New field on edit mode. ${uniqueId}`);
            $log.info($scope['onEditCount']);
        }));
        $scope['onEditModeOff'] = (uniqueId) => $timeout(() => $scope.$apply(() => {
            delete $scope['onEditCount'][uniqueId];
            $scope['isOnEdit'] = !_.isEmpty($scope['onEditCount']);
            $log.info(`Field out of edit mode. ${uniqueId} -> onEditMode: ${$scope['isOnEdit']}`);
            $log.info($scope['onEditCount']);
        }));
        $scope.updateEndpoint = e => $scope.endpoint = e;
        $timeout(function () {
          $scope.$apply(function () {
            $scope.history = null;
            $scope['endpoint'] = {
              httpMethod: "",
              path: "",
              operation: {}
            };
          });
        }).then(function () {
          if ($scope.requestMetadata.endpointPath) {
            srv.getDetailsFromMetadata($scope.requestMetadata)
            .then(function (detailsResponse) {
              $scope['applicationId'] = detailsResponse['data']['applicationId'];
              $scope['endpointId'] = detailsResponse['data']['endpointId'];
              if(detailsResponse['data']) {
                let links = tribeLinkHeaderService.parseLinkHeader(detailsResponse['data']['operation']['x-tribestream-api-registry']['links']);
                $scope.historyLink = links['history'];
                $scope.reloadHistory();
                $scope.applicationLink = links['application'];
                $scope.endpointLink = links['self'];
                $scope.endpointsLink = null;
                $timeout(function () {
                  $scope.$apply(function () {
                    let detailsData = detailsResponse['data'];
                    $scope['endpoint']['httpMethod'] = detailsData['httpMethod'];
                    $scope['endpoint'].path = detailsData.path;
                    $scope['endpoint'].operation = detailsData.operation;
                  });
                });
              }
              srv.getApplicationDetails($scope.applicationLink).then(function (applicationDetails) {
                $timeout(function () {
                  $scope.$apply(function () {
                    if (!applicationDetails['data'] || !applicationDetails['data'].swagger) {
                      $log.error("Got no application details!");
                    }
                    $scope.application = applicationDetails['data'];
                  });
                });
              });
            });
          } else {
            srv.getApplicationDetailsFromName($scope.requestMetadata.applicationName).then(function (response) {
              $timeout(function () {
                $scope.$apply(function () {
                  if (!response['data'] || !response['data'].swagger) {
                    $log.error("Got no application details!");
                  }
                  $scope.application = response['data'];
                  let links = tribeLinkHeaderService.parseLinkHeader(response['data']['swagger']['x-tribestream-api-registry']['links']);
                  $scope.applicationLink = links['self'];
                  $scope.endpointLink = null;
                  $scope.endpointsLink = links['endpoints'];
                  $scope.historyLink = null;
                });
              });
            });
          }
          var handleError = function(errorResponse) {
              if(errorResponse['data'] && errorResponse['data']['key'] === "duplicated.endpoint.exception") {
                  systemMessagesService.error(`There is an existing endpoint with the same Verb and
                        Path combination. Please try it again with new data.`);
              } else {
                  systemMessagesService.error("Unable to create endpoint.");
              }
          };
          $scope.save = () => {
            if (!!$scope.endpoint.endpointProtocol) {
              $scope.endpoint.operation.schemes = [$scope.endpoint.endpointProtocol];
            }
            if ($scope.endpointLink) {
              srv.saveEndpoint($scope.endpointLink, {
                // Cannot simply send the endpoint object because it's polluted with errors and expectedValues
                httpMethod: $scope['endpoint']['httpMethod'],
                path: $scope['endpoint'].path,
                operation: $scope['endpoint'].operation
              }).then(
                function (saveResponse) {
                  systemMessagesService.info("Saved endpoint details!");
                  $scope.reloadHistory();
                }, handleError
              );
            } else {
              srv.createEndpoint($scope.endpointsLink, {
                // Cannot simply send the endpoint object because it's polluted with errors and expectedValues
                httpMethod: $scope['endpoint']['httpMethod'],
                path: $scope['endpoint'].path,
                operation: $scope['endpoint'].operation
              }).then(
                function (saveResponse) {
                  systemMessagesService.info("Created new endpoint! " + saveResponse.status);
                  let res = saveResponse.data;
                  let app = $scope['application'];
                  let appName = app['humanReadableName'];
                  $location.path(`endpoint/${appName}/${res.httpMethod}/${res.path}`);
                }, handleError
              );
            }
          };
          $scope.delete = () => {
            srv.delete($scope.endpointLink).then((response) => {
                systemMessagesService.info("Deleted endpoint!");
                $location.path("/application/" + $scope.requestMetadata.applicationName);
            });
          };
          $scope.reloadHistory = () => {
            if(!$scope.historyLink) {
              return;
            }
            srv.getHistory($scope.historyLink).then(function(response) {

              let links = tribeLinkHeaderService.parseLinkHeader(response['data']['links']);
              for (let entry of response['data']['items']) {
                entry.link = links["revision " + entry.revisionId];
              }

              $timeout(function () {
                $scope.$apply(function () {
                  $scope.history = response['data']['items'];
                });
              });
            });
          };
          // Triggered by selecting one revision, will load it and show it
          $scope.showHistoricEndpoint = function(historyItem) {
            srv.getHistoricItem(historyItem).then(function(response) {
              $timeout(function () {
                $scope.$apply(function () {
                  let detailsData = response['data'];
                  $scope['historyItem'] = historyItem;
                  $scope['endpoint']['httpMethod'] = detailsData['httpMethod'];
                  $scope['endpoint'].path = detailsData.path;
                  $scope['endpoint'].operation = detailsData.operation;
                });
              });
            });
          };
        });
      }],
      link: (scope, el) => $timeout(() => {
          scope.$watch('historyLink', () => {
              if(!scope['historyLink']) {
                  return;
              }
              el.find('div.history').on('click', () => {
                  var winEl = angular.element($window);
                  var calculateScroll = () => {
                      var target = el.find('div[data-app-endpoints-details-history]');
                      return target.offset().top;
                  };
                  winEl.scrollTop(calculateScroll());
              });
          });
      })
    };
  }])

    .directive('setClassWhenAtTop', ['$window', function($window) {
        function stickyNavLink(scope, element){
            var window = angular.element($window),
                size = element[0].clientHeight,
                top = 0,
                fixedClass = 'fixed-header';

            function stickyNav(){
                if (!element.hasClass(fixedClass)) {
                    if($window.pageYOffset > top + size) element.addClass(fixedClass);
                } else if($window.pageYOffset <= top + size) {
                    element.removeClass(fixedClass);
                }
            }

            function resizeNav(){
                element.removeClass(fixedClass);
                top = element[0].getBoundingClientRect().top + $window.pageYOffset;
                size = element[0].clientHeight;
                stickyNav();
            }

            window.bind('resize', resizeNav);
            window.bind('scroll', stickyNav);
        }

        return {
            scope: {
            },
            restrict: 'A',
            link: stickyNavLink
        };
    }])

  .config(['uiSelectConfig', function(uiSelectConfig) {
    uiSelectConfig.theme = 'selectize';
  }]);
}
