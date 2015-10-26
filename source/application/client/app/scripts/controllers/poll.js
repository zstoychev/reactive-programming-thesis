'use strict';

angular.module('clientApp').controller('PollCtrl', function($scope, $location, $routeParams, $websocket,
                                                            notificationsService, ServiceHostPort, pollService) {
  var pollId = $routeParams.id;
  $scope.pollData = null;

  function track(promise, message) {
    return notificationsService.trackPromise(promise, message);
  }

  function emptyAnswer(options) {
    return {
      name: '',
      optionsAnswers: _.fill(new Array(options), false)
    };
  }

  $scope.newAnswer = emptyAnswer(0);
  $scope.editedAnswer = null;

  $scope.editAnswer = function(id) {
    $scope.editedAnswer = id;
  };
  $scope.answerPoll = function() {
    track(pollService.answerPoll(pollId, $scope.newAnswer), 'Posting answer failed').then(function() {
      $scope.newAnswer = emptyAnswer($scope.pollData.options.length);
    });
  };
  $scope.updateAnswer = function(answer) {
    track(pollService.updatePollAnswer(pollId, answer), 'Updating answer failed').then(function() {
      $scope.editedAnswer = null;
    });
  };
  $scope.deleteAnswer = function(id) {
    track(pollService.deletePollAnswer(pollId, id), 'Deleting answer failed');
  };

  $scope.getTotal = function(index) {
    return $scope.pollData.answers.filter(function(answer) {
      return answer.optionsAnswers[index];
    }).length;
  };

  $scope.range = _.range;

  $scope.$watch('pollData.options.length', function(length) {
    if (length) {
      $scope.newAnswer = emptyAnswer(length);
    }
  });

  var pollWs = $websocket('ws://' + ServiceHostPort + '/polls/' + pollId, { reconnectIfNotNormalClose: true });
  pollWs.onMessage(function(message) {
    $scope.pollData = JSON.parse(message.data);
  });
  pollWs.onOpen(function() {
    notificationsService.notify('Poll stream connected');
  });
  pollWs.onError(function() {
    notificationsService.notify('Poll stream error');
  });

  // Chat logic. TODO: Refactor in separate directive

  $scope.messages = [];

  $scope.chatInput = {
    sender: '',
    message: ''
  };

  $scope.postChatMessage = function() {
    track(pollService.postChatMessage(
      pollId, $scope.chatInput.sender, $scope.chatInput.message), 'Posting message failed').then(function() {
      $scope.chatInput.message = '';
      $scope.chatForm.$setPristine();
    });
  };

  var chatsWs = $websocket('ws://' + ServiceHostPort + '/poll-chats/' + pollId, { reconnectIfNotNormalClose: true });
  chatsWs.onMessage(function(message) {
    $scope.messages.push(JSON.parse(message.data));
  });
  chatsWs.onOpen(function() {
    notificationsService.notify('Chat stream connected');
  });
  chatsWs.onError(function() {
    notificationsService.notify('Chat stream error');
  });
});
