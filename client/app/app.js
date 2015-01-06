angular.module( 'chat', [] )
.controller( 'ChatController', function( $scope ) {
    $scope.isAuthenticated = false;

    $scope.history = [];
    $scope.login = "";

    $scope.sendMessage = function() {
        if ( $scope.message ) {
            var message = { from: $scope.login, to: '', text: $scope.message };

            $scope.history.push( message );
            socket.send( JSON.stringify( { action: "message", body: message } ) );

            $scope.message = '';
        }
    }

    $scope.loginHandler = function() {
        initWebSocket( $scope.login )
    }

    // private variables
    var socket;

    // private functions
    var initWebSocket = function( login ) {
        socket = new WebSocket( "ws://192.168.0.59:6696/chat" );
        socket.onopen = function() {
            socket.send( JSON.stringify( { action: "login", body: { from: login, to: '', text: '' } } ) );

            $scope.$apply( function() {
                $scope.history.push( { from: '***', text: 'Welcome, ' + login } );
                $scope.isAuthenticated = true;
            } );
        };

        socket.onclose = function( event ) {
            $scope.$apply( function() {
                $scope.history.push( { from: '***', text: 'Connection have closed. Please, reconnect' } );
                $scope.isAuthenticated = false;
            } );
        };

        socket.onmessage = function( event ) {
            $scope.$apply( function() {
                $scope.history.push( JSON.parse( event.data ).body )
            } );
        };

        socket.onerror = function( error ) {
            $scope.$apply( function () {
                $scope.history.push( { from: '***', text: 'Connection error: ' + error.message } );
                $scope.isAuthenticated = false;
            });
        };
    };
} );