angular.module( 'chat', [] )
.controller( 'ChatController', function( $scope ) {
    $scope.isAuthenticated = false;

    $scope.userList = [];
    $scope.history = [];
    $scope.login = "";
    $scope.room = 0;

    $scope.sendMessage = function() {
        if ( $scope.message ) {
            var message = { action: "message", room: $scope.room, from: $scope.login, to: '', text: $scope.message };

            $scope.history.push( message );
            socket.send( JSON.stringify( message ) );

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
        socket = new WebSocket( "ws://127.0.0.1:6696/chat" );
        socket.onopen = function() {
            socket.send( JSON.stringify( { action: "login", room: $scope.room, value: $scope.login } ) );
            socket.send( JSON.stringify( { action: "user-list", room: $scope.room, value: $scope.login } ) );

            $scope.$apply( function() {
                $scope.isAuthenticated = true;
            } );
        };

        socket.onclose = function( event ) {
            $scope.$apply( function() {
                $scope.history.push( { from: '***', text: 'Connection have closed. Please, reconnect' } );
                $scope.userList = [];
                $scope.isAuthenticated = false;
            } );
        };

        socket.onmessage = function( event ) {
            $scope.$apply( function() {
                var message = JSON.parse( event.data );
                switch ( message.action ) {
                    case "login":
                        if ( message.value !== $scope.login )
                            $scope.history.push( { from: '***', text: message.value + ' has join to chat' } );
                        else
                            $scope.history.push( { from: '***', text: "Welcome, " + message.value } );
                        $scope.userList.push( message.value );
                    break;
                    case "logout":
                        $scope.history.push( { from: '***', text: message.value + ' has left from chat' } );
                        for ( var i = 0; i < $scope.userList.length; i++ ) {
                            if ( $scope.userList[ i ] === message.value ) {
                                $scope.userList.splice( i, 1 );
                                break;
                            }
                        }
                    break;
                    case "user-list":
                        $scope.userList = message.value;
                    break;
                    case "message":
                        if ( message.from !== $scope.login )
                            $scope.history.push( { from: message.from, to: message.to, text: message.text } );
                    break;
                }
            } );
        };

        socket.onerror = function( error ) {
            $scope.$apply( function () {
                $scope.history.push( { from: '***', text: 'Connection error: ' + error.message } );
                $scope.userList = [];
                $scope.isAuthenticated = false;
            });
        };
    };
} );
