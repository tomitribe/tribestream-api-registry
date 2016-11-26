let Base64 = require("../../../static/bower_components/js-base64/base64.min.js").Base64;

interface AuthenticationHeaderProvider { // TODO: move to a shared module to do basic too?
  login(username: string, password: string): angular.IPromise<any>;
  logout();
  isValid(): boolean;
  getAuthorizationHeader(): angular.IPromise<string>;
  getState(): any;
  fromState(state: any);
}

class BasicHeaderProvider implements AuthenticationHeaderProvider {
  private token: string;

  constructor(private $http: angular.IHttpService,
              private $q: angular.IQService) {
  }

  login(username: string, password: string): angular.IPromise<any> {
    return this.$http.post('api/login',
        $.param({username: username, password: password}),
        {headers: {'Content-Type': 'application/x-www-form-urlencoded'}})
        .then((response) =>  this.token = 'Basic ' + Base64.encode(username + ':' + password));
  }

  logout() {
    this.token = undefined;
  }

  isValid(): boolean {
    return !!this.token;
  }

  getAuthorizationHeader(): angular.IPromise<string> {
    const deferred = this.$q.defer();
    deferred.resolve(this.token);
    return deferred.promise;
  }

  getState() : any {
    return { type: "Basic", token: this.token};
  }

  fromState(state: any) {
    // we could check and enforce the type of the state to Basic
    this.token = state["token"];
  }
}

class OAuth2HeaderProvider implements AuthenticationHeaderProvider {
  private token;
  private expiration: number;
  private retry = 0;

  constructor(private $http: angular.IHttpService,
              private $q: angular.IQService,
              private $localStorage) {
  }

  login(username: string, password: string) {
    // client_*  are owned by the server, don't put them here
    const start = new Date().getTime();
    return this.$http.post(
        'api/security/oauth2',
        'username=' + encodeURIComponent(username) + '&password=' + encodeURIComponent(password) + '&grant_type=password',
        {headers: {'Content-Type': 'application/x-www-form-urlencoded'}})
      .then((response) => this.onLoad(response, start));
  }

  logout() {
    if (!this.isLogged()) {
      return;
    }
    this.token = undefined;
  }

  /**
   * Build a promise returning the authentication header to set on the http request.
   * Note that it automatically tries to get a refresh token respecting retry counter.
   */
  getAuthorizationHeader(): angular.IPromise<string> {
    if (!this.isValid()) {
      this.retry++;
      if (this.retry > 3) {
        const deferred = this.$q.defer();
        deferred.reject('Retried the refresh flow but token is likely invalid or server is down');
        return deferred.promise;
      }
      return this.refresh()
        .then(() => this.getAuthorizationHeader());
    }
    const deferred = this.$q.defer();
    deferred.resolve('Bearer ' + this.token['access_token']);
    return deferred.promise;
  }

  isValid(): boolean {
    return this.isLogged() && new Date().getTime() < this.expiration;
  }

  isLogged(): boolean {
    return !!this.token;
  }

  getState() : any {
    return { type: "OAuth2", token: this.token, expiration: this.expiration};
  }

  fromState(state: any) {
    this.token = state["token"];
    this.expiration = state["expiration"];
  }

  private refresh() {
    const start = new Date().getTime();
    return this.$http.post(
        'api/security/oauth2',
        'refresh_token=' + encodeURIComponent(this.token['refresh_token']) + '&grant_type=refresh_token',
        {headers: {'Content-Type': 'application/x-www-form-urlencoded'}})
      .then((response) => this.onLoad(response, start));
  }

  private onLoad(response, start) {
    this.retry = 0;
    this.token = response.data;
    this.expiration = start + (1000 * (this.token['expires_in'] || 3600))
    this.$localStorage.tribe.security = this.getState();
  }
}

/**
 * Intended to be used if the GUI exposes a selector for the auth type.
 * This selector will accept 'Basic' and 'OAuth2' values.
 */
class HeaderProviderSelector {
  constructor(private oauth2: OAuth2HeaderProvider,
              private basic: BasicHeaderProvider) {
  }

  select(type): AuthenticationHeaderProvider {
    switch(type) {
      case 'Basic':
        return this.basic;
      case 'OAuth2':
      default:
        return this.oauth2;
    }
  }
}

// register them all as angular services
angular.module('tribe-services-header-providers', ['website-browser'])
  .service('tribeOauth2HeaderProvider', ['$http',  '$q', '$localStorage', OAuth2HeaderProvider])
  .service('tribeBasicHeaderProvider', ['$http', '$q', BasicHeaderProvider])
  .service('tribeHeaderProviderSelector', ['tribeOauth2HeaderProvider', 'tribeBasicHeaderProvider', HeaderProviderSelector]);
