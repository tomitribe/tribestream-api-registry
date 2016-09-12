angular.module('website-components-field-actions', [])

    .directive('tribeFieldActions', ['$window', '$document', ($window, $document) => {
        return {
            restrict: 'A',
            scope: {
                onCancel: '&',
                onConfirm: '&',
                active: '=',
                version: '='
            },
            templateUrl: 'app/templates/component_field_actions.html',
            controller: ['$scope', ($scope) => {
                $scope.confirm = () => $scope.onConfirm();
                $scope.cancel = () => $scope.onCancel();
            }],
            link: (scope, element, attrs, controller) => {
                let floatingBody = angular.element(element.find('> div'));
                floatingBody.detach();
                var body = $document.find('body');
                let adjustOffset = () => {
                    let position = element.offset();
                    floatingBody.offset(position);
                };
                scope.$watch('active', () => {
                    if (scope.active) {
                        body.append(floatingBody);
                        adjustOffset();
                    } else {
                        floatingBody.detach();
                    }
                });
                scope.$watch('version', () => adjustOffset());
                var eWin = angular.element($window);
                eWin.bind('resize', adjustOffset);
                scope.$on('$destroy', () => {
                    eWin.unbind('resize', adjustOffset);
                    floatingBody.remove();
                    element.remove();
                });
            }
        };
    }]);
