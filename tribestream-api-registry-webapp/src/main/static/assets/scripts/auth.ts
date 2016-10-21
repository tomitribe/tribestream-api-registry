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
            controller: ['$scope', '$location', '$timeout', 'tribeAuthorizationService', 'systemMessagesService', 'tribeHeaderProviderSelector', 'currentAuthProvider',
                function ($scope,
                          $location,
                          $timeout,
                          authorization,
                          systemMessagesService,
                          tribeHeaderProviderSelector,
                          currentAuthProvider) {
                    var me = this;
                    me.onMessage = function (msgs) {
                        $scope.messages = msgs;
                    };
                    systemMessagesService.addListener(me);
                    me['disconnectListener'] = function () {
                        systemMessagesService.removeListener(me);
                    };
                    var redirect = function() {
                        var path = authorization.targetPath;
                        if (path && path !== "/login") {
                            // in case we have a saved target url, use it instead of "/"
                            delete authorization.targetPath;
                            $location.path(path);
                        } else {
                            // redirect to home
                            $location.path('/');
                        }
                    };
                    authorization.getOauth2Status().then((response) => {
                        $timeout(() => {
                            $scope.$apply(() => {
                              $scope.oauth2Status = response.data;
                            });
                        });
                    });
                    // we don't activate the switch yet so enforce it but keep the logic once we'll have themed it
                    $scope.login = function () {
                        let headerProvider;
                        if ($scope.companyLogin) {
                            headerProvider = tribeHeaderProviderSelector.select('OAuth2');
                        } else {
                            headerProvider = tribeHeaderProviderSelector.select('Basic');
                        }

                        headerProvider.login($scope.username, $scope.password)
                          .then(
                              function() {
                                  authorization.setCredentials($scope.username, headerProvider.getState());
                                  currentAuthProvider.set(headerProvider);
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
                    controller['disconnectListener']();
                });
            }
        };
    }])

    .directive('appLogout', ['currentAuthProvider', function (currentAuthProvider) {
        return {
            restrict: 'A',
            controller: ['$scope', '$window', '$location', 'tribeAuthorizationService',
                function ($scope, $window, $location, authorization) {
                    this['logout'] = function () {
                        currentAuthProvider.reset();
                        authorization.clearCredentials();
                        $location.search({});
                        $location.path('/login');
                        $window.location.reload();
                    };
                }
            ],
            link: function (scope, el, attr, ctlr) {
                el.on('click', function () {
                    ctlr['logout']();
                });
            }
        };
    }])

    .run(function () {
        // placeholder
    });
