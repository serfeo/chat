angular.module( 'chat' )
.directive( 'ngEnter', function () {
    return function ( $scope, element, attrs ) {
        element.bind( "keydown keypress", function ( event ) {
            if( event.which === 13 ) {
                $scope.$apply( function () { $scope.$eval( attrs.ngEnter ); } );
                event.preventDefault();
            }
        });
    };
} )
.directive( 'chatAutoScroll', function() {
    return function( $scope, element, attrs ) {
        element.bind( "DOMSubtreeModified", function() {
            element[ 0 ].scrollTop = element[ 0 ].scrollHeight;
        } )
    }
} )