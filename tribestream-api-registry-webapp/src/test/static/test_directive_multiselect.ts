describe('it tests our custom multiselect component', () => {
    let expect = chai.expect;

    var compile;
    var rootScope;
    var timeout;
    var document;

    beforeEach(() => angular.module('website-components-multiselect'));
    beforeEach((done) => {
        angular.injector(['ng', 'website-components-multiselect']).invoke([
            '$compile', '$rootScope', '$timeout', '$document',
            function ($compile, $rootScope, $timeout, $document) {
                compile = $compile;
                rootScope = $rootScope;
                timeout = $timeout;
                document = $document;
            }]);
        done();
    });

    // it will destroy the scope, which will destroy all its elements.
    afterEach(() => rootScope.$destroy());

    let timeoutTryCatch = (ms, done, callback) => timeout(() => {
        try {
            callback();
        } catch (e) {
            done(e);
        }
    }, ms);

    let triggerKeyDown = function (element, keyCode) {
        var e = $.Event("keyup");
        e.keyCode = keyCode;
        element.trigger(e);
    };

    it('should load selected options', (done) => {
        let scope = rootScope.$new();
        scope.selected = ['aaa', 'bbb'];
        scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
        let element = angular.element('<div data-tribe-multiselect data-selected-options="selected" data-available-options="options"></div>');
        compile(element)(scope);
        // append to body so we can click on it.
        element.appendTo(document.find('body'));
        timeoutTryCatch(100, done, () => {
            let selectedItems = angular.element(element.find('div[data-tribe-multiselect-selected] .items'));
            timeoutTryCatch(100, done, () => {
                let values = _.map(selectedItems, (item) => angular.element(item).scope().opt);
                expect(values).to.deep.equal(['aaa', 'bbb']);
                expect(angular.element(element.find('div[data-tribe-multiselect-selected] .items i.remove')).css('display')).to.equal('none');
                done();
            });
        });
    });

    it('should show action buttons on input focus', (done) => {
        let scope = rootScope.$new();
        scope.selected = ['aaa'];
        scope.options = [];
        let element = angular.element('<div data-tribe-multiselect data-selected-options="selected" data-available-options="options"></div>');
        compile(element)(scope);
        // append to body so we can click on it.
        element.appendTo(document.find('body'));
        timeoutTryCatch(100, done, () => {
            expect(document.find('div.tribe-field-actions-body').length).to.equal(0);
            let componentWrapper = angular.element(element.find('> div'));
            // it should trigger the input focus event
            componentWrapper.focus();
            timeoutTryCatch(100, done, () => {
                let input = angular.element(element.find('input'));
                expect(input.is(":focus")).to.equal(true);
                timeoutTryCatch(100, done, () => {
                    expect(document.find('div.tribe-field-actions-body').length).to.equal(1);
                    done();
                });
            });
        });
    });

    it('should show available options', (done) => {
        timeoutTryCatch(100, done, () => {
            let scope = rootScope.$new();
            scope.selected = ['aaa', 'bbb'];
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-multiselect data-selected-options="selected" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeoutTryCatch(100, done, () => {
                let input = angular.element(element.find('input'));
                timeoutTryCatch(100, done, () => {
                    input.focus();
                    timeoutTryCatch(100, done, () => {
                        triggerKeyDown(input, 40);
                        timeoutTryCatch(100, done, () => {
                            let available = element.find('div[data-tribe-multiselect-available]');
                            // the list of items is visible
                            expect(available.hasClass('active')).to.equal(true);
                            done();
                        });
                    });
                });
            });
        });
    });

    it('should select selected items with the left-right keys', (done) => {
        timeoutTryCatch(100, done, () => {
            let scope = rootScope.$new();
            scope.selected = ['aaa', 'bbb', 'ccc'];
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-multiselect data-selected-options="selected" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeoutTryCatch(100, done, () => {
                let input = angular.element(element.find('input'));
                timeoutTryCatch(100, done, () => {
                    input.focus();
                    triggerKeyDown(input, 37); // left
                    timeoutTryCatch(100, done, () => {
                        let selected = element.find('div[data-tribe-multiselect-selected]');
                        expect(angular.element(selected.find('.selected')).scope().opt).to.equal('ccc');
                        triggerKeyDown(input, 39); // right
                        timeoutTryCatch(100, done, () => {
                            let selected = element.find('div[data-tribe-multiselect-selected]');
                            expect(angular.element(selected.find('.selected')).scope().opt).to.equal('aaa');
                            triggerKeyDown(input, 39); // right
                            timeoutTryCatch(100, done, () => {
                                let selected = element.find('div[data-tribe-multiselect-selected]');
                                expect(angular.element(selected.find('.selected')).scope().opt).to.equal('bbb');
                                triggerKeyDown(input, 8); // delete
                                timeoutTryCatch(100, done, () => {
                                    let selected = element.find('div[data-tribe-multiselect-selected]');
                                    expect(angular.element(selected.find('.selected')).scope().opt).to.equal('ccc');
                                    triggerKeyDown(input, 8); // delete
                                    timeoutTryCatch(100, done, () => {
                                        let selected = element.find('div[data-tribe-multiselect-selected]');
                                        expect(angular.element(selected.find('.selected')).scope().opt).to.equal('aaa');
                                        done();
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    });

    it('should enter new items', (done) => {
        timeoutTryCatch(100, done, () => {
            let scope = rootScope.$new();
            scope.selected = ['aaa', 'bbb', 'ccc'];
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-multiselect data-selected-options="selected" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeoutTryCatch(100, done, () => {
                let selected = angular.element(element.find('div[data-tribe-multiselect-selected]'));
                let input = angular.element(selected.find('input'));
                timeoutTryCatch(100, done, () => {
                    input.focus();
                    let selectedScope = selected.scope();
                    selectedScope.$apply(() => selectedScope.inputText = 'fff');
                    timeoutTryCatch(100, done, () => {
                        triggerKeyDown(input, 13); // enter
                        timeoutTryCatch(100, done, () => {
                            let lastSelected = selected.find('.items').last();
                            expect(angular.element(lastSelected).scope().opt).to.equal('fff');
                            selectedScope.$apply(() => selectedScope.inputText = 'eee');
                            timeoutTryCatch(100, done, () => {
                                triggerKeyDown(input, 13); // enter
                                timeoutTryCatch(100, done, () => {
                                    let lastSelected = selected.find('.items')[3];
                                    expect(angular.element(lastSelected).scope().opt).to.equal('eee');
                                    triggerKeyDown(input, 40); // arrowdown
                                    timeoutTryCatch(100, done, () => {
                                        let available = element.find('div[data-tribe-multiselect-available]');
                                        // the list of items is visible
                                        expect(available.hasClass('active')).to.equal(true);
                                        let availableOptions = document.find('div.tribe-data-tribe-multiselect-available-body div.option');
                                        expect(availableOptions.length).to.equal(1);
                                        expect(angular.element(availableOptions.last()).scope().opt).to.equal('ddd');
                                        triggerKeyDown(input, 40); // arrowdown
                                        timeoutTryCatch(100, done, () => {
                                            triggerKeyDown(input, 13); // enter
                                            timeoutTryCatch(100, done, () => {
                                                // items are sorted, so it should go to the third position
                                                expect(angular.element(selected.find('.items')[3]).scope().opt).to.equal('ddd');
                                                done();
                                            });
                                        });
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    });

    it('should cancel and confirm edits', (done) => {
        timeoutTryCatch(100, done, () => {
            let scope = rootScope.$new();
            scope.selected = ['aaa', 'bbb', 'ccc'];
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-multiselect data-selected-options="selected" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeoutTryCatch(100, done, () => {
                let selected = angular.element(element.find('div[data-tribe-multiselect-selected]'));
                let input = angular.element(selected.find('input'));
                timeoutTryCatch(100, done, () => {
                    input.focus();
                    let selectedScope = selected.scope();
                    selectedScope.$apply(() => selectedScope.inputText = 'fff');
                    timeoutTryCatch(100, done, () => {
                        triggerKeyDown(input, 13); // enter
                        timeoutTryCatch(100, done, () => {
                            expect(_.map(angular.element(selected.find('.items')), (item) => {
                                return angular.element(item).scope().opt;
                            })).to.deep.equal(['aaa', 'bbb', 'ccc', 'fff']);
                            document.find('div.tribe-field-actions-body div[ng-click="cancel()"]').click();
                            timeoutTryCatch(100, done, () => {
                                expect(_.map(angular.element(selected.find('.items')), (item) => {
                                    return angular.element(item).scope().opt;
                                })).to.deep.equal(['aaa', 'bbb', 'ccc']);
                                selectedScope.$apply(() => selectedScope.inputText = 'ggg');
                                timeoutTryCatch(100, done, () => {
                                    triggerKeyDown(input, 13); // enter
                                    timeoutTryCatch(100, done, () => {
                                        document.find('div.tribe-field-actions-body div[ng-click="confirm()"]').click();
                                        timeoutTryCatch(100, done, () => {
                                            expect(_.map(angular.element(selected.find('.items')), (item) => {
                                                return angular.element(item).scope().opt;
                                            })).to.deep.equal(['aaa', 'bbb', 'ccc', 'ggg']);
                                            done();
                                        });
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    });

    it('should confirm edits with double enter', (done) => {
        timeoutTryCatch(100, done, () => {
            let scope = rootScope.$new();
            scope.selected = ['aaa', 'bbb', 'ccc'];
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-multiselect data-selected-options="selected" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeoutTryCatch(100, done, () => {
                let selected = angular.element(element.find('div[data-tribe-multiselect-selected]'));
                let input = angular.element(selected.find('input'));
                timeoutTryCatch(100, done, () => {
                    input.focus();
                    let selectedScope = selected.scope();
                    selectedScope.$apply(() => selectedScope.inputText = 'fff');
                    timeoutTryCatch(100, done, () => {
                        triggerKeyDown(input, 13); // enter
                        timeoutTryCatch(100, done, () => {
                            expect(_.map(angular.element(selected.find('.items')), (item) => {
                                return angular.element(item).scope().opt;
                            })).to.deep.equal(['aaa', 'bbb', 'ccc', 'fff']);
                            triggerKeyDown(input, 13); // enter again
                            timeoutTryCatch(100, done, () => {
                                expect(selected.scope().fieldDirty).to.equal(false);
                                expect(_.map(angular.element(selected.find('.items')), (item) => {
                                    return angular.element(item).scope().opt;
                                })).to.deep.equal(['aaa', 'bbb', 'ccc', 'fff']);
                                done();
                            });
                        });
                    });
                });
            });
        });
    });

    it('should cancel edits with double escape', (done) => {
        timeoutTryCatch(100, done, () => {
            let scope = rootScope.$new();
            scope.selected = ['aaa', 'bbb', 'ccc'];
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-multiselect data-selected-options="selected" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeoutTryCatch(100, done, () => {
                let selected = angular.element(element.find('div[data-tribe-multiselect-selected]'));
                let input = angular.element(selected.find('input'));
                timeoutTryCatch(100, done, () => {
                    input.focus();
                    let selectedScope = selected.scope();
                    selectedScope.$apply(() => selectedScope.inputText = 'fff');
                    timeoutTryCatch(100, done, () => {
                        triggerKeyDown(input, 13); // enter
                        timeoutTryCatch(100, done, () => {
                            expect(_.map(angular.element(selected.find('.items')), (item) => {
                                return angular.element(item).scope().opt;
                            })).to.deep.equal(['aaa', 'bbb', 'ccc', 'fff']);
                            selectedScope.$apply(() => selectedScope.inputText = 'ddd');
                            triggerKeyDown(input, 27); // escape
                            timeoutTryCatch(100, done, () => {
                                timeoutTryCatch(100, done, () => {
                                    expect(selectedScope.inputText).to.equal('');
                                    expect(_.map(angular.element(selected.find('.items')), (item) => {
                                        return angular.element(item).scope().opt;
                                    })).to.deep.equal(['aaa', 'bbb', 'ccc', 'fff']);
                                    triggerKeyDown(input, 27); // escape again
                                    timeoutTryCatch(100, done, () => {
                                        timeoutTryCatch(100, done, () => {
                                            expect(_.map(angular.element(selected.find('.items')), (item) => {
                                                return angular.element(item).scope().opt;
                                            })).to.deep.equal(['aaa', 'bbb', 'ccc']);
                                            done();
                                        });
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    });

    it('should show the "new item" label in the available oprions list', (done) => {
        timeoutTryCatch(100, done, () => {
            let scope = rootScope.$new();
            scope.selected = ['aaa', 'bbb', 'ccc'];
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-multiselect data-selected-options="selected" data-available-options="options" data-new-label="new lala"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeoutTryCatch(100, done, () => {
                let selected = angular.element(element.find('div[data-tribe-multiselect-selected]'));
                let input = angular.element(selected.find('input'));
                timeoutTryCatch(100, done, () => {
                    input.focus();
                    let selectedScope = selected.scope();
                    triggerKeyDown(input, 40); // arrowdown
                    timeoutTryCatch(100, done, () => {
                        selectedScope.$apply(() => selectedScope.inputText = 'fffffff');
                        timeoutTryCatch(100, done, () => {
                            let newLabel = angular.element(document.find('div.tribe-data-tribe-multiselect-available-body .new-opt'));
                            expect(newLabel.find('span').first().html()).to.deep.equal('new lala:');
                            done();
                        });
                    });
                });
            });
        });
    });

    it('should order available options list case insensitive', (done) => {
        timeoutTryCatch(100, done, () => {
            let scope = rootScope.$new();
            scope.options = ['basic', 'form', 'digest', 'HTTP Signatures', 'Bearer'];
            let element = angular.element('<div data-tribe-multiselect data-selected-options="[]" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeoutTryCatch(100, done, () => {
                let selected = angular.element(element.find('div[data-tribe-multiselect-selected]'));
                let input = angular.element(selected.find('input'));
                timeoutTryCatch(100, done, () => {
                    input.focus();
                    triggerKeyDown(input, 40); // arrowdown
                    timeoutTryCatch(100, done, () => {
                        let availableOptions = document.find('div.tribe-data-tribe-multiselect-available-body div.option');
                        expect(_.map(angular.element(availableOptions), (item) => {
                            return angular.element(item).scope().opt;
                        })).to.deep.equal(['basic', 'Bearer', 'digest', 'form', 'HTTP Signatures']);
                        done();
                    });
                });
            });
        });
    });

    it('should enter new items when selected items is null', (done) => {
        timeoutTryCatch(100, done, () => {
            let scope = rootScope.$new();
            scope.selected = null;
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-multiselect data-selected-options="selected" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeoutTryCatch(100, done, () => {
                let selected = angular.element(element.find('div[data-tribe-multiselect-selected]'));
                let input = angular.element(selected.find('input'));
                timeoutTryCatch(100, done, () => {
                    input.focus();
                    let selectedScope = selected.scope();
                    selectedScope.$apply(() => selectedScope.inputText = 'aaa');
                    timeoutTryCatch(100, done, () => {
                        triggerKeyDown(input, 13); // enter
                        timeoutTryCatch(100, done, () => {
                            let lastSelected = selected.find('.items').last();
                            expect(angular.element(lastSelected).scope().opt).to.equal('aaa');
                            done();
                        });
                    });
                });
            });
        });
    });
});