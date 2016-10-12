///<reference path="../../bower_components/DefinitelyTyped/angularjs/angular.d.ts"/>


class CurrentAuthProvider {

    private currentProvider: AuthenticationHeaderProvider;

    set(authenticationProvider: AuthenticationHeaderProvider) {
        this.currentProvider = authenticationProvider;
    }

    isActive(): boolean {
        return !!this.currentProvider;
    }

    get(): AuthenticationHeaderProvider {
        return this.currentProvider;
    }

    reset() {
        this.currentProvider = null;
    }

}

angular
    .module('tribe-authentication', [
        'website-services',
        'tribe-alerts',
        'tribe-services-header-providers'
    ])

    .service('currentAuthProvider', [CurrentAuthProvider])

    .directive('appLogin', [function () {
        return {
            restrict: 'A',
            scope: true,
            controller: ['$scope', '$location', 'tribeAuthorizationService', '$sessionStorage', 'systemMessagesService', 'tribeHeaderProviderSelector', 'currentAuthProvider',
                function ($scope,
                          $location,
                          authorization,
                          $sessionStorage,
                          systemMessagesService,
                          tribeHeaderProviderSelector,
                          currentAuthProvider) {
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
                        let headerProvider;
                        if ($scope.companyLogin) {
                            headerProvider = tribeHeaderProviderSelector.select('Oauth2');
                        } else {
                            headerProvider = tribeHeaderProviderSelector.select('Basic');
                        }

                        headerProvider.login($scope.username, $scope.password)
                          .then(
                              function() {
                                  $sessionStorage.tribe.user = {name: $scope.username};
                                  currentAuthProvider.set(headerProvider);
                                  // TODO: Sometimes redirects to the login page, that's stupid
                                  redirect();
                              },
                              function (response) {
                                  if (response && response.data) {
                                      if (response.data.error_description) {
                                          systemMessagesService.error('Login failed! ' + response.data.error_description);
                                      } else {
                                          systemMessagesService.error('The server authentication failed with status ' + response.data.status);
                                      }
                                  } else {
                                      systemMessagesService.error('Login failed with an unknown reason!');
                                  }
                              }
                          );
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

    .directive('appLogout', ['currentAuthProvider', function (currentAuthProvider) {
        return {
            restrict: 'A',
            controller: ['$scope', '$window', '$location', 'tribeAuthorizationService',
                function ($scope, $window, $location, authorization) {
                    this.logout = function () {
                        currentAuthProvider.reset();
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

