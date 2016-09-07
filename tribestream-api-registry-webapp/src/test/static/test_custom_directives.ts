describe('it tests our custom multiselect component', function () {
    this.timeout(0);

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

    it('should load selected options', (done) => {
        let scope = rootScope.$new();
        scope.selected = ['aaa', 'bbb'];
        scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
        let element = angular.element('<div data-tribe-multiselect data-selected-options="selected" data-available-options="options"></div>');
        compile(element)(scope);
        // append to body so we can click on it.
        element.appendTo(document.find('body'));
        // it will keep trying until angular compiled the what we need.
        var index = 20;
        let tryit = (exec) => {
            try {
                exec();
                done();
            } catch (e) {
                index = index - 1;
                if (index == 0) {
                    done(e);
                } else {
                    timeout(() => {
                        tryit(exec);
                    }, 1);
                }
            }
        };
        tryit(() => {
            expect(element.html()).to.contain('aaa');
            expect(element.html()).to.contain('bbb');
            expect(element.html()).not.to.contain('ccc');
            expect(element.html()).not.to.contain('ddd');
            expect(element.html()).not.to.contain('eee');
        });
    });

    var triggerKeyDown = function (element, keyCode) {
        var e = $.Event("keyup");
        e.keyCode = keyCode;
        element.trigger(e);
    };

    it('should show available options', (done) => {
        try {
            let scope = rootScope.$new();
            scope.selected = ['aaa', 'bbb'];
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-multiselect data-selected-options="selected" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeout(() => {
                let input = angular.element(element.find('input'));
                timeout(() => {
                    input.focus();
                    expect(element.hasClass('active')).to.equal(true);
                    triggerKeyDown(input, 40);
                    timeout(() => {
                        let available = element.find('div[data-tribe-multiselect-available]');
                        // the list of items is visible
                        expect(available.hasClass('active')).to.equal(true);
                        done();
                    }, 100);
                }, 100);
            }, 100);
        } catch (e) {
            done(e);
        }
    });

    it('should select selected items with the left-right keys', (done) => {
        try {
            let scope = rootScope.$new();
            scope.selected = ['aaa', 'bbb', 'ccc'];
            scope.options = ['aaa', 'bbb', 'ccc', 'ddd', 'eee'];
            let element = angular.element('<div data-tribe-multiselect data-selected-options="selected" data-available-options="options"></div>');
            // append to body so we can click on it.
            element.appendTo(document.find('body'));
            compile(element)(scope);
            timeout(() => {
                let input = angular.element(element.find('input'));
                timeout(() => {
                    input.focus();
                    triggerKeyDown(input, 37); // left
                    timeout(() => {
                        let selected = element.find('div[data-tribe-multiselect-selected]');
                        expect(angular.element(selected.find('.selected')).scope().opt).to.equal('ccc');
                        triggerKeyDown(input, 39); // right
                        timeout(() => {
                            let selected = element.find('div[data-tribe-multiselect-selected]');
                            expect(angular.element(selected.find('.selected')).scope().opt).to.equal('aaa');
                            triggerKeyDown(input, 39); // right
                            timeout(() => {
                                let selected = element.find('div[data-tribe-multiselect-selected]');
                                expect(angular.element(selected.find('.selected')).scope().opt).to.equal('bbb');
                                triggerKeyDown(input, 8); // delete
                                timeout(() => {
                                    let selected = element.find('div[data-tribe-multiselect-selected]');
                                    expect(angular.element(selected.find('.selected')).scope().opt).to.equal('ccc');
                                    done();
                                }, 100);
                            }, 100);
                        }, 100);
                    }, 100);
                }, 100);
            }, 100);
        } catch (e) {
            done(e);
        }
    });
});