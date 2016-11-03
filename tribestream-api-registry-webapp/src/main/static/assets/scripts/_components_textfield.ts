angular.module('website-components-text', [
    'website-components-field-actions'
])

    .directive('tribeText', ['$window', '$timeout', ($window, $timeout) => {
        return {
            restrict: 'A',
            scope: {
                originalValue: '=value',
                type: '@',
                placeholder: '@',
                regex: '@?',
                regexTip: '@?',
                onEditModeOn: '&?',
                onEditModeOff: '&?'
            },
            template: require('../templates/component_text.jade'),
            controller: ['$log', '$scope', ($log, $scope) => $timeout(() => {
                if(!$scope['regexTip']) {
                    $scope['regexTip'] = 'Invalid Pattern';
                }
                let normalTitle = 'Click to edit';
                $scope['title'] = normalTitle;
                $scope.$watch('valid', () => $timeout(() => $scope.$apply(() => {
                    if ($scope['valid'] === false) {
                        $scope['title'] = 'Invalid pattern';
                    } else {
                        $scope['title'] = normalTitle;
                    }
                })));
                $scope['version'] = 0;
                $scope['fieldDirty'] = false;
                $scope.$watch('originalValue', () => $timeout(() => $scope.$apply(() => {
                    $scope['value'] = _.clone($scope['originalValue']);
                })));
                $scope['onCommit'] = () =>  $timeout(() => $scope.$apply(() => {
                    if ($scope['fieldDirty']) {
                        $scope['fieldDirty'] = false;
                        $scope['originalValue'] = _.clone($scope['value']);
                        $scope['title'] = normalTitle;
                        $scope.$broadcast('fieldCommited');
                    }
                }));
                $scope.onCancel = () =>  $timeout(() => $scope.$apply(() => {
                    $scope['fieldDirty'] = false;
                    $scope['value'] = _.clone($scope['originalValue']);
                    $scope['title'] = normalTitle;
                    $scope['valid'] = true;
                    $scope.$broadcast('fieldCanceled');
                }));
                $scope.onChange = () =>  $timeout(() => $scope.$apply(() => {
                    $scope['version'] = $scope['version'] + 1;
                    if ($scope['originalValue'] !== $scope['value']) {
                        $scope['fieldDirty'] = true;
                    }
                }));
                $scope.keyEntered = (event) =>  $timeout(() => $scope.$apply(() => {
                    if (event.keyCode === 13 /* Enter */) {
                        $scope['onCommit']();
                    } else if (event.keyCode === 27 /* Escape */) {
                        $scope.onCancel();
                    } else {
                        $scope.onChange();
                    }
                }));
                $scope.$watchGroup(['version', 'value'], () => $timeout(() => $scope.$apply(() => {
                    if($scope['regex'] !== undefined && $scope['regex'] !== null) {
                        let value = $scope['value'];
                        let valid = new RegExp($scope['regex'], 'g').test(value);
                        $scope['valid'] = valid;
                    }
                })));
            })],
            link: (scope, element) =>  $timeout(() => {
                var deactivatePromise = null;
                let cancelDeactivate = () => {
                    if (deactivatePromise) {
                        $timeout.cancel(deactivatePromise);
                    }
                    deactivatePromise = null;
                };
                let input = element.find('input');
                let deactivate = () => {
                    cancelDeactivate();
                    deactivatePromise = $timeout(() => {
                        if (element.hasClass('invalid')) {
                            scope['onCancel']();
                        } else {
                            scope['onCommit']();
                        }
                        element.removeClass('invalid');
                        element.removeClass('active');
                        if(scope['onEditModeOff']) {
                            scope['onEditModeOff']();
                        }
                    }, 500);
                };
                scope.$on('fieldCanceled', () => input.blur());
                scope.$on('fieldCommited', () => input.blur());
                input.on('blur', () => deactivate());
                input.on('focus', () => {
                    cancelDeactivate();
                    element.addClass('active');
                    input.select();
                    $timeout(() => scope.$apply(() => {
                        scope['version'] = scope['version'] + 1;
                        scope['fieldDirty'] = true;
                        if(scope['onEditModeOn']) {
                            scope['onEditModeOn']();
                        }
                    }));
                });
                element.find('> div').on('focus', () => input.focus());
                scope.$on('$destroy', () => element.remove());
                scope.$watch('valid', () => {
                    if (scope['valid']) {
                        element.removeClass('invalid');
                    } else {
                        if(scope['regex'] !== undefined && scope['regex'] !== null) {
                            element.addClass('invalid');
                        }
                    }
                });
            })
        };
    }]);
