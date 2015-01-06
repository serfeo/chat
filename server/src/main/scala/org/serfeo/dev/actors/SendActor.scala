package org.serfeo.dev.actors

import akka.actor.Actor
import org.java_websocket.WebSocket

object SendActor {
    sealed trait SendEvent

    case class BroadcastWelcomeMessage( login: String, connections : Iterable[ ( WebSocket, String ) ] ) extends SendEvent
    case class BroadcastExitMessage( login: String, connections : Iterable[ ( WebSocket, String ) ] ) extends SendEvent
    case class BroadcastMessage( msg: String, connections : Iterable[ ( WebSocket, String ) ] ) extends SendEvent
}

class SendActor extends Actor {
    import org.serfeo.dev.actors.ChatActor.{ChatMessage,MessageBody,ChatMessageJsonFormat}
    import org.serfeo.dev.actors.SendActor.{BroadcastWelcomeMessage,BroadcastExitMessage,BroadcastMessage}

    def receive = {
        case m: BroadcastWelcomeMessage => {
            val message = ChatMessageJsonFormat.chatMessageFormat.write( ChatMessage( "message", MessageBody( "", "***", m.login + " has join to chat" ) ) ).toString()
            sendMessage( message, m.connections )
        }
        case m: BroadcastExitMessage => {
            val message = ChatMessageJsonFormat.chatMessageFormat.write( ChatMessage( "message", MessageBody( "", "***", m.login + " has left from chat" ) ) ).toString()
            sendMessage( message, m.connections )
        }
        case m: BroadcastMessage => {
            sendMessage( m.msg, m.connections )
        }
    }

    def sendMessage( msg: String, connections: Iterable[ ( WebSocket, String ) ] ) = {
        for ( (socket, login) <- connections ) socket.send( msg )
    }
}
