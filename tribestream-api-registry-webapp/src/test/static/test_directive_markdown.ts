describe('it tests our custom markdown component', () => {
    let expect = chai.expect;

    var compile;
    var rootScope;
    var timeout;
    var document;

    beforeEach(() => angular.module('website-components-text'));
    beforeEach((done) => {
        angular.injector(['ng', 'website-components-markdown']).invoke([
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

    it('should load markdown; confirm and cancel edit;', (done) => {
        let scope = rootScope.$new();
        scope.myvalue = `
# title

body content here
        `;
        let element = angular.element('<div data-tribe-markdown data-value="myvalue"></div>');
        compile(element)(scope);
        // append to body so we can click on it.
        element.appendTo(document.find('body'));
        timeoutTryCatch(100, done, () => {
            expect(element.find('div.preview').html()).to.contain('<p>body content here</p>');
            expect(element.find('div.preview').html()).to.not.contain('<p>second paragraph</p>');
            timeoutTryCatch(100, done, () => {
                let mainDiv = element.find('> div');
                expect(mainDiv.scope().cmFocused).to.equal(false);
                mainDiv.focus();
                timeoutTryCatch(100, done, () => {
                    let elScope = mainDiv.scope();
                    expect(elScope.cmFocused).to.equal(true);
                    elScope.$apply(() => elScope.value = `
# title

body content here

second paragraph
`);
                    timeoutTryCatch(100, done, () => {
                        expect(elScope.fieldDirty).to.equal(true);
                        let actionBtns = document.find('div.tribe-field-actions-body');
                        expect(actionBtns.length).to.equal(1);
                        expect(element.find('div.preview').html()).to.contain('<p>body content here</p>');
                        expect(element.find('div.preview').html()).to.contain('<p>second paragraph</p>');
                        timeoutTryCatch(100, done, () => {
                            actionBtns.find('div[ng-click="cancel()"]').click();
                            timeoutTryCatch(100, done, () => {
                                expect(element.find('div.preview').html()).to.contain('<p>body content here</p>');
                                expect(element.find('div.preview').html()).to.not.contain('<p>second paragraph</p>');
                                elScope.$apply(() => elScope.value = `
# title

body content here

second paragraph
`);
                                timeoutTryCatch(100, done, () => {
                                    expect(elScope.fieldDirty).to.equal(true);
                                    let actionBtns = document.find('div.tribe-field-actions-body');
                                    expect(actionBtns.length).to.equal(1);
                                    expect(element.find('div.preview').html()).to.contain('<p>body content here</p>');
                                    expect(element.find('div.preview').html()).to.contain('<p>second paragraph</p>');
                                    timeoutTryCatch(100, done, () => {
                                        actionBtns.find('div[ng-click="confirm()"]').click();
                                        timeoutTryCatch(100, done, () => {
                                            expect(element.find('div.preview').html()).to.contain('<p>body content here</p>');
                                            expect(element.find('div.preview').html()).to.contain('<p>second paragraph</p>');
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

    it('should show markdown preview', (done) => {
        let scope = rootScope.$new();
        scope.myvalue = `
# title

*body* content here
`;
        let element = angular.element('<div data-tribe-markdown data-value="myvalue"></div>');
        compile(element)(scope);
        // append to body so we can click on it.
        element.appendTo(document.find('body'));
        timeoutTryCatch(100, done, () => {
            let toggleBtn = element.find('div.editor-toolbar > a.fa-eye');
            toggleBtn.click();
            timeoutTryCatch(100, done, () => {
                expect(element.find('div.editor-preview').html()).to.contain('<em>body</em>');
                done();
            });
        });
    });

    it('should highlight code', (done) => {
        let scope = rootScope.$new();
        scope.myvalue = "\`\`\`xml\n <root><a>a</a></root> \n \`\`\`";
        let element = angular.element('<div data-tribe-markdown data-value="myvalue"></div>');
        compile(element)(scope);
        // append to body so we can click on it.
        element.appendTo(document.find('body'));
        timeoutTryCatch(100, done, () => {
            let toggleBtn = element.find('div.editor-toolbar > a.fa-eye');
            toggleBtn.click();
            timeoutTryCatch(100, done, () => {
                expect(element.find('div.preview > div[x-ng-bind-html]').html()).to.contain('<code class="lang-xml hljs">');
                done();
            });
        });
    });

});