'use strict';

angular.module('clientApp').factory('pollService', function(apiCall) {
  // Setups timeout
  var polls = apiCall('polls').withHttpConfig({ timeout: 4000 });

  return {
    startPoll: function(poll) {
      return polls.post(poll);
    },

    answerPoll: function(id, answer) {
      return polls.sub(id).post(answer);
    },

    updatePollAnswer: function(id, answer) {
      return polls.sub(id).sub('answers').sub(answer.id).put(answer);
    },

    deletePollAnswer: function(id, answerId) {
      return polls.sub(id).sub('answers').sub(answerId).delete();
    },

    postChatMessage: function(id, sender, message) {
      return apiCall('poll-chats').withHttpConfig({ timeout: 4000 }).sub(id).post({
        name: sender,
        message: message
      })
    }
  }
});
