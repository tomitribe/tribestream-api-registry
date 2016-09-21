angular.module('website-components-text', [
    'website-components-field-actions'
])

    .directive('tribeText', ['$window', '$timeout', ($window, $timeout) => {
        return {
            restrict: 'A',
            scope: {
                originalValue: '=value',
                type: '@'
            },
            templateUrl: 'app/templates/component_text.html',
            controller: ['$log', '$scope', ($log, $scope) => $timeout(() => {
                $scope.version = 0;
                $scope.fieldDirty = false;
                $scope.$watch('originalValue', () => $timeout(() => $scope.$apply(() => {
                    $scope.value = _.clone($scope.originalValue);
                })));
                $scope.onCommit = () =>  $timeout(() => $scope.$apply(() => {
                    if ($scope.fieldDirty) {
                        $scope.fieldDirty = false;
                        $scope.originalValue = _.clone($scope.value);
                        $scope.$broadcast('fieldCommited');
                    }
                }));
                $scope.onCancel = () =>  $timeout(() => $scope.$apply(() => {
                    $scope.fieldDirty = false;
                    $scope.value = _.clone($scope.originalValue);
                    $scope.$broadcast('fieldCanceled');
                }));
                $scope.onChange = () =>  $timeout(() => $scope.$apply(() => {
                    $scope.version = $scope.version + 1;
                    if ($scope.originalValue !== $scope.value) {
                        $scope.fieldDirty = true;
                    }
                }));
                $scope.keyEntered = (event) =>  $timeout(() => $scope.$apply(() => {
                    if (event.keyCode === 13 /* Enter */) {
                        $scope.onCommit();
                    } else if (event.keyCode === 27 /* Escape */) {
                        $scope.onCancel();
                    } else {
                        $scope.onChange();
                    }
                }));
            })],
            link: (scope, element) =>  $timeout(() => {
                var deactivatePromise = null;
                let cancelDeactivate = () => {
                    if (deactivatePromise) {
                        $timeout.cancel(deactivatePromise);
                    }
                    deactivatePromise = null;
                };
                let deactivate = () => {
                    cancelDeactivate();
                    deactivatePromise = $timeout(() => {
                        scope.onCommit();
                        element.removeClass('active');
                    }, 500);
                };
                let input = element.find('input');
                scope.$on('fieldCanceled', () => input.blur());
                scope.$on('fieldCommited', () => input.blur());
                input.on('blur', () => deactivate());
                input.on('focus', () => {
                    cancelDeactivate();
                    element.addClass('active');
                    input.select();
                    $timeout(() => scope.$apply(() => {
                        scope.version = scope.version + 1;
                        scope.fieldDirty = true;
                    }));
                });
                element.find('> div').on('focus', () => input.focus());
                scope.$on('$destroy', () => element.remove());
            })
        };
    }]);
