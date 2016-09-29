describe('it tests the endpoint details page', () => {
    let expect = chai.expect;

    var compile;
    var rootScope;
    var timeout;
    var document;
    var appEndpointsDetailsHeaderService;

    beforeEach(() => angular.module('tribe-endpoints-details'));
    beforeEach(() => angular.module('website-components-markdown'));
    beforeEach(() => angular.module('website-components'));
    beforeEach(() => angular.module('website-components-field-actions'));


    beforeEach(() => angular.module('website-components'));

    beforeEach((done) => {
        angular.injector(['ng', 'tribe-endpoints-details', 'website-components']).invoke([
            '$compile', '$rootScope', '$timeout', '$document', 'appEndpointsDetailsHeaderService',
            function ($compile, $rootScope, $timeout, $document, $appEndpointsDetailsHeaderService) {
                compile = $compile;
                rootScope = $rootScope;
                timeout = $timeout;
                document = $document;
                appEndpointsDetailsHeaderService = $appEndpointsDetailsHeaderService;
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

    it('should load baseUrl acording to the swagger file', () => {
        expect(appEndpointsDetailsHeaderService.getBaseUrl({
            basePath: '/kakaka',
            host: 'http://lalala:8080'
        })).to.equal('http://lalala:8080/kakaka');
        expect(appEndpointsDetailsHeaderService.getBaseUrl({
            basePath: '/kakaka/tatata',
            host: 'http://lalala'
        })).to.equal('http://lalala/kakaka/tatata');
        expect(appEndpointsDetailsHeaderService.getBaseUrl({
            basePath: '/',
            host: 'http://lalala:8080'
        })).to.equal('http://lalala:8080');
    });

    it('should load "response request" section', (done) => {
        let scope = rootScope.$new();
        let item = {
            http_status: 222,
            error_code: 333,
            message: 'lalala',
            description: 'lelele'
        };
        scope.endpoint = {
            operation: {
                'x-tribestream-api-registry': {
                    'response-codes': [item]
                }
            }
        };
        let element = angular.element('<div data-app-endpoints-details-response-request></div>');
        compile(element)(scope);
        // append to body so we can click on it.
        element.appendTo(document.find('body'));
        timeoutTryCatch(200, done, () => {
            expect(element.html()).to.contain('lalala');
            let compiledScope = element.find('> div').scope();
            compiledScope.addErrorCode();
            timeoutTryCatch(100, done, () => {
                expect(compiledScope.endpoint.operation['x-tribestream-api-registry']['response-codes'].length).to.equal(2);
                compiledScope.removeErrorCode(item);
                timeoutTryCatch(100, done, () => {
                    expect(compiledScope.endpoint.operation['x-tribestream-api-registry']['response-codes'].length).to.equal(1);
                    let remainingItem = compiledScope.endpoint.operation['x-tribestream-api-registry']['response-codes'][0];
                    expect(remainingItem.http_status).to.equal(0);
                    expect(remainingItem.error_code).to.equal(0);
                    expect(remainingItem.message).to.equal('');
                    expect(remainingItem.description).to.equal('');
                    done();
                });
            });
        });
    });

});