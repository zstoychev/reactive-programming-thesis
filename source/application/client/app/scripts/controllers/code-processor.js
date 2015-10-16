'use strict';

angular.module('clientApp').controller('CodeProcessorCtrl', function($scope, $websocket,
                                                                     notificationsService, ServiceHostPort) {
  $scope.code = '';
  $scope.messages = [];
  $scope.result = null;
  $scope.parallelism = 0;
  $scope.isProcessing = false;

  function watchParallelism() {
    var ws = $websocket('ws://' + ServiceHostPort + '/parallelism');
    ws.onMessage(function(message) {
      $scope.parallelism = message.data;
    });
  }

  $scope.processCode = function() {
    $scope.isProcessing = true;
    $scope.result = null;
    $scope.messages = [];

    var ws = $websocket('ws://' + ServiceHostPort + '/code-processor');
    ws.send($scope.code);
    ws.onMessage(function(message) {
      $scope.messages.push(message);
    });
    ws.onClose(function() {
      $scope.isProcessing = false;
      $scope.result = $scope.messages[$scope.messages.length - 1].data;
      $scope.$apply();
    });
    ws.onError(function() {
      notificationsService.notify('Connection error');
    });
  };

  watchParallelism();
});
