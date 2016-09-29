angular.module('website-components-singleselect', [
    'website-components-field-actions'
])

    .directive('tribeSingleselect', ['$window', '$timeout', ($window, $timeout) => {
        return {
            restrict: 'A',
            scope: {
                editable: '@?',
                originalAvailableOptions: '=availableOptions',
                originalSelectedOption: '=selectedOption',
                originalGetOptionText: '=getOptionText',
                newLabel: '@?'
            },
            templateUrl: 'app/templates/component_singleselect.html',
            controller: ['$scope', '$timeout', ($scope, $timeout) => $timeout(() => {
                $scope.$watch('originalGetOptionText', () => {
                    if ($scope.originalGetOptionText) {
                        $scope.getOptionText = $scope.originalGetOptionText;
                    } else {
                        $scope.getOptionText = (item) => {
                            if (!item || item.text === undefined) {
                                return item;
                            }
                            return item.text;
                        };
                    }
                });
                $scope.getOptionValue = (item) => {
                    if (!item || item.value === undefined) {
                        return item;
                    }
                    return item.value;
                };
                if ($scope.editable === undefined) {
                    $scope.editable = false;
                } else {
                    $scope.editable = $scope.editable === 'true';
                }
                $scope.selectedItem = null;
                $scope.inputText = null;
                $scope.fieldDirty = false;
                $scope.optionsActivated = false;
                $scope.optionsActivatedTopDown = 0;
                $scope.optionsActivatedBottomUp = 0;
                $scope.version = 0;
                $scope.$watch('originalAvailableOptions', () => {
                    $scope.availableOptions = _.clone($scope.originalAvailableOptions);
                    $scope.$watch('originalSelectedOption', () => {
                        let existing = _.find($scope.availableOptions, (item) => {
                            let availValue = $scope.getOptionValue(item);
                            return availValue === $scope.originalSelectedOption;
                        });
                        if (existing) {
                            $scope.inputText = $scope.getOptionText(_.clone(existing));
                            $scope.selectedItem = _.clone(existing);
                        } else {
                            $scope.inputText = _.clone($scope.originalSelectedOption);
                            $scope.selectedItem = _.clone($scope.originalSelectedOption);
                        }
                    });
                });
                $scope.onChange = () => $timeout(() => $scope.$apply(() => {
                    $scope.optionsActivated = true;
                    $scope.fieldDirty = true;
                    $scope.version = $scope.version + 1;
                }));
                $scope.onCancel = () => $timeout(() => $scope.$apply(() => {
                    $scope.fieldDirty = false;
                    $scope.optionsActivated = false;
                    let existing = _.find($scope.availableOptions, (item) => {
                        let availValue = $scope.getOptionValue(item);
                        return availValue === $scope.originalSelectedOption;
                    });
                    $scope.inputText = $scope.getOptionText(existing);
                    $scope.selectedItem = _.clone(existing);
                    $scope.$broadcast('fieldCanceled');
                }));
                $scope.onCommit = (revertIfNoMatch) => $timeout(() => $scope.$apply(() => {
                    if ($scope.selectedItem) {
                        $scope.fieldDirty = false;
                        $scope.optionsActivated = false;
                        $scope.originalSelectedOption = $scope.getOptionValue($scope.selectedItem);
                        $scope.inputText = $scope.getOptionText($scope.selectedItem);
                        $scope.selectedItem = _.clone($scope.originalSelectedOption);
                        $scope.$broadcast('fieldCommitted');
                    } else if(revertIfNoMatch) {
                        $scope.fieldDirty = false;
                        $scope.optionsActivated = false;
                        let existing = _.find($scope.availableOptions, (item) => {
                            let availValue = $scope.getOptionValue(item);
                            return availValue === $scope.originalSelectedOption;
                        });
                        $scope.inputText = $scope.getOptionText(existing);
                        $scope.selectedItem = _.clone(existing);
                    }
                }));
                $scope.onSelectTopDownOption = () => $timeout(() => $scope.$apply(() => {
                    $scope.optionsActivatedTopDown = $scope.optionsActivatedTopDown + 1;
                    $scope.optionsActivated = true;
                }));
                $scope.onSelectBottomUpOption = () => $timeout(() => $scope.$apply(() => {
                    $scope.optionsActivatedBottomUp = $scope.optionsActivatedBottomUp + 1;
                    $scope.optionsActivated = true;
                }));
            })],
            link: (scope, el) => $timeout(() => {
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
                        el.removeClass('active');
                        if (scope.fieldDirty) {
                            scope.onCommit(true);
                        }
                    }, 500);
                };
                el.find('> div').on('focus', () => el.find('input').focus());
                el.find('input').on('focus', () => {
                    cancelDeactivate();
                    el.addClass('active');
                    $timeout(() => scope.$apply(() => {
                        scope.fieldDirty = true;
                        scope.version = scope.version + 1;
                    }));
                });
                scope.$on('fieldCanceled', () => el.find('input').blur());
                scope.$on('fieldCommitted', () => el.find('input').blur());
                el.find('input').on('blur', deactivate);
                scope.$on('fieldDirty', () => {
                    if (scope.fieldDirty) {
                        cancelDeactivate();
                        el.addClass('active');
                    }
                });
                scope.$watch('inputText', () => {
                    if (el.hasClass('active')) {
                        scope.onChange();
                    }
                });
                scope.$on('$destroy', () => el.remove());
            })
        };
    }])

    .directive('tribeSingleselectAvailable', ['$document', '$window', ($document, $window) => {
        return {
            restrict: 'A',
            scope: {
                originalAvailableOptions: '=availableOptions',
                active: '=',
                activeTopDown: '=',
                activeBottomUp: '=',
                onSelect: '&',
                inputText: '=',
                selectedItem: '=',
                version: '=',
                newLabel: '@?',
                editable: '=',
                getOptionText: '='
            },
            templateUrl: 'app/templates/component_singleselect_available.html',
            controller: ['$scope', '$timeout', ($scope, $timeout) => {
                $scope.showOptions = () => $timeout(() => $scope.$apply(() => {
                    $scope.selectedItem = null;
                    $scope.newOpt = null;
                    $scope.availableOptions = _.clone($scope.originalAvailableOptions);
                    let text = $scope.inputText ? $scope.inputText.trim() : '';
                    $scope.availableOptions = _.sortBy(_.filter($scope.availableOptions, (opt) => {
                        return $scope.getOptionText(opt).startsWith(text);
                    }), (item) => $scope.getOptionText(item));
                    $scope.selectedItem = _.find($scope.availableOptions, (opt) => $scope.getOptionText(opt).startsWith(text));
                    if ($scope.editable) {
                        if (_.find($scope.availableOptions, (opt) => $scope.getOptionText(opt) === text)) {
                            $scope.newOpt = null;
                        } else {
                            $scope.newOpt = text;
                        }
                        if (!$scope.selectedItem) {
                            $scope.selectedItem = $scope.newOpt;
                        }
                    }
                }));
                $scope.selectAvailableItem = (item) => $timeout(() => $scope.$apply(() => {
                    $scope.selectedItem = item;
                }));
                $scope.selectNext = () => $timeout(() => $scope.$apply(() => {
                    let ordered = _.sortBy($scope.availableOptions, (item) => $scope.getOptionText(item).toLowerCase());
                    if ($scope.selectedItem) {
                        var index = ordered.indexOf($scope.selectedItem) + 1;
                        if (index >= ordered.length) {
                            if ($scope.newOpt) {
                                $scope.selectedItem = $scope.newOpt;
                            } else {
                                $scope.selectedItem = ordered[0];
                            }
                        } else {
                            $scope.selectedItem = ordered[index];
                        }
                    } else {
                        $scope.selectedItem = _.first(ordered);
                    }
                }));
                $scope.selectPrevious = () => $timeout(() => $scope.$apply(() => {
                    let ordered = _.sortBy($scope.availableOptions, (item) => $scope.getOptionText(item).toLowerCase());
                    if ($scope.selectedItem) {
                        if ($scope.newOpt === $scope.selectedItem && ordered.length) {
                            $scope.selectedItem = _.last(ordered);
                        } else {
                            var index = ordered.indexOf($scope.selectedItem) - 1;
                            if (index < 0) {
                                if ($scope.newOpt) {
                                    $scope.selectedItem = $scope.newOpt;
                                } else {
                                    $scope.selectedItem = _.last(ordered);
                                }
                            } else {
                                $scope.selectedItem = ordered[index];
                            }
                        }
                    } else {
                        if ($scope.newOpt) {
                            $scope.selectedItem = $scope.newOpt;
                        } else {
                            $scope.selectedItem = _.last(ordered);
                        }
                    }
                }));
                $scope.selectItem = (opt) => {
                    $scope.selectedOption = opt;
                    $scope.active = false;
                    $scope.inputText = $scope.getOptionText(opt);
                };
            }],
            link: (scope, element) => {
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
                        scope.showOptions();
                        element.addClass('active');
                    } else {
                        floatingBody.detach();
                        element.removeClass('active');
                    }
                });
                scope.$watch('activeTopDown', () => {
                    if (scope.activeTopDown) {
                        scope.selectNext();
                    }
                });
                scope.$watch('activeBottomUp', () => {
                    if (scope.activeBottomUp) {
                        scope.selectPrevious();
                    }
                });
                scope.$watch('version', () => {
                    adjustOffset();
                    if (scope.active) {
                        scope.showOptions();
                        element.addClass('active');
                    }
                });
                var eWin = angular.element($window);
                eWin.bind('resize', adjustOffset);
                scope.$on('$destroy', () => {
                    eWin.unbind('resize', adjustOffset);
                    floatingBody.remove();
                    element.remove();
                });
            }
        };
    }])

    .directive('tribeSingleselectSelected', [() => {
        return {
            restrict: 'A',
            scope: {
                onChange: '&',
                onCancel: '&',
                onCommit: '&',
                onSelectTopDownOption: '&',
                onSelectBottomUpOption: '&',
                inputText: '='
            },
            templateUrl: 'app/templates/component_singleselect_selected.html',
            controller: ['$scope', '$timeout', ($scope, $timeout) => {
                $scope.keyEntered = (event) =>  $timeout(() => $scope.$apply(() => {
                    if (event.keyCode === 27 /* Escape */) {
                        $scope.onCancel();
                    } else if (event.keyCode === 40 /* ArrowDown */) {
                        $scope.onSelectTopDownOption();
                    } else if (event.keyCode === 38 /* ArrowUp */) {
                        $scope.onSelectBottomUpOption();
                    } else if (event.keyCode === 13 /* Enter */) {
                        $scope.onCommit();
                    } else if (event.keyCode === 9) {
                        // this is a tab key. no-op for now.
                    }
                }));
            }]
        };
    }]);
