angular.module( 'chat', [] )
.controller( 'ChatController', function( $scope ) {
    $scope.history = [];

    $scope.sendMessage = function() {
        if ( $scope.message ) {
            $scope.history.push( { from: 'Me', to: '', text: $scope.message } );
            $scope.message = '';
        }
    }

    // private variables
    var socket;

    // private functions
    var initWebSocket = function() {
        socket = new WebSocket( "ws://127.0.0.1:8095/chat" );
        socket.onopen = function() {
            alert("Соединение установлено.");
        };

        socket.onclose = function(event) {
            if (event.wasClean) {
                alert('Соединение закрыто чисто');
            } else {
                alert('Обрыв соединения'); // например, "убит" процесс сервера
            }
            alert('Код: ' + event.code + ' причина: ' + event.reason);
        };

        socket.onmessage = function(event) {
            alert("Получены данные " + event.data);
        };

        socket.onerror = function(error) {
            alert("Ошибка " + error.message);
        };
    };

    initWebSocket();
} );