package org.serfeo.dev

import java.net.InetSocketAddress

import akka.actor.ActorRef
import org.java_websocket.WebSocket
import org.java_websocket.framing.CloseFrame
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer

import scala.collection.mutable.Map

object ReactiveServer {
    sealed trait ReactiveServerMessage

    case class Message( ws: WebSocket, msg: String ) extends ReactiveServerMessage
    case class Open( ws: WebSocket, hs: ClientHandshake ) extends ReactiveServerMessage
    case class Close( ws: WebSocket, code: Int, reason: String, external: Boolean ) extends ReactiveServerMessage
    case class Error( ws: WebSocket, ex: Exception ) extends ReactiveServerMessage
}

class ReactiveServer( val port: Int ) extends WebSocketServer( new InetSocketAddress( port ) ) {
    private val pathToActor = Map[ String, ActorRef ]( )
    private val socketToPath = Map[ WebSocket, String ]()

    def forResource( descriptor: String, reactor: Option[ ActorRef ] ) = for ( actor <- reactor ) pathToActor += ( ( descriptor, actor ) )

    override def onOpen( ws: WebSocket, hs: ClientHandshake ) {
        if ( null != ws ) {
            socketToPath += ( ( ws, hs.getResourceDescriptor ) )
            getActor( ws ) match {
                case Some( actor ) => actor ! ReactiveServer.Open( ws, hs )
                case None => ws.close( CloseFrame.REFUSE )
            }
        }
    }

    override def onClose( ws: WebSocket, code: Int, reason: String, external: Boolean ) {
        if ( null != ws ) {
            getActor( ws ) match {
                case Some( actor ) => actor ! ReactiveServer.Close( ws, code, reason, external )
                case None => ws.close( CloseFrame.REFUSE )

            }
        }
    }

    override def onMessage( ws: WebSocket, msg: String ) {
        if ( null != ws ) {
            getActor( ws ) match {
                case Some( actor ) => actor ! ReactiveServer.Message( ws, msg )
                case None => ws.close( CloseFrame.REFUSE )
            }
        }
    }

    override def onError( ws: WebSocket, ex: Exception ) {
        if ( null != ws ) {
            getActor( ws ) match {
                case Some( actor ) => actor ! ReactiveServer.Error( ws, ex )
                case None => ws.close( CloseFrame.REFUSE )
            }
        }
    }

    def getActor( ws: WebSocket ): Option[ ActorRef ] = {
        var actorRef: Option[ ActorRef ] = None
        if ( null != ws ) {
            for ( desc <- socketToPath.get( ws ) ) {
                actorRef = pathToActor.get( desc )
            }
        }

        actorRef
    }

}
