module _components_markdown {

let hljs = require("../../../static/bower_components/highlightjs/highlight.pack.js");
let SimpleMDE = require("../../../static/bower_components/simplemde/dist/simplemde.min.js");
let marked = require("../../../static/bower_components/marked/marked.min.js");

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

    .directive('tribeMarkdownHelp', ['$document', '$timeout', ($document, $timeout) => {
        return {
            restrict: 'A',
            scope: {
                visible: '='
            },
            controller: ['$timeout', '$scope', function($timeout, $scope) {
                $scope.close = function() {
                    $timeout(() => $scope.$apply(() => {
                        $scope['visible'] = false;
                    }));
                }
            }],
            template: require('../templates/component_markdown_help.jade'),
            link: (scope, el, attrs, controller) => {
                let content = el.find('> div > div.markdown-help-content');
                content.detach();
                let body = $document.find('body');
                let keyPress = (event) => {
                    console.log('event.keyCode -> ' + event.keyCode);
                    if (event.keyCode === 27 /* Escape */) {
                        scope['close']();
                    }
                };
                scope.$watch('visible', () => {
                    if (scope['visible']) {
                        body.addClass('noscroll');
                        body.append(content);
                        $document.on('keyup', keyPress);
                    } else {
                        content.detach();
                        body.removeClass('noscroll');
                        $document.off('keyup', keyPress);
                    }
                });
                scope.$on('$destroy', () => {
                    content.remove();
                    el.remove();
                    body.removeClass('noscroll');
                    $document.off('keyup', keyPress);
                });
            }
        };
    }])

    .directive('tribeMarkdown', ['$window', '$timeout', '$log', 'tribeMarkdownService', '$document', ($window, $timeout, $log, mdService, $document) => {
        return {
            restrict: 'A',
            scope: {
                originalValue: '=value',
                placeholder: '@?',
                onEditModeOn: '&?',
                onEditModeOff: '&?'
            },
            template: require('../templates/component_markdown.jade'),
            controller: ['$scope', ($scope) => $timeout(() => {
                $scope['uniqueId'] = _.uniqueId('tribeMarkdown_');
                $scope['helpVisible'] = false;
                $scope['simplemde'] = null;
                $scope['version'] = 0;
                $scope['fieldDirty'] = false;
                $scope['cmFocused'] = false;
                $scope['sidebyside'] = false;
                $scope.$watch('originalValue', () => $timeout(() => $scope.$apply(() => {
                    $scope['value'] = $scope.originalValue ? _.clone($scope.originalValue) : '';
                })));
                $scope.$watch('value', () => $timeout(() => $scope.$apply(() => {
                    $scope.preview = mdService.compileMd($scope['value']);
                })));
                $scope.onCommit = () =>  $timeout(() => $scope.$apply(() => {
                    $scope['cmFocused'] = false;
                    if ($scope['fieldDirty']) {
                        $scope['fieldDirty'] = false;
                        $scope.originalValue = _.clone($scope['value']);
                        $scope.$broadcast('fieldCommited');
                    }
                    if ($scope['onEditModeOff']) {
                        $scope['onEditModeOff']({'uniqueId': $scope['uniqueId']});
                    }
                }));
                $scope['onCancel'] = () =>  $timeout(() => $scope.$apply(() => {
                    $scope['fieldDirty'] = false;
                    $scope['value'] = _.clone($scope.originalValue);
                    $scope.$broadcast('fieldCanceled');
                    if ($scope['onEditModeOff']) {
                        $scope['onEditModeOff']({'uniqueId': $scope['uniqueId']});
                    }
                }));
                $scope['onChange'] = (newValue) =>  $timeout(() => $scope.$apply(() => {
                    $scope['version'] = $scope['version'] + 1;
                    $scope['value'] = newValue;
                    if ($scope.originalValue !== newValue) {
                        $scope['fieldDirty'] = true;
                    }
                }));
            })],
            link: (scope, el) => $timeout(() => {
                let body = $document.find('body');
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
                        scope['onCommit']();
                        scope.$apply(() => {
                            scope['sidebyside'] = false;
                            scope['fullscreen'] = false;
                        });
                        el.removeClass('active');
                        if(simplemde) {
                            angular.element(simplemde.toolbarElements["side-by-side"]).removeClass('active');
                            angular.element(simplemde.toolbarElements["fullscreen"]).removeClass('active');
                            if (simplemde.isPreviewActive()) {
                                SimpleMDE.togglePreview(simplemde);
                            }
                        }
                    }, 500);
                };
                let focusAction = (cm) => {
                    $log.debug('codemirror focus; cm.getSelection() empty? ' + !!cm.getSelection());
                    cancelDeactivate();
                    if (!scope['cmFocused']) {
                        cm.execCommand('selectAll');
                    }
                    $timeout(() => scope.$apply(() => {
                        scope['cmFocused'] = true;
                        scope['version'] = scope['version'] + 1;
                        scope['fieldDirty'] = true;
                        if (scope['onEditModeOn']) {
                            scope['onEditModeOn']({'uniqueId': scope['uniqueId']});
                        }
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
                    autoDownloadFontAwesome: false,
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
                        // replace by this action block if this PR gets merged.
                        // https://github.com/NextStepWebs/simplemde-markdown-editor/pull/463
                        // action: (editor) => actionClick(editor, SimpleMDE.cleanBlock),
                        action: (editor) => $timeout(() => scope.$apply(() => {
                            cancelDeactivate();
                            var cm = editor.codemirror;
                            // split the selection in lines
                            var selections = cm.getSelection().split("\n");
                            var removeTags = function (selection) {
                                var html = marked(selection);
                                // create a div...
                                var tmp = document.createElement("DIV");
                                // .. with the new generated html code...
                                tmp.innerHTML = html;
                                // ... now read the text of the generated code.
                                // This way the browser does the job of removing the tags.
                                var result = selection;
                                if(tmp.textContent) {
                                    result = tmp.textContent;
                                } else if (tmp.innerText) {
                                    result = tmp.innerText;
                                }
                                // removing trailing "new line"
                                return result.split("\n").join('');
                            };
                            var result = [];
                            for(var i = 0; i < selections.length; i++) {
                                result.push(removeTags(selections[i]));
                            }
                            // Add removed "new lines" back to the resulting string.
                            // Replace the selection with the new clean selection.
                            cm.replaceSelection(result.join("\n"));
                        })),
                        className: "fa fa-eraser fa-clean-block",
                        title: "Clean block"
                    }, '|', {
                        name: "side-by-side",
                        action: (editor) => $timeout(() => scope.$apply(() => {
                            cancelDeactivate();
                            scope['sidebyside'] = !scope['sidebyside'];
                            let btn = angular.element(editor.toolbarElements["side-by-side"]);
                            if(scope['sidebyside']) {
                                btn.addClass('active');
                            } else {
                                btn.removeClass('active');
                            }
                            if(editor.isPreviewActive()) {
                                SimpleMDE.togglePreview(editor);
                            }
                            editor.codemirror.focus();
                        })),
                        className: "fa fa-columns no-disable no-mobile",
                        title: "Toggle Side by Side"
                    }, {
                        name: "preview",
                        action: (editor) => $timeout(() => scope.$apply(() => {
                            cancelDeactivate();
                            scope['sidebyside'] = false;
                            angular.element(editor.toolbarElements["side-by-side"]).removeClass('active');
                            SimpleMDE.togglePreview(editor);
                            if (!editor.isPreviewActive()) {
                                editor.codemirror.focus();
                            }
                        })),
                        className: "fa fa-eye no-disable",
                        title: "Toggle Preview"
                    }, {
                        name: "fullscreen",
                        action: (editor) => $timeout(() => scope.$apply(() => {
                            cancelDeactivate();
                            scope['fullscreen'] = !scope['fullscreen'];
                            let btn = angular.element(editor.toolbarElements["fullscreen"]);
                            if(scope['fullscreen']) {
                                btn.addClass('active');
                                body.addClass('noscroll')
                            } else {
                                btn.removeClass('active');
                                body.removeClass('noscroll');
                            }
                        })),
                        className: "fa fa-arrows-alt no-disable no-mobile",
                        title: "Toggle Fullscreen"
                    }, {
                        name: "guide",
                        action: (editor) => $timeout(() => scope.$apply(() => {
                            scope['helpVisible'] = true;
                        })),
                        className: "fa fa-question-circle",
                        title: "Markdown Guide"
                    }]
                });
                simplemde.codemirror.on('change', () => {
                    scope['onChange'](simplemde.value());
                });
                simplemde.codemirror.on('focus', focusAction);
                simplemde.codemirror.on('blur', () => $timeout(() => {
                    deactivate();
                }));
                let disablePreview = () => {
                    $timeout(() => scope.$apply(() => {
                        scope['sidebyside'] = false;
                        scope['fullscreen'] = false;
                    }));
                    el.removeClass('active');
                    angular.element(simplemde.toolbarElements["side-by-side"]).removeClass('active');
                    angular.element(simplemde.toolbarElements["fullscreen"]).removeClass('active');
                    if (simplemde.isPreviewActive()) {
                        simplemde.togglePreview();
                    }
                };
                scope.$on('fieldCanceled', disablePreview);
                scope.$watch('value', () => $timeout(() => {
                    $log.debug(`scope['fieldDirty'] = ${scope['fieldDirty']}; scope.value = ${scope['value']}`);
                    if (!scope['fieldDirty']) {
                        simplemde.value(scope['value'] ? scope['value'] : '');
                    }
                }));
                scope.$on('$destroy', () => {
                    el.remove();
                    body.removeClass('noscroll');
                });
                el.find('> div').on('focus', () => {
                    el.addClass('active');
                    $timeout(() => simplemde.codemirror.focus());
                });
            })
        };
    }]);

}
