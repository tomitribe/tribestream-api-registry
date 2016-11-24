export class TryMeService {
  constructor(private $http) {
  }

  request(request) {
    return this.$http.post('api/try', request);
  }

  getOAuth2Header(request, ignoreSsl) {
    return this.$http.post('api/try/header/oauth2?ignore-ssl=' + (!!ignoreSsl ? 'true' : 'false'), request);
  }

  getSignatureHeader(request) {
    return this.$http.post('api/try/header/signature', request);
  }

  getBasicHeader(request) {
    return this.$http.post('api/try/header/basic', request);
  }
}
