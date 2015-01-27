angular.module( 'chat', [] )
.controller( 'ChatController', function( $scope ) {
    $scope.isAuthenticated = false;

    $scope.userList = [];
    $scope.rooms = [ { id: 0,
                       title: "Main room" } ];
    $scope.room = 0;
    $scope.history = { 0: [] };
    $scope.login = "";

    $scope.sendMessage = function() {
        if ( $scope.message ) {
            var message = { action: "message", room: $scope.room, from: $scope.login, to: '', text: $scope.message };

            addMessageToHistory( message );
            socket.send( JSON.stringify( message ) );

            $scope.message = '';
        }
    }

    $scope.loginHandler = function() {
        initWebSocket( $scope.login )
    }

    $scope.getCurrentTime = function() {
        return ( new Date() ).getTime()
    }

    $scope.startPrivateRoom = function( login ) {
        if ( login != $scope.login )
            socket.send( JSON.stringify( { action: "start-private", room: 0, user1: $scope.login, user2: login } ) )
    }

    $scope.changeRoom = function( room ) {
        if ( $scope.room != room )
            $scope.room = room;
    }

    // private variables
    var socket;

    // private functions
    var initWebSocket = function( login ) {
        socket = new WebSocket( "ws://192.168.1.127:6696/chat" );
        socket.onopen = function() {
            socket.send( JSON.stringify( { action: "login", room: $scope.room, value: $scope.login } ) );
            socket.send( JSON.stringify( { action: "user-list", room: $scope.room, value: $scope.login } ) );

            $scope.$apply( function() {
                $scope.isAuthenticated = true;
            } );
        };

        socket.onclose = function( event ) {
            $scope.$apply( function() {
                broadcastMessage( { from: '***', text: 'Connection have closed. Please, reconnect' } );
                $scope.userList = [];
                $scope.isAuthenticated = false;
            } );
        };

        socket.onmessage = function( event ) {
            $scope.$apply( function() {
                var message = JSON.parse( event.data );
                if ( message.errorType === "LOGIN_IN_USE" ) {
                    broadcastMessage( { from: '***', text: "Login [ " + $scope.login + ' ] already in use. Please, choose other login.' } );
                } else {
                    switch ( message.action ) {
                        case "login":
                            if ( message.value !== $scope.login )
                                addMessageToHistory( { from: '***', text: message.value + ' has join to chat' }, message.room );
                            else
                                addMessageToHistory( { from: '***', text: "Welcome, " + message.value }, message.room );
                            $scope.userList.push( message.value );
                        break;
                        case "logout":
                            broadcastMessage( { from: '***', text: message.value + ' has left' } );
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
                                addMessageToHistory( { from: message.from, to: message.to, text: message.text }, message.room );
                        break;
                        case "start-private":
                            $scope.rooms.push( { id: message.room, title: message.user1 + " -> " + message.user2 } );
                            $scope.room = message.room;

                            addMessageToHistory( { from: "***", text: "Private room started:" } )
                            addMessageToHistory( { from: "***", text: "Members: " + message.user1 + ", " + message.user2 } )
                        break;
                    }
                }
            } );
        };

        socket.onerror = function( error ) {
            $scope.$apply( function () {
                broadcastMessage( { from: '***', text: 'Connection error: ' + error.message } );
                $scope.userList = [];
                $scope.isAuthenticated = false;
            });
        };
    };

    var addMessageToHistory = function( message, room ) {
        if ( room === undefined )
            room = $scope.room;

        if ( $scope.history[ room ] === undefined )
            $scope.history[ room ] = [];

        $scope.history[ room ].push( message );
    }

    var broadcastMessage = function( message ) {
        for ( var room in $scope.history )
            addMessageToHistory( message, room )
    }
} );
