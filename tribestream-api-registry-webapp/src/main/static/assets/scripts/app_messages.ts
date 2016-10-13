angular.module('website-messages', [
        'website-services',
        'tribe-alerts'
    ])

    .directive('appNgClickConfirm', ['$document', '$parse', '$compile', function ($document, $parse, $compile) {
        return {
            restrict: 'A',
            link: function (scope, el, attrs) {
                var getMessage = function (attrKey, strDefault) {
                    var msg = attrs[attrKey];
                    if (!msg) {
                        msg = strDefault;
                    }
                    return msg;
                };
                var appConfirmationTitle = getMessage('appConfirmationMessageTitle', '');
                var appConfirmationMessage = getMessage('appConfirmationMessage', 'Do you confirm this action?');
                var appConfirmationMessageYes = getMessage('appConfirmationMessageYes', 'yes');
                var appConfirmationMessageNo = getMessage('appConfirmationMessageNo', 'no');
                var html = $compile(['<i, data-app-ng-click-confirm-el ',
                    'data-action="' + attrs['appNgClickConfirm'] + '" ',
                    'data-title="' + appConfirmationTitle + '" ',
                    'data-message="' + appConfirmationMessage + '" ',
                    'data-yes="' + appConfirmationMessageYes + '" ',
                    'data-no="' + appConfirmationMessageNo + '"',
                    '></i>'].join(''))(scope.$new());
                el.on('click', function () {
                    var body = $document.find('body');
                    body.append(html);
                });
                el.on('$destroy', function () {
                    html.remove();
                });
            }
        };
    }])

    .directive('appNgClickConfirmEl', [function () {
        return {
            restrict: 'A',
            template: require('../templates/component_confirmation_popup.jade'),
            controller: ['$timeout', '$element', '$scope', '$parse', function ($timeout, $element, $scope, $parse) {
                $scope.yesClick = function () {
                    $timeout(function () {
                        $element.detach();
                        var fn = $parse($scope['action'], null, true);
                        var callback = function () {
                            fn($scope, {$event: event});
                        };
                        $scope.$apply(callback);
                    });
                };
                $scope.noClick = function () {
                    $element.detach();
                };
            }],
            link: function (scope, el, attrs) {
                scope['action'] = attrs['action'];
                scope['title'] = attrs['title'];
                scope['message'] = attrs['message'];
                scope['yes'] = attrs['yes'];
                scope['no'] = attrs['no'];
            }
        };
    }])

    .directive('appClosableMessages', [function () {
        return {
            restrict: 'A',
            scope: true,
            template: require('../templates/app_closable_messages.jade'),
            controller: ['$scope', '$timeout', 'systemMessagesService',
                function ($scope, $timeout, systemMessagesService) {
                    $scope.messages = systemMessagesService.getMessages();
                    var me = this;
                    me.onMessage = function (msgs) {
                        $timeout(function () {
                            $scope.messages = _.reject(msgs, function (msg) {
                                return msg['read'];
                            });
                        });
                    };
                    systemMessagesService.addListener(me);
                    me['disconnectListener'] = function () {
                        systemMessagesService.removeListener(me);
                    };
                    me['setMessageRead'] = function (msg) {
                        systemMessagesService['setMessageRead'](msg);
                    }
                }
            ],
            link: function (scope, el, attrs, controller) {
                el.on('$destroy', function () {
                    controller['disconnectListener']();
                });
            }
        };
    }])

    .directive('appClosableMessage', ['$timeout', function ($timeout) {
        return {
            restrict: 'A',
            scope: {
                message: '='
            },
            require: '^appClosableMessages',
            link: function (scope, el, attrs, controller) {
                var removeTimer;
                var setTimer = function () {
                    // remove element after X seconds
                    var timeoutValue = scope['message']['msgType'] === 'error' ? 10000 : 2000;
                    if (scope['message']['msgTimeout']) {
                        timeoutValue = scope['message']['msgTimeout'];
                    }
                    removeTimer = $timeout(function () {
                        controller['setMessageRead'](scope['message']);
                    }, timeoutValue);
                };
                setTimer();
                el.on('$destroy', function () {
                    $timeout.cancel(removeTimer);
                });
                el.on('mouseover', function () {
                    $timeout.cancel(removeTimer);
                });
                el.on('mouseout', function () {
                    setTimer();
                });
                el.on('click', function () {
                    controller['setMessageRead'](scope['message']);
                });
            }
        };
    }])

    .run(function () {
        // placeholder
    });