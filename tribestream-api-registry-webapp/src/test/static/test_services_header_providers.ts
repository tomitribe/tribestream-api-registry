///<reference path="../../main/static/bower_components/DefinitelyTyped/mocha/mocha.d.ts"/>
///<reference path="../../main/static/bower_components/DefinitelyTyped/chai/chai.d.ts"/>
///<reference path="../../main/static/bower_components/DefinitelyTyped/angularjs/angular-mocks.d.ts"/>

// to run using mocha web runner start tomee and gulp (if you work on it)
// and run http://localhost:8080/registry/?test=true&grep=header%20providers
describe('it tests the header providers', () => {
  let $httpBackend, $rootScope;
  let tribeOauth2HeaderProvider, tribeBasicHeaderProvider, tribeHeaderProviderSelector;
  let expect = chai.expect;
  let fail = chai.assert.fail;

  beforeEach(module('tribe-services-header-providers'));
  beforeEach(inject([
      '$rootScope', '$httpBackend', 'tribeOauth2HeaderProvider', 'tribeBasicHeaderProvider', 'tribeHeaderProviderSelector',
      (rootScope, backend, oauth2Service, basicService, selector) => {
        $rootScope = rootScope;
        $httpBackend = backend;
        tribeOauth2HeaderProvider = oauth2Service;
        tribeBasicHeaderProvider = basicService;
        tribeHeaderProviderSelector = selector;
      }]));

  afterEach(() => {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('should be able to select Basic provider', () => expect(tribeHeaderProviderSelector.select('Basic')).to.equal(tribeBasicHeaderProvider));
  it('should be able to select OAuth2 provider', () => expect(tribeHeaderProviderSelector.select('OAuth2')).to.equal(tribeOauth2HeaderProvider));

  it('should not be valid at the beginning (Basic)', () => expect(tribeBasicHeaderProvider.isValid()).to.equal(false));
  it('should not be logged at the beginning (Oauth2)', () => expect(tribeOauth2HeaderProvider.isLogged()).to.equal(false));

  it('should get a Basic token on any login', done => {
    tribeBasicHeaderProvider.login('testuser', 'testpwd')
      .then(token => {
        expect(tribeBasicHeaderProvider.isValid()).to.equal(true);
        expect(token['token']).to.equal('Basic dGVzdHVzZXI6dGVzdHB3ZA=='); // not that important, just there as a hook
        tribeBasicHeaderProvider.getAuthorizationHeader().then(header => {
          expect(header).to.equal('Basic dGVzdHVzZXI6dGVzdHB3ZA==');

          // reset
          tribeBasicHeaderProvider.logout();
          done();
        });
      });
    $rootScope.$apply(); // basic will use a deferred, enforce it to be evaluated
  });

  it('should get a Oauth2 token on a valid login', done => {
    $httpBackend.expectPOST('api/security/oauth2', 'username=valid&password=login&grant_type=password')
      .respond(200, {access_token: 'access', refresh_token: 'refresh', expires_in: 3600});

    tribeOauth2HeaderProvider.login('valid', 'login')
      .then(() => {
        expect(tribeOauth2HeaderProvider.isLogged()).to.equal(true);
        expect(tribeOauth2HeaderProvider.isValid()).to.equal(true);
        tribeOauth2HeaderProvider.getAuthorizationHeader().then(header => {
          expect(header).to.equal('Bearer access');

          // reset
          tribeOauth2HeaderProvider.logout();
          done();
        });
      });

    $httpBackend.flush();
  });

  it('should refresh a token on expired ones', done => {
    $httpBackend.expectPOST('api/security/oauth2', 'username=valid&password=login&grant_type=password')
      .respond(200, {access_token: 'access', refresh_token: 'refresh', expires_in: -1});
    $httpBackend.expectPOST('api/security/oauth2', 'refresh_token=refresh&grant_type=refresh_token')
      .respond(200, {access_token: 'access2', refresh_token: 'refresh2', expires_in: 3600});

    tribeOauth2HeaderProvider.login('valid', 'login')
      .then(() => {
        expect(tribeOauth2HeaderProvider.isLogged()).to.equal(true);
        expect(tribeOauth2HeaderProvider.isValid()).to.equal(false);

        // will trigger a refresh flow
        tribeOauth2HeaderProvider.getAuthorizationHeader().then(header => {
          expect(header).to.equal('Bearer access2');

          // reset
          tribeOauth2HeaderProvider.logout();
          done();
        });
      });

    $httpBackend.flush();
  });

  it('should fail if refresh is not possible', done => {
    $httpBackend.expectPOST('api/security/oauth2', 'username=valid&password=login&grant_type=password')
      .respond(200, {access_token: 'access', refresh_token: 'refresh', expires_in: -1});
    $httpBackend.expectPOST('api/security/oauth2', 'refresh_token=refresh&grant_type=refresh_token')
      .respond(400, {message: 'simulating an error'});

    tribeOauth2HeaderProvider.login('valid', 'login')
      .then(() => {
        expect(tribeOauth2HeaderProvider.isLogged()).to.equal(true);
        expect(tribeOauth2HeaderProvider.isValid()).to.equal(false);

        // will trigger a refresh flow
        tribeOauth2HeaderProvider.getAuthorizationHeader().then(
          header => fail(header, '', 'An invalid refresh token flow should lead to an error'),
          error => {
            expect(error.data.message).to.equal('simulating an error');

            // reset
            tribeOauth2HeaderProvider.logout();
            done();
          });
      });

    $httpBackend.flush();
  });
});
