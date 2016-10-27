describe('Testing the markdown component', () => {
    require("../scripts/_components_textfield.ts");
    require('../scripts/_components_markdown.ts');
    require('../scripts/_components_field_actions.ts');
    require('../scripts/_components_filters.ts');

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
        let compiledScope = element.scope();
        // append to body so we can click on it.
        element.appendTo(document.find('body'));
        timeoutTryCatch(100, done, () => {
            let previewValue = angular.element(element.find('div[data-tribe-markdown] > div')[0]).scope()['preview'];
            expect(previewValue).toContain('<p>body content here</p>');
            expect(previewValue).not.toContain('<p>second paragraph</p>');
            timeoutTryCatch(100, done, () => {
                let mainDiv = element.find('> div');
                expect(mainDiv.scope()['cmFocused']).toEqual(false);
                mainDiv.focus();
                timeoutTryCatch(100, done, () => {
                    let elScope = mainDiv.scope();
                    expect(elScope['cmFocused']).toEqual(true);
                    elScope.$apply(() => elScope['value'] = `
# title

body content here

second paragraph
`);
                    timeoutTryCatch(100, done, () => {
                        expect(elScope['fieldDirty']).toEqual(true);
                        let actionBtns = document.find('div.tribe-field-actions-body');
                        expect(actionBtns.length).toEqual(1);
                        expect(element.find('div.preview').html()).toContain('<p>body content here</p>');
                        expect(element.find('div.preview').html()).toContain('<p>second paragraph</p>');
                        timeoutTryCatch(100, done, () => {
                            actionBtns.find('div[ng-click="cancel()"]').click();
                            timeoutTryCatch(100, done, () => {
                                expect(element.find('div.preview').html()).toContain('<p>body content here</p>');
                                expect(element.find('div.preview').html()).not.toContain('<p>second paragraph</p>');
                                elScope.$apply(() => elScope['value'] = `
# title

body content here

second paragraph
`);
                                timeoutTryCatch(100, done, () => {
                                    expect(elScope['fieldDirty']).toEqual(true);
                                    let actionBtns = document.find('div.tribe-field-actions-body');
                                    expect(actionBtns.length).toEqual(1);
                                    expect(element.find('div.preview').html()).toContain('<p>body content here</p>');
                                    expect(element.find('div.preview').html()).toContain('<p>second paragraph</p>');
                                    timeoutTryCatch(100, done, () => {
                                        actionBtns.find('div[ng-click="confirm()"]').click();
                                        timeoutTryCatch(100, done, () => {
                                            expect(element.find('div.preview').html()).toContain('<p>body content here</p>');
                                            expect(element.find('div.preview').html()).toContain('<p>second paragraph</p>');
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
                expect(element.find('div.editor-preview').html()).toContain('<em>body</em>');
                expect('titi').toBe('titi');
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
                expect(element.find('div.preview > div[x-ng-bind-html]').html()).toContain('<code class="lang-xml hljs">');
                done();
            });
        });
    });

    let triggerKeyDown = function (element, keyCode) {
        var e = $.Event("keyup");
        e.keyCode = keyCode;
        element.trigger(e);
    };

    it('should show help page', (done) => {
        let scope = rootScope.$new();
        scope.myvalue = "";
        let element = angular.element('<div data-tribe-markdown data-value="myvalue"></div>');
        compile(element)(scope);
        // append to body so we can click on it.
        element.appendTo(document.find('body'));
        timeoutTryCatch(100, done, () => {
            let toggleBtn = element.find('div.editor-toolbar > a.fa-question-circle');
            toggleBtn.click();
            timeoutTryCatch(100, done, () => {
                expect(document.find('body div.markdown-help-content').length).toEqual(1);
                triggerKeyDown(document, 27); // escape button
                timeoutTryCatch(100, done, () => {
                    expect(document.find('body div.markdown-help-content').length).toEqual(0);
                    done();
                });
            });
        });
    });

    it('should remove single char [REG-340]', (done) => {
        let scope = rootScope.$new();
        scope.myvalue = "";
        let element = angular.element('<div data-tribe-markdown data-value="myvalue"></div>');
        compile(element)(scope);
        // append to body so we can click on it.
        element.appendTo(document.find('body'));
        timeoutTryCatch(100, done, () => {
            let mainDiv = element.find('> div');
            mainDiv.focus();
            let mainScope = mainDiv.scope();
            mainScope['onChange']('A');
            timeoutTryCatch(100, done, () => {
                mainScope['onChange']('');
                mainDiv.blur();
                timeoutTryCatch(100, done, () => {
                    expect(element.find('div.preview > div').html()).toEqual('');
                    done();
                });
            });
        });
    });
});
