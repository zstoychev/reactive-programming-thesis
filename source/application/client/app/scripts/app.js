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
      .when('/polls', {
        templateUrl: 'views/polls.html',
        controller: 'PollsCtrl',
        controllerAs: 'polls'
      })
      .when('/polls/:id', {
        templateUrl: 'views/poll.html',
        controller: 'PollCtrl',
        controllerAs: 'poll'
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
