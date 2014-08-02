var portfolioApp = angular.module('portfolioApp', [], function($locationProvider) {
  $locationProvider.html5Mode(true);
});

portfolioApp.controller('LoginController', function($scope, $location) {
  $scope.loginFailed = $location.search()['error'];
});