angular.module('website-components-multiselect', [
    'website-components-field-actions'
])

    .directive('tribeMultiselect', ['$window', '$timeout', ($window, $timeout) => {
        return {
            restrict: 'A',
            scope: {
                originalAvailableOptions: '=availableOptions',
                originalSelectedOptions: '=selectedOptions',
                originalGetOptionText: '=getOptionText',
                newLabel: '@?'
            },
            templateUrl: 'app/templates/component_multiselect.html',
            controller: ['$log', '$scope', '$timeout', ($log, $scope, $timeout) => $timeout(() => {
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
                $scope.$watch('originalSelectedOptions', () => $timeout(() => $scope.$apply(() => {
                    if(!$scope.originalSelectedOptions) {
                        $scope.selectedOptions = [];
                    } else {
                        $scope.selectedOptions = _.clone($scope.originalSelectedOptions);
                    }

                })));
                $scope.$watch('originalAvailableOptions', () => {
                    $scope.availableOptions = _.clone($scope.originalAvailableOptions);
                });
                $scope.fieldDirty = false;
                $scope.optionsActivated = false;
                $scope.optionsActivatedTopDown = 0;
                $scope.optionsActivatedBottomUp = 0;
                $scope.version = 0;
                $scope.selectedOption = null;
                $scope.inputText = '';
                $scope.fieldChanged = () => $timeout(() => $scope.$apply(() => {
                    $scope.fieldDirty = true;
                    $scope.version = $scope.version + 1;
                }));
                $scope.fieldCommitted = () => $timeout(() => $scope.$apply(() => {
                    $scope.onCommit();
                    $scope.$broadcast('fieldCommitted');
                }));
                $scope.onCommit = () => $timeout(() => $scope.$apply(() => {
                    if ($scope.fieldDirty) {
                        $scope.fieldDirty = false;
                        $scope.optionsActivated = false;
                        $scope.originalSelectedOptions = _.clone($scope.selectedOptions);
                        $log.debug('field committed. values: ' + $scope.selectedOptions);
                    }
                }));
                $scope.fieldCanceled = () => {
                    $scope.fieldDirty = false;
                    $scope.selectedOptions = _.clone($scope.originalSelectedOptions);
                    $scope.$broadcast('fieldCanceled');
                };
                $scope.onSelectTopDownOption = () => $timeout(() => $scope.$apply(() => {
                    $scope.optionsActivatedTopDown = $scope.optionsActivatedTopDown + 1;
                    $scope.optionsActivated = true;
                }));
                $scope.onSelectBottomUpOption = () => $timeout(() => $scope.$apply(() => {
                    $scope.optionsActivatedBottomUp = $scope.optionsActivatedBottomUp + 1;
                    $scope.optionsActivated = true;
                }));
                $scope.onOptionsDeactivated = () => $timeout(() => $scope.$apply(() => {
                    $scope.optionsActivated = false;
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
                        scope.onCommit();
                        el.removeClass('active');
                    }, 500);
                };
                el.find('> div').on('focus', () => el.find('input').focus());
                el.find('input').on('focus', () => {
                    cancelDeactivate();
                    el.addClass('active');
                    $timeout(() => scope.$apply(() => scope.fieldDirty = true));
                });
                el.find('input').on('blur', deactivate);
                scope.$on('fieldDirty', () => {
                    if (scope.fieldDirty) {
                        cancelDeactivate();
                        el.addClass('active');
                    }
                });
                scope.$on('$destroy', () => el.remove());
                scope.$on('fieldCanceled', () => $timeout(() => el.find('input').blur()));
                scope.$on('fieldCommitted', () => $timeout(() => el.find('input').blur()));
            })
        };
    }])

    .directive('tribeMultiselectAvailable', ['$document', '$window', '$timeout', ($document, $window, $timeout) => {
        return {
            restrict: 'A',
            scope: {
                selectedOptions: '=',
                originalAvailableOptions: '=availableOptions',
                active: '=',
                activeTopDown: '=',
                activeBottomUp: '=',
                onSelect: '&',
                inputText: '=',
                selectedItem: '=selectedOption',
                newLabel: '@?',
                getOptionText: '='
            },
            templateUrl: 'app/templates/component_multiselect_available.html',
            controller: ['$scope', '$timeout', ($scope, $timeout) => {
                $scope.availableOptions = [];
                $scope.showOptions = () => $timeout(() => $scope.$apply(() => {
                    $scope.selectedItem = null;
                    $scope.newOpt = null;
                    $scope.availableOptions = _.clone($scope.originalAvailableOptions);
                    for (let opt of $scope.selectedOptions) {
                        $scope.availableOptions = _.without($scope.availableOptions, opt);
                    }
                    if ($scope.inputText.trim()) {
                        $scope.availableOptions = _.filter($scope.availableOptions, (opt) => {
                            return opt.startsWith($scope.inputText);
                        });
                        $scope.selectedItem = _.find($scope.availableOptions, (opt) => opt.startsWith($scope.inputText.trim()));
                        if (_.find($scope.availableOptions, (opt) => opt === $scope.inputText.trim())) {
                            $scope.newOpt = null;
                        } else {
                            $scope.newOpt = $scope.inputText.trim();
                        }
                        if (!$scope.selectedItem) {
                            $scope.selectedItem = $scope.newOpt;
                        }
                    }
                }));
                $scope.selectedItem = null;
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
                $scope.selectItem = (opt) => $timeout(() => $scope.$apply(() => {
                    $scope.selectedOptions.push(opt);
                    $scope.active = false;
                    $scope.inputText = '';
                }));
                $scope.$watch('inputText', () => {
                    $timeout(() => $scope.$apply(() => {
                        if (!$scope.inputText) {
                            $scope.selectedItem = null;
                            $scope.active = false;
                        } else {
                            $scope.selectedItem = _.find($scope.availableOptions, (opt) => opt.startsWith($scope.inputText));
                            $scope.active = true;
                            $scope.showOptions();
                        }
                    }));
                });
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
                        $timeout(() => scope.$apply(() => {
                            scope.selectedItem = null;
                        }));
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
    }])

    .directive('tribeMultiselectSelected', [() => {
        return {
            restrict: 'A',
            scope: {
                selectedOptions: '=',
                onChange: '&',
                onCommit: '&',
                onCancel: '&',
                onSelectTopDownOption: '&',
                onSelectBottomUpOption: '&',
                onOptionsDeactivated: '&',
                selectedOption: '=',
                inputText: '='
            },
            templateUrl: 'app/templates/component_multiselect_selected.html',
            controller: ['$log', '$scope', '$timeout', ($log, $scope, $timeout) => {
                $scope.inputText = '';
                $scope.releaseEngaged = false;
                $scope.selectedItem = null;
                let selectOrDeleteLast = () => $timeout(() => $scope.$apply(() => {
                    let ordered = _.sortBy($scope.selectedOptions, (item) => item);
                    if ($scope.selectedItem) {
                        var selectedIndex = ordered.indexOf($scope.selectedItem);
                        $scope.selectedOptions = _.without($scope.selectedOptions, $scope.selectedItem);
                        ordered = _.without(ordered, $scope.selectedItem);
                        if (ordered.length) {
                            if (selectedIndex >= ordered.length) {
                                selectedIndex = ordered.length - 1;
                            }
                            $scope.selectedItem = ordered[selectedIndex];
                        } else {
                            $scope.selectedItem = null;
                        }
                        $scope.onChange();
                    } else {
                        $scope.selectedItem = _.last(ordered);
                    }
                }));
                let releaseSelection = () => $timeout(() => $scope.$apply(() => $scope.selectedItem = null));
                let selectLeft = () => $timeout(() => $scope.$apply(() => {
                    if (!$scope.selectedOptions.length) {
                        return;
                    }
                    if (!$scope.selectedItem) {
                        selectOrDeleteLast();
                    } else {
                        let ordered = _.sortBy($scope.selectedOptions, (item) => item);
                        var next = ordered.indexOf($scope.selectedItem) - 1;
                        if (next === -1) {
                            next = ordered.length - 1;
                        }
                        $scope.selectedItem = ordered[next];
                    }
                }));
                let selectRight = () => $timeout(() => $scope.$apply(() => {
                    if (!$scope.selectedOptions.length) {
                        return;
                    }
                    let ordered = _.sortBy($scope.selectedOptions, (item) => item);
                    if (!$scope.selectedItem) {
                        $scope.selectedItem = ordered[0];
                    } else {
                        var next = ordered.indexOf($scope.selectedItem) + 1;
                        if (next === ordered.length) {
                            next = 0;
                        }
                        $scope.selectedItem = ordered[next];
                    }
                }));
                let addItem = () => {
                    if ($scope.selectedOption) {
                        let existing = _.find($scope.selectedOptions, (selected) => selected === $scope.selectedOption);
                        if (!existing) {
                            $scope.selectedOptions.push($scope.selectedOption);
                        }
                    }
                    $scope.selectedOption = null;
                    $scope.inputText = '';
                };
                $scope.keyEntered = (event) =>  $timeout(() => $scope.$apply(() => {
                    if (event.keyCode === 13 /* Enter */) {
                        let isCommitChanges = !$scope.inputText && !$scope.selectedOption;
                        $log.debug('enter key detected. commit values? ' + isCommitChanges);
                        if (isCommitChanges) {
                            $scope.onOptionsDeactivated();
                            $scope.releaseEngaged = false;
                            $scope.selectedItem = null;
                            $scope.onCommit();
                        } else {
                            addItem();
                            releaseSelection();
                            $scope.onOptionsDeactivated();
                            $scope.onChange();
                        }
                    } else if (event.keyCode === 27 /* Escape */) {
                        let isCancelChanges = !$scope.inputText;
                        $scope.inputText = '';
                        releaseSelection();
                        $scope.onOptionsDeactivated();
                        if (isCancelChanges) {
                            $scope.onCancel();
                        }
                    } else if (event.keyCode === 8 /* Backspace */) {
                        if (!$scope.inputText) {
                            selectOrDeleteLast();
                        }
                    } else if (event.keyCode === 37 /* ArrowLeft */ && !$scope.inputText) {
                        selectLeft();
                    } else if (event.keyCode === 39 /* ArrowRight */ && !$scope.inputText) {
                        selectRight();
                    } else if (event.keyCode === 40 /* ArrowDown */) {
                        $scope.onSelectTopDownOption();
                    } else if (event.keyCode === 38 /* ArrowUp */) {
                        $scope.onSelectBottomUpOption();
                    } else {
                        releaseSelection();
                    }
                }));
                $scope.releaseItem = (item) => {
                    $scope.selectedOptions = _.without($scope.selectedOptions, item);
                    $scope.onChange();
                };
                $scope.$on('fieldCanceled', () => $timeout(() => $scope.$apply(() => {
                    $scope.inputText = '';
                    $scope.releaseEngaged = false;
                    $scope.selectedItem = null;
                })));
            }]
        };
    }]);
