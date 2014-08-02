var portfolioApp = angular.module('portfolioApp', []);

var portfolioController = portfolioApp.controller('PortfolioController', function($scope, $window, $log) {
  // private fields
  var socket = new SockJS('/portfolio', options = {debug: false});
  var stompClient = Stomp.over(socket);
  var rowLookup = {};

  // $scope fields
  $scope.username = '';
  $scope.rows = [];
  $scope.selectedRow = {};
  $scope.selectedAction = '';
  $scope.sharesToTrade = 0;
  $scope.notifications = [];
  $scope.validationError = '';
  $scope.suppressValidation = true;

  // Private methods
  var connect = function() {
    stompClient.connect({}, function(frame) {
      $log.log('Connected ' + frame);
      $scope.username = frame.headers['user-name'];

      stompClient.subscribe('/positions', function(message) {
        $scope.rows = angular.fromJson(message.body);
        angular.forEach($scope.rows , function(row) {
          rowLookup[row.ticker] = row;
          row.oldPrice = row.price;
          row.change = 0.0;
          row.arrow = 'glyphicon glyphicon-minus';
        });
        $scope.$apply();
      });

      stompClient.subscribe('/user/queue/position-updates', function(message) {
        var position = angular.fromJson(message.body);
        rowLookup[position.ticker].shares = position.shares;
        $scope.$apply();
        pushNotification("Position update " + message.body);
      });

      stompClient.subscribe('/user/queue/errors', function(message) {
        pushNotification("Error: " + message.body);
      });

      stompClient.subscribe('/topic/price.stock.*', function(message) {
        var quote = angular.fromJson(message.body);
        var row = rowLookup[quote.ticker];
        if(row != null) {
          row.change = quote.price - row.price;
          row.oldPrice = row.price;
          row.price = quote.price;
          row.arrow = (row.change < 0) ? 'glyphicon glyphicon-arrow-down' : 'glyphicon glyphicon-arrow-up'
          $scope.$apply();
        }
      });


    }, function(error) {
      $log.error('STOMP protocol error: ' + error);
    });
  }

  var pushNotification = function(msg) {
    $scope.notifications.push({value:msg});
    if ($scope.notifications.length > 5)
      $scope.notifications.shift();
  }

  var validateShares = function() {
    if (!angular.isNumber($scope.sharesToTrade) || $scope.sharesToTrade < 1) {
      $scope.validationError = 'Invalid number';
      return false;
    }
    if (($scope.selectedAction === 'Sell') && ($scope.sharesToTrade > $scope.selectedRow.shares)) {
      $scope.validateError = 'Not enough shares';
      return false;
    }
    return true;
  }

  // $scope methods
  $scope.logout = function() {
    stompClient.disconnect();
    $window.location = '../logout.html';
    $log.log('Logged out!');
  }

  $scope.showTradeDialog = function(action, row) {
    $scope.validationError = '';
    $scope.sharesToTrade = 0;

    $('#trade-dialog').modal('show');
    $scope.selectedAction = action;
    $scope.selectedRow = row;
  }

  $scope.executeTrade = function() {
    if (!$scope.suppressValidation && !validateShares()) {
      return;
    }
    var trade = {
      ticker: $scope.selectedRow.ticker,
      shares: $scope.sharesToTrade,
      action: $scope.selectedAction
    };
    stompClient.send('/trade', {}, angular.toJson(trade));

    $('#trade-dialog').modal('hide');
  }

  $scope.totalShares = function() {
    var total = 0.0;
    angular.forEach($scope.rows , function(row) {
      total += row.shares;
    });
    return total;
  }

  $scope.totalValue = function() {
    var total = 0.0;
    angular.forEach($scope.rows , function(row) {
      total += row.shares * row.price;
    });
    return total;
  }

  // Initialization
  connect();
});