module _components_diff {

    let codemirror = require("../../../static/bower_components/codemirror/lib/codemirror.js");

    angular.module('website-components-diff', [])

        .directive('tribeDiff', ['$document', '$timeout', ($document, $timeout) => {
            return {
                restrict: 'A',
                scope: {
                    valueA: '=',
                    titleA: '=',
                    valueB: '=',
                    titleB: '=',
                    mode: '@?'
                },
                template: require('../templates/component_diff.jade'),
                controller: ['$scope', ($scope) => {
                    $scope['loaded'] = false;
                    if(!$scope['mode']) {
                        $scope['mode'] = 'application/json';
                    }
                }],
                link: (scope, el) => $timeout(() => {
                    let checkLoaded = () => {
                        if (!scope['valueA']) {
                            return;
                        }
                        if (!scope['valueB']) {
                            return;
                        }
                        $timeout(() => scope.$apply(() => scope['loaded'] = true));
                    };
                    scope.$watch('valueA', checkLoaded);
                    scope.$watch('valueB', checkLoaded);
                    scope.$watch('loaded', () => {
                        if (!scope['loaded']) {
                            return;
                        }
                        codemirror.MergeView(el.find('> div')[0], {
                            value: scope['valueA'],
                            orig: scope['valueB'],
                            mode: scope['mode'],
                            connect: 'align',
                            lineNumbers: true,
                            highlightDifferences: true,
                            collapseIdentical: false,
                            lineWrapping: true
                        });
                        if(scope['titleA'] && scope['titleB']) {
                            $timeout(() => {
                                let diffPanels = el.find('div.CodeMirror-merge-pane');
                                // left
                                angular.element(diffPanels[0]).prepend(`<h3 class="diff-title">${scope['titleA']}</h3>`);
                                // right
                                angular.element(diffPanels[1]).prepend(`<h3 class="diff-title">${scope['titleB']}</h3>`);
                                $timeout(() => {
                                    let titleHeight = el.find('div.CodeMirror-merge-pane h3.diff-title').outerHeight();
                                    let cmHeight = el.find('div.CodeMirror-merge-pane h3.diff-title ~ div.CodeMirror').outerHeight();
                                    el.find('div.CodeMirror-merge-pane h3.diff-title ~ div.CodeMirror').outerHeight(cmHeight - titleHeight);
                                });
                            });
                        }

                    });
                })
            }
        }]);

}