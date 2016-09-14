angular.module('website-components', [
    'ui.codemirror',
    'hc.marked',
    'website-components-multiselect',
    'website-components-singleselect'
])
    .filter('uriencode', ['$window', function ($window) {
        return $window.encodeURIComponent;
    }])

    .filter('pathencode', [function () {
        return function (input) {
            // The root path is the most simple case
            if (input === '/') {
                return '/';
            }
            return input.split('/')
                .map((part) => {
                    return part.match('\{.*\}') ? ':' + part.slice(1, -1) : part
                })
                .join('/');
        }
    }])

    .filter('tribeHtml', ['$sce', function ($sce) {
        return function (input) {
            return $sce.trustAsHtml(input);
        }
    }])

    .filter('tribeHtmlText', [function () {
        return function (input) {
            var el = angular.element('<div></div>');
            el.append(input);
            return el.text();
        }
    }])

    .directive('tribeEditableButtonText', [function () {
        return {
            restrict: 'A',
            scope: {
                value: '='
            },
            templateUrl: 'app/templates/component_editable_button_text.html'
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
            templateUrl: 'app/templates/component_editable_link.html',
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
            templateUrl: 'app/templates/component_editable_md.html',
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
            templateUrl: 'app/templates/component_editable_block.html',
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
            templateUrl: 'app/templates/component_editable_number.html',
            link: function (scope, el, attrs, controller) {
                var activate = function () {
                    var span = el.find('span');
                    var width = span.width();
                    el.addClass('edit');
                    var input = el.find('input');
                    if (scope.adjust !== 'false') {
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

    .directive('tribeEditableText', ['$timeout', '$interval', function ($timeout, $interval) {
        return {
            restrict: 'A',
            scope: {
                value: '=',
                adjust: '@?',
                emptyText: '@?'
            },
            templateUrl: 'app/templates/component_editable_text.html',
            link: function (scope, el) {
                $timeout(function () {
                    var activate = function () {
                        var span = el.find('span');
                        var width = span.width();
                        el.addClass('edit');
                        var input = el.find('input');
                        if (scope.adjust !== 'false') {
                            input.width(width);
                        }
                        input.focus();
                    };
                    el.on('click', activate);
                    el.find('input').on('blur', function () {
                        el.removeClass('edit');
                    });
                    el.find('> div').on('focus', activate);
                });
            }
        };
    }])

    .directive('tribeEditableOption', ['$timeout', '$document', function ($timeout, $document) {
        return {
            restrict: 'A',
            scope: {
                value: '=',
                options: '=',
                emptyText: '@?'
            },
            templateUrl: 'app/templates/component_editable_option.html',
            controller: ['$scope', '$timeout', function ($scope, $timeout) {
                if (!$scope.emptyText) {
                    $scope.emptyText = 'empty';
                }
                $scope.selectOption = function (opt) {
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.value = opt;
                        });
                    });
                };
                $scope.$watch('value', function () {
                    if (!$scope.value) {
                        $timeout(function () {
                            $scope.$apply(function () {
                                $scope.valueText = '';
                            });
                        });
                    } else {
                        $timeout(function () {
                            $scope.$apply(function () {
                                for (let opt of $scope.options) {
                                    if (opt === $scope.value || opt.value === $scope.value) {
                                        $scope.valueText = opt.text ? opt.text : opt;
                                        return;
                                    }
                                }
                                $scope.valueText = '';
                            });
                        });
                    }
                });
            }],
            link: function (scope, el, attrs, controller) {
                $timeout(function () {
                    var optionsDiv = el.find('.options');
                    optionsDiv.detach();
                    var body = $document.find('body');
                    var clear = function () {
                        el.removeClass('visible');
                        optionsDiv.detach();
                    };
                    var elWin = $($document.find('div[data-app-endpoints-details] > div'));
                    el.on('click', function () {
                        if (el.hasClass('visible')) {
                            optionsDiv.detach();
                            el.removeClass('visible');
                            elWin.off('scroll', clear);
                        } else {
                            var pos = el.find('> div').offset();
                            optionsDiv.css({
                                top: `${pos.top + el.find('> div').height()}px`,
                                left: `${pos.left}px`
                            });
                            body.append(optionsDiv);
                            el.addClass('visible');
                            elWin.on('scroll', clear);
                        }
                    });
                    optionsDiv.on('click', function () {
                        optionsDiv.detach();
                        el.removeClass('visible');
                        elWin.off('scroll', clear);
                    });
                    scope.$on('$destroy', function () {
                        optionsDiv.remove();
                        elWin.off('scroll', clear);
                    });
                });
            }
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
            templateUrl: 'app/templates/component_switch.html',
            link: function (scope, el) {
                scope.uniqueId = _.uniqueId('tribeSwitch-');
                var updateValue = function () {
                    $timeout(function () {
                        scope.$apply(function () {
                            scope.value = !scope.value;
                        });
                    });
                };
                el.find('div[tabindex]').on('click', updateValue);
                el.find('div[tabindex]').on('keypress', updateValue);
            },
            controller: ['$scope', function ($scope) {
                if (_.isNull($scope.trueValue) || _.isUndefined($scope.trueValue)) {
                    $scope.trueValue = true;
                }
                if (_.isNull($scope.falseValue) || _.isUndefined($scope.falseValue)) {
                    $scope.falseValue = false;
                }
            }]
        };
    }]);
