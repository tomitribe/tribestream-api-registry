angular.module('website-components-field-actions', [])

    .directive('tribeFieldActions', ['$interval', '$document', ($interval, $document) => {
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
                    // do nothing if not attached to the DOM
                    if(floatingBody.parent().length) {
                        let position = element.offset();
                        floatingBody.offset(position);
                    }
                };
                scope.$watch('active', () => {
                    if (scope.active) {
                        body.append(floatingBody);
                        adjustOffset();
                    } else {
                        floatingBody.detach();
                    }
                });
                let adjustInterval = $interval(adjustOffset, 500);
                scope.$on('$destroy', () => {
                    $interval.cancel(adjustInterval);
                    floatingBody.remove();
                    element.remove();
                });
            }
        };
    }]);
