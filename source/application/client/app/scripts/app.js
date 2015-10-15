'use strict';

angular
  .module('clientApp', [
    'ngRoute',
    'ngWebSocket'
  ])
  .config(function ($httpProvider) {
    $httpProvider.defaults.withCredentials = true;
  })
  .config(function ($routeProvider) {
    $routeProvider
      .when('/', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl',
        controllerAs: 'main'
      })
      .when('/code-processor', {
        templateUrl: 'views/code-processor.html',
        controller: 'CodeProcessorCtrl',
        controllerAs: 'codeProcessor'
      })
      .otherwise({
        redirectTo: '/'
      });
  })
  .constant('ServiceHostPort', 'localhost:9000');
