describe('it tests our custom text component', () => {
    let expect = chai.expect;

    var compile;
    var rootScope;
    var timeout;
    var document;

    beforeEach(() => angular.module('website-components-text'));
    beforeEach((done) => {
        angular.injector(['ng', 'website-components-text']).invoke([
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
        var e = angular.element.Event("keyup");
        e.keyCode = keyCode;
        element.trigger(e);
    };

    it('should have title', (done) => {
        let scope = rootScope.$new();
        scope.value = null;
        let element = angular.element('<div data-tribe-text data-value="value"></div>');
        compile(element)(scope);
        // append to body so we can click on it.
        element.appendTo(document.find('body'));
        timeoutTryCatch(100, done, () => {
            expect(element.find('> div').attr('title')).to.deep.equal('Click to edit');
            done();
        });
    });

    it('should load selected option', (done) => {
        let scope = rootScope.$new();
        scope.value = 'aaa';
        let element = angular.element('<div data-tribe-text data-value="value"></div>');
        compile(element)(scope);
        // append to body so we can click on it.
        element.appendTo(document.find('body'));
        timeoutTryCatch(100, done, () => {
            let value = element.scope().value;
            expect(value).to.deep.equal('aaa');
            done();
        });
    });

    it('should show action buttons on change', (done) => {
        let scope = rootScope.$new();
        scope.value = 1;
        let element = angular.element('<div data-tribe-text data-type="number" data-value="value"></div>');
        compile(element)(scope);
        // append to body so we can click on it.
        element.appendTo(document.find('body'));
        timeoutTryCatch(100, done, () => {
            let input = angular.element(element.find('input'));
            expect(input.scope().value).to.deep.equal(1);
            input.focus();
            timeoutTryCatch(100, done, () => {
                input.scope().value = 3;
                input.scope().keyEntered({
                    keyCode: 27
                });
                timeoutTryCatch(100, done, () => {
                    expect(input.scope().value).to.deep.equal(1);
                    input.focus();
                    timeoutTryCatch(100, done, () => {
                        input.scope().value = 3;
                        timeoutTryCatch(100, done, () => {
                            expect(document.find('div.tribe-field-actions-body').length).to.equal(1);
                            input.scope().keyEntered({
                                keyCode: 13
                            });
                            timeoutTryCatch(100, done, () => {
                                expect(input.scope().value).to.deep.equal(3);
                                expect(document.find('div.tribe-field-actions-body').length).to.equal(0);
                                done();
                            });
                        });
                    });
                });
            });
        });
    });
});