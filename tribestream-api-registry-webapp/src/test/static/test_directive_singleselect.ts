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
        scope.selected = ['aaa'];
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

});