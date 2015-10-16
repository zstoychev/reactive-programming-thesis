'use strict';

angular.module('clientApp').factory('apiCall', function($http, ServiceHostPort) {
  function ApiCall(resourceUrl, config, fullResponse) {
    if (resourceUrl.endsWith('/')) {
      resourceUrl = resourceUrl.substring(0, resourceUrl.length - 1);
    }

    this.url = resourceUrl;
    this.config = angular.copy(config || {});
    this.fullResponse = fullResponse || false;
  }

  angular.extend(ApiCall.prototype, {
    sub: function(subResource) {
      return new ApiCall(this.url + '/' + subResource, this.config);
    },

    withHttpConfig: function(config) {
      return new ApiCall(this.url, angular.extend({}, this.config, config));
    },

    withFullResponse: function(fullResponse) {
      return new ApiCall(this.url, this.config, fullResponse);
    },

    get: function(query, headers) {
      return this._makeRequest('GET', query, headers);
    },

    delete: function(query, headers) {
      return this._makeRequest('DELETE', query, headers);
    },

    head: function(query, headers) {
      return this._makeRequest('HEAD', query, headers);
    },

    options: function(query, headers) {
      return this._makeRequest('OPTIONS', query, headers);
    },

    post: function(data, query, headers) {
      return this._makeRequest('POST', query, headers, data);
    },

    put: function(data, query, headers) {
      return this._makeRequest('PUT', query, headers, data);
    },

    patch: function(data, query, headers) {
      return this._makeRequest('PATCH', query, headers, data);
    },

    _makeRequest: function(method, query, headers, data) {
      var extraConfig = _.omit({
        method: method,
        url: this.url,
        data: data,
        params: query,
        headers: headers
      }, function(value) {
        return value === undefined || value === null;
      });
      var config = angular.extend({}, this.config, extraConfig);

      var fullResponse = $http(config);
      var dataResponse = fullResponse.then(function(response) {
        return response.data;
      });

      var result = this.fullResponse ? fullResponse : dataResponse;
      result.fullResponse = fullResponse;
      result.dataResponse = dataResponse;

      return result;
    }
  });

  return function(resourceUrl) {
    return new ApiCall("http://" + ServiceHostPort).sub(resourceUrl);
  };
});

