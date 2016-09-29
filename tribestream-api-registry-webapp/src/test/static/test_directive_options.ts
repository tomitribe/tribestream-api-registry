describe('it tests our custom options component', () => {
    let expect = chai.expect;

    var compile;
    var rootScope;
    var timeout;
    var document;

    beforeEach(() => angular.module('website-components'));
    beforeEach((done) => {
        angular.injector(['ng', 'website-components']).invoke([
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

    it('should show options', (done) => {
        let scope = rootScope.$new();
        scope.options = ['Copy...', 'Release...', 'Export...', 'Share link...'];
        let element = angular.element('<i data-tribe-editable-option data-empty-text="Actions" data-value="action" data-options="options"></i>');
        compile(element)(scope);
        // append to body so we can click on it.
        element.appendTo(document.find('body'));
        timeoutTryCatch(100, done, () => {
            let comp = document.find('body [data-tribe-editable-option]');
            expect(comp.length).to.equal(1);
            expect(document.find('body').html()).to.not.contain('Copy...');
            let compScope = comp.find('> div').scope();
            compScope.open();
            timeoutTryCatch(100, done, () => {
                expect(document.find('body').html()).to.contain('Copy...');
                document.find('body').click();
                timeoutTryCatch(100, done, () => {
                    expect(document.find('body').html()).to.not.contain('Copy...');
                    compScope.open();
                    timeoutTryCatch(100, done, () => {
                        expect(document.find('body').html()).to.contain('Copy...');
                        triggerKeyDown(document, 27);
                        timeoutTryCatch(100, done, () => {
                            expect(document.find('body').html()).to.not.contain('Copy...');
                            compScope.open();
                            done();
                        });
                    });
                });
            });
        });
    });

});