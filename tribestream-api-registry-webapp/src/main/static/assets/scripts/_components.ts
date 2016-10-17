module components {

let marked = require("../../../static/bower_components/marked/marked.min.js");

angular.module('website-components', [
    'ui.codemirror',
    'website-components-filters',
    'website-components-multiselect',
    'website-components-singleselect',
    'website-components-text',
    'website-components-markdown'
])

    .directive('tribeConfirmBtn', ['$timeout', function ($timeout) {
        return {
            restrict: 'A',
            scope: {
                callback: '=',
                message: '@',
                icon: '@',
                label: '@?'
            },
            template: require('../templates/component_confirm_btn.jade'),
            link: function (scope, el) {
                el.find('.trigger').on('click', () => {
                    el.addClass('visible');
                });
                el.find('button.cancel').on('click', () => {
                    el.removeClass('visible');
                });
                el.find('button.confirm').on('click', () => {
                    el.removeClass('visible');
                    scope['callback']();
                });
            }
        };
    }])

    .directive('tribeEditableButtonText', [function () {
        return {
            restrict: 'A',
            scope: {
                value: '='
            },
            template: require('../templates/component_editable_button_text.jade')
        };
    }])

    .directive('tribeEditableLink', ['$document', function ($document) {
        return {
            restrict: 'A',
            scope: {
                href: '=',
                title: '=',
                emptyText: '@?'
            },
            template: require('../templates/component_editable_link.jade'),
            controller: ['$scope', function ($scope) {
                if (!$scope.emptyText || $scope.emptyText.trim() === '') {
                    $scope.emptyText = 'Empty link';
                }
            }],
            link: function (scope, el, attrs, controller) {
                var valueDiv = el.find('.value');
                valueDiv.detach();
                var body = $document.find('body');
                var clear = function () {
                    el.removeClass('visible');
                    valueDiv.detach();
                };
                var elWin = $($document.find('div[data-app-endpoints-details] > div'));
                el.find('div.edit-trigger').on('click', function () {
                    if (el.hasClass('visible')) {
                        valueDiv.detach();
                        el.removeClass('visible');
                        valueDiv.off('scroll', clear);
                    } else {
                        var pos = el.find('> div').offset();
                        valueDiv.css({
                            top: `${pos.top + el.find('> div').outerHeight()}px`,
                            left: `${pos.left}px`
                        });
                        body.append(valueDiv);
                        el.addClass('visible');
                        elWin.on('scroll', clear);
                    }
                });
                scope.$on('$destroy', function () {
                    valueDiv.remove();
                    elWin.off('scroll', clear);
                });


            }
        };
    }])


    .directive('tribeEditableMd', [function () {
        return {
            restrict: 'A',
            scope: {
                content: '='
            },
            template: require('../templates/component_editable_md.jade'),
            controller: ['$scope', '$timeout', '$element', function ($scope, $timeout, $element) {
                $scope.editorHolder = {
                    editor: null
                };
                $scope.cmOption = {
                    lineNumbers: false,
                    lineWrapping: true,
                    viewportMargin: Infinity,
                    mode: 'markdown',
                    onLoad: function (editor) {
                        $timeout(function () {
                            $scope.$apply(function () {
                                $scope.editorHolder.editor = editor;
                            });
                        });
                    }
                };
                $scope.$watch('content', function () {
                    $timeout(function () {
                        $scope.$apply(function () {
                            if (!$scope.content || $scope.content.trim() === '') {
                                $scope.compiledContent = '';
                            } else {
                                $scope.compiledContent = marked($scope.content);
                            }
                        });
                    });
                });
                $scope.codemirrorLoaded = function (_editor) {
                    _editor.on("blur", function () {
                        $element.removeClass('edit');
                    });
                };
            }],
            link: function (scope, el, attr, controller) {
                scope.$watch('editorHolder.editor', function () {
                    var editor = scope.$eval('editorHolder.editor');
                    if (editor) {
                        var activate = function () {
                            el.addClass('edit');
                            editor.refresh();
                            editor.focus();
                        };
                        el.on('click', activate);
                        el.find('> div').on('focus', activate);
                        editor.on('blur', function () {
                            el.removeClass('edit');
                        });
                    }
                });

            }
        };
    }])

    .directive('tribeEditableBlock', [function () {
        return {
            restrict: 'A',
            scope: {
                content: '='
            },
            template: require('../templates/component_editable_block.jade'),
            controller: ['$scope', '$timeout', function ($scope, $timeout) {
                $scope.editorHolder = {
                    editor: null
                };
                $scope.cmOption = {
                    lineNumbers: false,
                    lineWrapping: true,
                    viewportMargin: Infinity,
                    onLoad: function (editor) {
                        $timeout(function () {
                            $scope.$apply(function () {
                                $scope.editorHolder.editor = editor;
                            });
                        });
                    }
                };
            }],
            link: function (scope, el, attr, controller) {
                scope.$watch('editorHolder.editor', function () {
                    var editor = scope.$eval('editorHolder.editor');
                    if (editor) {
                        editor.refresh();
                        var activate = function () {
                            el.addClass('edit');
                            editor.refresh();
                            editor.focus();
                        };
                        el.on('click', activate);
                        el.find('> div').on('focus', activate);
                        editor.on('blur', function () {
                            el.removeClass('edit');
                        });
                    }
                });

            }
        };
    }])

    .directive('tribeEditableNumber', ['$timeout', function ($timeout) {
        return {
            restrict: 'A',
            scope: {
                value: '=',
                adjust: '@?'
            },
            template: require('../templates/component_editable_number.jade'),
            link: function (scope, el, attrs, controller) {
                var activate = function () {
                    var span = el.find('span');
                    var width = span.width();
                    el.addClass('edit');
                    var input = el.find('input');
                    if (scope['adjust'] !== 'false') {
                        input.width(width);
                    }
                    input.focus();
                };
                el.on('click', activate);
                el.find('input').on('blur', function () {
                    el.removeClass('edit');
                });
                el.find('> div').on('focus', activate);
            }
        };
    }])

    .directive('tribeEditableOption', ['$timeout', '$document', function ($timeout, $document) {
        return {
            restrict: 'A',
            scope: {
                value: '=',
                options: '=',
                defaultText: '@?',
                emptyText: '@?'
            },
            template: require('../templates/component_editable_option.jade'),
            controller: ['$scope', '$timeout', function ($scope, $timeout) {
                $scope.visible = false;
                if (!$scope.emptyText) {
                    $scope.emptyText = 'empty';
                }
                $scope.selectOption = (opt) => $timeout(() => $scope.$apply(() => {
                    $scope.value = opt;
                    $scope.visible = false;
                }));
                $scope.$watch('value', () => {
                    if (!$scope.value) {
                        $timeout(() => $scope.$apply(() => $scope.valueText = ''));
                    } else {
                        $timeout(() => $scope.$apply(() => {
                            for (let opt of $scope.options) {
                                if (opt === $scope.value || opt.value === $scope.value) {
                                    $scope.valueText = opt.text ? opt.text : opt;
                                    return;
                                }
                            }
                            $scope.valueText = '';
                        }));
                    }
                });
                $scope.open = () => $timeout(() => $scope.$apply(() => {
                    $scope.visible = true;
                }));
            }],
            link: (scope, el) => $timeout(() => {
                var optionsDiv = el.find('.options');
                optionsDiv.detach();
                let body = $document.find('body');
                var detachPromise = null;
                let detachOptions = () => {
                    optionsDiv.detach();
                    el.removeClass('visible');
                    $timeout(() => scope.$apply(() => {
                        scope['visible'] = false;
                    }));
                };
                el.on('mouseover', () => {
                    $document.off('click', detachOptions);
                });
                optionsDiv.on('mouseover', () => {
                    $document.off('click', detachOptions);
                });
                el.on('mouseout', () => {
                    $document.on('click', detachOptions);
                });
                optionsDiv.on('mouseout', () => {
                    $document.on('click', detachOptions);
                });
                scope.$watch('visible', () => {
                    if (scope['visible']) {
                        el.addClass('visible');
                        var pos = el.find('> div').offset();
                        optionsDiv.css({
                            top: `${pos.top + el.find('> div').height()}px`,
                            left: `${pos.left}px`
                        });
                        body.append(optionsDiv);
                        $document.on('click', detachOptions);
                    } else {
                        detachOptions();
                        $document.off('click', detachOptions);
                    }
                });
                let escapeDetach = (event) => {
                    if (event.keyCode === 27 /* Escape */) {
                        detachOptions();
                    }
                };
                $document.on('keyup', escapeDetach);
                scope.$on('$destroy', function () {
                    el.remove();
                    optionsDiv.remove();
                    $document.off('click', detachOptions);
                    $document.off('keyup', escapeDetach);
                });
            })
        };
    }])

    .directive('tribeSwitch', ['$timeout', function ($timeout) {
        return {
            restrict: 'A',
            scope: {
                value: '=',
                trueValue: '=?',
                falseValue: '=?'
            },
            template: require('../templates/component_switch.jade'),
            link: function (scope, el) {
                scope['uniqueId'] = _.uniqueId('tribeSwitch-');
                var updateValue = function () {
                    $timeout(function () {
                        scope.$apply(function () {
                            scope['value'] = !scope['value'];
                        });
                    });
                };
                el.find('div[tabindex]').on('click', updateValue);
                el.find('div[tabindex]').on('keypress', updateValue);
            },
            controller: ['$scope', function ($scope) {
                if (_.isNull($scope['trueValue']) || _.isUndefined($scope['trueValue'])) {
                    $scope['trueValue'] = true;
                }
                if (_.isNull($scope['falseValue']) || _.isUndefined($scope['falseValue'])) {
                    $scope['falseValue'] = false;
                }
            }]
        };
    }]);

}
