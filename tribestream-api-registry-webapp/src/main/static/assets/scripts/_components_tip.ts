angular.module('website-components-tip', [])

    .directive('tribeTip', ['$window', '$timeout', '$interval', '$document', ($window, $timeout, $interval, $document) => {
        return {
            restrict: 'A',
            scope: {
                message: '@',
                visible: '='
            },
            template: require('../templates/component_tip.jade'),
            controller: ['$log', '$scope', ($log, $scope) => {

            }],
            link: (scope, el) => $timeout(() => {
                let parent = angular.element(el.parent());
                el.detach();
                var body = $document.find('body');
                body.append(el);
                let adjust = () => {
                    let parentPosition = parent.offset();
                    el.offset({
                        top: parentPosition.top + parent.height(),
                        left: parentPosition.left + 40
                    });
                };
                var adjustPromise = null;
                scope.$watch('visible', () => {
                    if (scope['visible']) {
                        adjustPromise = $interval(adjust, 1000);
                    } else {
                        if (adjustPromise) {
                            $interval.cancel(adjustPromise);
                        }
                        adjustPromise = null;
                    }
                });
                scope.$on('$destroy', () => {
                    if (adjustPromise) {
                        $interval.cancel(adjustPromise);
                    }
                    el.remove();
                });
            })
        };
    }]);
