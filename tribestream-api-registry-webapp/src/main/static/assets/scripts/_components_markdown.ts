angular.module('website-components-markdown-service', [])

    .factory('tribeMarkdownService', [() => {
        return {
            compileMd: function (content) {
                if (content === null || content === undefined || content.trim() === '') {
                    return '';
                }
                let compiledMd = angular.element('<div></div>');
                angular.element(marked(content)).each((index, el) => {
                    compiledMd.append(el);
                });
                compiledMd.find('code').each((index, codeTag) => {
                    let aCode = angular.element(codeTag);
                    if (aCode.attr('class')) {
                        hljs.highlightBlock(codeTag)
                    }
                });
                return compiledMd.html();
            }
        };
    }]);


angular.module('website-components-markdown', [
    'website-components-field-actions',
    'website-components-filters',
    'website-components-markdown-service'
])


    .directive('tribeMarkdown', ['$window', '$timeout', '$log', 'tribeMarkdownService', ($window, $timeout, $log, mdService) => {
        return {
            restrict: 'A',
            scope: {
                originalValue: '=value'
            },
            templateUrl: 'app/templates/component_markdown.html',
            controller: ['$scope', ($scope) => $timeout(() => {
                $scope.simplemde = null;
                $scope.version = 0;
                $scope.fieldDirty = false;
                $scope.cmFocused = false;
                $scope.$watch('originalValue', () => $timeout(() => $scope.$apply(() => {
                    $scope.value = $scope.originalValue ? _.clone($scope.originalValue) : '';
                })));
                $scope.$watch('value', () => $timeout(() => $scope.$apply(() => {
                    $scope.preview = mdService.compileMd($scope.value);
                })));
                $scope.onCommit = () =>  $timeout(() => $scope.$apply(() => {
                    $scope.cmFocused = false;
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
                $scope.onChange = (newValue) =>  $timeout(() => $scope.$apply(() => {
                    $scope.version = $scope.version + 1;
                    if ($scope.originalValue !== newValue) {
                        $scope.value = newValue;
                        $scope.fieldDirty = true;
                    }
                }));
            })],
            link: (scope, el) => $timeout(() => {
                var simplemde = null;
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
                        el.removeClass('active');
                        if (simplemde && simplemde.isPreviewActive()) {
                            SimpleMDE.togglePreview(simplemde);
                        }
                    }, 500);
                };
                let focusAction = (cm) => {
                    $log.debug('codemirror focus; cm.getSelection() empty? ' + !!cm.getSelection());
                    cancelDeactivate();
                    if (!scope.cmFocused) {
                        cm.execCommand('selectAll');
                    }
                    $timeout(() => scope.$apply(() => {
                        scope.cmFocused = true;
                        scope.version = scope.version + 1;
                        scope.fieldDirty = true;
                    }));
                };
                let anchorEl = el.find('div.value > textarea')[0];
                let actionClick = (editor, callback) => {
                    cancelDeactivate();
                    callback(editor);
                    editor.codemirror.off('focus', focusAction);
                    editor.codemirror.focus();
                    editor.codemirror.on('focus', focusAction);
                };
                simplemde = new SimpleMDE({
                    element: anchorEl,
                    status: false,
                    spellChecker: false,
                    previewRender: mdService.compileMd,
                    toolbar: ["bold", "italic", "heading", "quote"],
                    toolbar: [{
                        name: "bold",
                        action: (editor) => actionClick(editor, SimpleMDE.toggleBold),
                        className: "fa fa-bold",
                        title: "Bold",
                    }, {
                        name: "italic",
                        action: (editor) => actionClick(editor, SimpleMDE.toggleItalic),
                        className: "fa fa-italic",
                        title: "Italic"
                    }, {
                        name: "heading",
                        action: (editor) => actionClick(editor, SimpleMDE.toggleHeadingSmaller),
                        className: "fa fa-header",
                        title: "Heading"
                    }, '|', {
                        name: "code",
                        action: (editor) => actionClick(editor, SimpleMDE.toggleCodeBlock),
                        className: "fa fa-code",
                        title: "Code"
                    }, {
                        name: "quote",
                        action: (editor) => actionClick(editor, SimpleMDE.toggleBlockquote),
                        className: "fa fa-quote-left",
                        title: "Quote"
                    }, {
                        name: "unordered-list",
                        action: (editor) => actionClick(editor, SimpleMDE.toggleUnorderedList),
                        className: "fa fa-list-ul",
                        title: "Generic List"
                    }, {
                        name: "ordered-list",
                        action: (editor) => actionClick(editor, SimpleMDE.toggleOrderedList),
                        className: "fa fa-list-ol",
                        title: "Numbered List"
                    }, {
                        name: "clean-block",
                        action: (editor) => actionClick(editor, SimpleMDE.cleanBlock),
                        className: "fa fa-eraser fa-clean-block",
                        title: "Clean block"
                    }, '|', {
                        name: "preview",
                        action: (editor) => $timeout(() => scope.$apply(() => {
                            cancelDeactivate();
                            SimpleMDE.togglePreview(editor);
                            if (!editor.isPreviewActive()) {
                                editor.codemirror.focus();
                            }
                        })),
                        className: "fa fa-eye no-disable",
                        title: "Toggle Preview"
                    }, 'guide']
                });
                simplemde.codemirror.on('change', () => {
                    scope.onChange(simplemde.value());
                });
                simplemde.codemirror.on('focus', focusAction);
                simplemde.codemirror.on('blur', () => $timeout(() => {
                    deactivate();
                }));
                let disablePreview = () => {
                    if (simplemde.isPreviewActive()) {
                        simplemde.togglePreview();
                    }
                };
                scope.$on('fieldCanceled', disablePreview);
                scope.$watch('value', () => $timeout(() => {
                    $log.debug(`scope.fieldDirty = ${scope.fieldDirty}; scope.value = ${scope.value}`);
                    if (!scope.fieldDirty) {
                        simplemde.value(scope.value ? scope.value : '');
                    }
                }));
                scope.$on('$destroy', () => {
                    el.remove();
                });
                el.find('> div').on('focus', () => {
                    el.addClass('active');
                    $timeout(() => simplemde.codemirror.focus());
                });
            })
        };
    }]);
