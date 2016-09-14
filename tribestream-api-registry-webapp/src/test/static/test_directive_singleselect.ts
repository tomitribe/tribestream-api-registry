describe('it tests our custom singleselect component', () => {
    let expect = chai.expect;

    var compile;
    var rootScope;
    var timeout;
    var document;

    beforeEach(() => angular.module('website-components-singleselect'));
    beforeEach((done) => {
        angular.injector(['ng', 'website-components-singleselect']).invoke([
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

    it('should show action buttons on input focus', (done) => {
        let scope = rootScope.$new();
        scope.selected = 'aaa';
        scope.options = [];
        let element = angular.element('<div data-tribe-singleselect data-selected-option="selected" data-available-options="options"></div>');
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
            scope.selected = 'aaa';
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-singleselect data-selected-option="selected" data-available-options="options"></div>');
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
                            let available = element.find('div[data-tribe-singleselect-available]');
                            // the list of items is visible
                            expect(available.hasClass('active')).to.equal(true);
                            done();
                        });
                    });
                });
            });
        });
    });

    it('should enter new items', (done) => {
        timeoutTryCatch(100, done, () => {
            let scope = rootScope.$new();
            scope.selected = 'aaa';
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-singleselect data-editable="true" data-selected-option="selected" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeoutTryCatch(100, done, () => {
                let selected = angular.element(element.find('div[data-tribe-singleselect-selected]'));
                let input = angular.element(selected.find('input'));
                timeoutTryCatch(100, done, () => {
                    input.focus();
                    let selectedScope = selected.scope();
                    selectedScope.$apply(() => selectedScope.inputText = 'fff');
                    timeoutTryCatch(100, done, () => {
                        triggerKeyDown(input, 40); // keydown
                        timeoutTryCatch(100, done, () => {
                            let newLabel = angular.element(document.find('div.tribe-data-tribe-singleselect-available-body .new-opt'));
                            expect(newLabel.find('span').first().html()).to.deep.equal('New:');
                            expect(newLabel.find('span').last().html()).to.deep.equal('fff');
                            input.blur();
                            timeoutTryCatch(100, done, () => {
                                expect(selectedScope.selectedItem).to.equal('fff');
                                done();
                            });
                        });
                    });
                });
            });
        });
    });

    it('should not enter new items', (done) => {
        timeoutTryCatch(100, done, () => {
            let scope = rootScope.$new();
            scope.selected = 'aaa';
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-singleselect data-editable="false" data-selected-option="selected" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeoutTryCatch(100, done, () => {
                let selected = angular.element(element.find('div[data-tribe-singleselect-selected]'));
                let input = angular.element(selected.find('input'));
                timeoutTryCatch(100, done, () => {
                    input.focus();
                    let selectedScope = selected.scope();
                    expect(selectedScope.selectedItem).to.equal('aaa');
                    selectedScope.$apply(() => selectedScope.inputText = 'fff');
                    timeoutTryCatch(100, done, () => {
                        triggerKeyDown(input, 40); // keydown
                        timeoutTryCatch(100, done, () => {
                            let newLabel = angular.element(document.find('div.tribe-data-tribe-singleselect-available-body .new-opt'));
                            expect(newLabel.find('span').first().html()).to.deep.equal('No matches');
                            done();
                        });
                    });
                });
            });
        });
    });

    it('should cancel edit', (done) => {
        timeoutTryCatch(100, done, () => {
            let scope = rootScope.$new();
            scope.selected = 'aaa';
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-singleselect data-selected-option="selected" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeoutTryCatch(100, done, () => {
                let selected = angular.element(element.find('div[data-tribe-singleselect-selected]'));
                let input = angular.element(selected.find('input'));
                timeoutTryCatch(100, done, () => {
                    input.focus();
                    let selectedScope = selected.scope();
                    selectedScope.$apply(() => selectedScope.inputText = 'fff');
                    timeoutTryCatch(100, done, () => {
                        triggerKeyDown(input, 27); // escape
                        timeoutTryCatch(200, done, () => {
                            expect(selectedScope.selectedItem).to.equal('aaa');
                            done();
                        });
                    });
                });
            });
        });
    });

    it('should do nothing when trying to commit new non-available entry', (done) => {
        timeoutTryCatch(100, done, () => {
            let scope = rootScope.$new();
            scope.selected = 'aaa';
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-singleselect data-selected-option="selected" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeoutTryCatch(100, done, () => {
                let selected = angular.element(element.find('div[data-tribe-singleselect-selected]'));
                let input = angular.element(selected.find('input'));
                timeoutTryCatch(100, done, () => {
                    input.focus();
                    let selectedScope = selected.scope();
                    selectedScope.$apply(() => selectedScope.inputText = 'fff');
                    timeoutTryCatch(100, done, () => {
                        triggerKeyDown(input, 13); // enter
                        timeoutTryCatch(100, done, () => {
                            expect(selectedScope.selectedItem).to.equal('aaa');
                            input.blur();
                            timeoutTryCatch(100, done, () => {
                                expect(selectedScope.inputText).to.equal('aaa');
                                done();
                            });
                        });
                    });
                });
            });
        });
    });

    it('should show empty label', (done) => {
        timeoutTryCatch(100, done, () => {
            let scope = rootScope.$new();
            scope.selected = null;
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-singleselect data-selected-option="selected" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeoutTryCatch(100, done, () => {
                expect(angular.element(element.find('span.empty')).length).to.equal(1);
                done();
            });
        });
    });

    it('should not show empty label', (done) => {
        timeoutTryCatch(100, done, () => {
            let scope = rootScope.$new();
            scope.selected = 'bbb';
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-singleselect data-selected-option="selected" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeoutTryCatch(100, done, () => {
                expect(angular.element(element.find('span.empty')).length).to.equal(0);
                done();
            });
        });
    });

});