///<reference path="../../bower_components/DefinitelyTyped/angularjs/angular.d.ts"/>

angular
    .module('tribe-authentication', [
        'website-services',
        'tribe-alerts'
    ])

    .directive('appLogin', [function () {
        return {
            restrict: 'A',
            scope: true,
            controller: ['$scope', '$location', 'tribeAuthorizationService', '$sessionStorage', 'systemMessagesService',
                function ($scope,
                          $location,
                          authorization,
                          $sessionStorage,
                          systemMessagesService) {
                    var me = this;
                    me.onMessage = function (msgs) {
                        $scope.messages = msgs;
                    };
                    systemMessagesService.addListener(me);
                    me.disconnectListener = function () {
                        systemMessagesService.removeListener(me);
                    };
                    var redirect = function() {
                        var path = authorization.targetPath;
                        if (path) {
                            // in case we have a saved target url, use it instead of "/"
                            delete authorization.targetPath;
                            $location.path(path);
                        } else {
                            // redirect to home
                            $location.path('/');
                        }
                    };
                    $scope.login = function () {
                        var credentials = {
                            username: $scope.username,
                            password: $scope.password
                        };
                        authorization.login(credentials).success(function (data) {
                            $sessionStorage.tribe.isConnected = true;
                            $sessionStorage.tribe.user = data;

                            // init the api
                            authorization.setCredentials(credentials.username, credentials.password);
                            redirect();
                        }).error(function (data, status, headers, config) {
                            systemMessagesService.error('The server authentication failed with status ' + status);
                        });
                    };
                    $scope.loginAsGuest = function() {
                        $sessionStorage.tribe.isConnected = true;
                        redirect();
                    };
                }
            ],
            link: function (scope, el, attrs, controller) {
                el.on('$destroy', function () {
                    controller.disconnectListener();
                });
            }
        };
    }])

    .directive('appLogout', [function () {
        return {
            restrict: 'A',
            controller: ['$scope', '$window', '$location', 'tribeAuthorizationService',
                function ($scope, $window, $location, authorization) {
                    this.logout = function () {
                        authorization.clearCredentials();
                        $location.search({});
                        $location.path('/');
                        $window.location.reload();
                    };
                }
            ],
            link: function (scope, el, attr, ctlr) {
                el.on('click', function () {
                    ctlr.logout();
                });
            }
        };
    }])

    .directive('appLoggedIn', [function () {
        return {
            restrict: 'A',
            scope: true,
            controller: ['$scope', '$sessionStorage',
                function ($scope, $sessionStorage) {
                    if ($sessionStorage.tribe && $sessionStorage.tribe.user) {
                        $scope.user = $sessionStorage.tribe.user.name;
                    } else {
                        $scope.user = 'guest';
                    }
                }
            ]
        };
    }])

    .run(function () {
        // placeholder
    });

