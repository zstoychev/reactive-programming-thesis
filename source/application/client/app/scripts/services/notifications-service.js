'use strict';

angular.module('clientApp').factory('notificationsService', function($timeout, $rootScope) {
  $rootScope.notifications = [];

  var notificationsService = {
    notify: function(message) {
      var notification = { message: message };
      $rootScope.notifications.push(notification);

      $timeout(function() {
        _.remove($rootScope.notifications, function(el) {
          return el === notification;
        });
      }, 4000);
    },

    trackPromise: function(promise, message) {
      promise.catch(function() {
        notificationsService.notify(message);
      });
      return promise;
    }
  };

  return notificationsService;
});
