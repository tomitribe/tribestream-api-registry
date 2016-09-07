///<reference path="../../bower_components/DefinitelyTyped/angularjs/angular.d.ts"/>
///<reference path="../../bower_components/DefinitelyTyped/underscore/underscore.d.ts"/>

module basecomponents {

    angular.module('website-components-multiselect', [
        'website-components-field-actions'
    ])

        .directive('tribeMultiselect', ['$window', '$timeout', ($window, $timeout) => {
            return {
                restrict: 'A',
                scope: {
                    originalAvailableOptions: '=availableOptions',
                    originalSelectedOptions: '=selectedOptions',
                    newLabel: '@?'
                },
                templateUrl: 'app/templates/component_multiselect.html',
                controller: ['$scope', '$timeout', ($scope, $timeout) => $timeout(() => {
                    $scope.$watch('originalSelectedOptions', () => {
                        $scope.selectedOptions = _.clone($scope.originalSelectedOptions);
                    });
                    $scope.$watch('originalAvailableOptions', () => {
                        $scope.availableOptions = _.clone($scope.originalAvailableOptions);
                    });
                    $scope.fieldDirty = false;
                    $scope.optionsActivated = false;
                    $scope.optionsActivatedTopDown = 0;
                    $scope.optionsActivatedBottomUp = 0;
                    $scope.version = 0;
                    $scope.inputText = '';
                    $scope.fieldChanged = () => $timeout(() => $scope.$apply(() => {
                        $scope.fieldDirty = true;
                        $scope.version = $scope.version + 1;
                    }));
                    $scope.fieldCommitted = () => $timeout(() => $scope.$apply(() => {
                        $scope.$broadcast('fieldCommitted');
                    }));
                    $scope.onCommit = () => $timeout(() => $scope.$apply(() => {
                        $scope.fieldDirty = false;
                        $scope.optionsActivated = false;
                        $scope.originalSelectedOptions = _.clone($scope.selectedOptions);
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
                            scope.fieldCommitted();
                            el.removeClass('active');
                        }, 500);
                    };
                    el.find('> div').on('focus', () => el.find('input').focus());
                    el.find('input').on('focus', () => {
                        cancelDeactivate();
                        el.addClass('active');
                    });
                    el.find('input').on('blur', deactivate);
                    scope.$on('fieldDirty', () => {
                        if (scope.fieldDirty) {
                            cancelDeactivate();
                            el.addClass('active');
                        }
                    });
                    scope.$on('$destroy', () => el.remove());
                })
            };
        }])

        .directive('tribeMultiselectAvailable', ['$document', '$window', ($document, $window) => {
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
                    newLabel: '@?'
                },
                templateUrl: 'app/templates/component_multiselect_available.html',
                controller: ['$scope', '$timeout', ($scope, $timeout) => {
                    $scope.availableOptions = [];
                    $scope.showOptions = () => $timeout(() => $scope.$apply(() => {
                        $scope.selectedItem = null;
                        $scope.availableOptions = _.clone($scope.originalAvailableOptions);
                        for (let opt of $scope.selectedOptions) {
                            $scope.availableOptions = _.without($scope.availableOptions, opt);
                        }
                    }));
                    $scope.selectedItem = null;
                    let setText = () => {
                        if ($scope.selectedItem) {
                            if ($scope.selectedItem.toString) {
                                $scope.inputText = $scope.selectedItem.toString();
                            } else {
                                $scope.inputText = $scope.selectedItem;
                            }
                        }
                    };
                    $scope.selectNext = () => $timeout(() => $scope.$apply(() => {
                        if ($scope.selectedItem) {
                            var index = $scope.availableOptions.indexOf($scope.selectedItem) + 1;
                            if (index >= $scope.availableOptions.length) {
                                index = 0;
                            }
                            $scope.selectedItem = $scope.availableOptions[index];
                        } else {
                            $scope.selectedItem = _.first($scope.availableOptions);
                        }
                        setText();
                    }));
                    $scope.selectPrevious = () => $timeout(() => $scope.$apply(() => {
                        if ($scope.selectedItem) {
                            var index = $scope.availableOptions.indexOf($scope.selectedItem) - 1;
                            if (index < 0) {
                                index = $scope.availableOptions.length - 1;
                            }
                            $scope.selectedItem = $scope.availableOptions[index];
                        } else {
                            $scope.selectedItem = _.last($scope.availableOptions);
                        }
                        setText();
                    }));
                    $scope.selectItem = (opt) => {
                        $scope.selectedOptions.push(opt)
                        $scope.active = false;
                        $scope.inputText = '';
                    };
                    $scope.$watch('inputText', () => {
                        if (!$scope.inputText) {
                            return;
                        }
                        $scope.selectedItem = _.find($scope.availableOptions, (opt) => opt === $scope.inputText);
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
                    onSelectTopDownOption: '&',
                    onSelectBottomUpOption: '&',
                    onOptionsDeactivated: '&',
                    inputText: '='
                },
                templateUrl: 'app/templates/component_multiselect_selected.html',
                controller: ['$scope', '$timeout', ($scope, $timeout) => {
                    $scope.inputText = '';
                    $scope.releaseEngaged = false;
                    $scope.selectedItem = null;
                    let selectOrDeleteLast = () => $timeout(() => $scope.$apply(() => {
                        if ($scope.selectedItem) {
                            var selectedIndex = $scope.selectedOptions.indexOf($scope.selectedItem);
                            $scope.selectedOptions = _.without($scope.selectedOptions, $scope.selectedItem);
                            if ($scope.selectedOptions.length) {
                                if (selectedIndex >= $scope.selectedOptions.length) {
                                    selectedIndex = $scope.selectedOptions.length - 1;
                                }
                                $scope.selectedItem = $scope.selectedOptions[selectedIndex];
                            } else {
                                $scope.selectedItem = null;
                            }
                            $scope.onChange();
                        } else {
                            $scope.selectedItem = _.last($scope.selectedOptions);
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
                            var next = $scope.selectedOptions.indexOf($scope.selectedItem) - 1;
                            if (next === -1) {
                                next = $scope.selectedOptions.length - 1;
                            }
                            $scope.selectedItem = $scope.selectedOptions[next];
                        }
                    }));
                    let selectRight = () => $timeout(() => $scope.$apply(() => {
                        if (!$scope.selectedOptions.length) {
                            return;
                        }
                        if (!$scope.selectedItem) {
                            $scope.selectedItem = $scope.selectedOptions[0];
                        } else {
                            var next = $scope.selectedOptions.indexOf($scope.selectedItem) + 1;
                            if (next === $scope.selectedOptions.length) {
                                next = 0;
                            }
                            $scope.selectedItem = $scope.selectedOptions[next];
                        }
                    }));
                    let addItem = () => {
                        let trimmed = $scope.inputText.trim();
                        if (trimmed) {
                            let existing = _.find($scope.selectedOptions, (selected) => selected === trimmed);
                            if (!existing) {
                                $scope.selectedOptions.push(trimmed);
                            }
                        }
                        $scope.inputText = '';
                    };
                    $scope.keyEntered = (event) =>  $timeout(() => $scope.$apply(() => {
                        if (event.keyCode === 13 /* Enter */) {
                            addItem();
                            releaseSelection();
                            $scope.onOptionsDeactivated();
                            $scope.onChange();
                        } else if (event.keyCode === 27 /* Escape */) {
                            $scope.inputText = '';
                            releaseSelection();
                            $scope.onOptionsDeactivated();
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
                    $scope.$on('fieldCommitted', () => {
                        $timeout(() => $scope.$apply(() => {
                            addItem();
                            $scope.releaseEngaged = false;
                            $scope.selectedItem = null;
                        }));
                        $scope.onCommit();
                    });
                    $scope.$on('fieldCanceled', () => $timeout(() => $scope.$apply(() => {
                        $scope.inputText = '';
                        $scope.releaseEngaged = false;
                        $scope.selectedItem = null;
                    })));
                }]
            };
        }]);
}

