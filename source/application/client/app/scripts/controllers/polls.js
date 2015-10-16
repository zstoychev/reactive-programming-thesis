'use strict';

angular.module('clientApp').controller('PollsCtrl', function($scope, $location, pollService, notificationsService) {
  $scope.poll = {
    description: '',
    options: []
  };
  $scope.numberOfOptions = 1;

  $scope.range = _.range;

  $scope.addOption = function() {
    $scope.numberOfOptions += 1;
  };
  $scope.removeOption = function() {
    $scope.numberOfOptions -= 1;
  };

  $scope.startPoll = function() {
    notificationsService.trackPromise(pollService.startPoll($scope.poll), 'Starting poll failed').then(function(id) {
      $location.path('/polls/' + id);
    });
  };
});
