package org.serfeo.dev.actors

import akka.actor.Actor
import org.java_websocket.WebSocket
import org.serfeo.dev.actors.SendActor.SendUserList

object SendActor {
    sealed trait SendEvent

    case class BroadcastWelcomeMessage( login: String, connections : Iterable[ ( WebSocket, String ) ] ) extends SendEvent
    case class BroadcastExitMessage( login: String, connections : Iterable[ ( WebSocket, String ) ] ) extends SendEvent
    case class BroadcastMessage( msg: String, connections : Iterable[ ( WebSocket, String ) ] ) extends SendEvent
    case class SendUserList( userConnection: WebSocket, users: Iterable[ String ] ) extends SendEvent
}

class SendActor extends Actor {
    import org.serfeo.dev.actors.ChatActor.{SystemMessage,SystemListMessage,ChatMessageJsonFormat}
    import org.serfeo.dev.actors.SendActor.{BroadcastWelcomeMessage,BroadcastExitMessage,BroadcastMessage}

    def receive = {
        case m: BroadcastWelcomeMessage => {
            val message = ChatMessageJsonFormat.systemMessageFormat.write( SystemMessage( "login", m.login ) ).toString()
            sendMessages( message, m.connections )
        }
        case m: BroadcastExitMessage => {
            val message = ChatMessageJsonFormat.systemMessageFormat.write( SystemMessage( "logout", m.login ) ).toString()
            sendMessages( message, m.connections )
        }
        case m: BroadcastMessage => {
            sendMessages( m.msg, m.connections )
        }
        case m: SendUserList => {
            val message = ChatMessageJsonFormat.systemListMessageFormat.write( SystemListMessage( "user-list", m.users ) ).toString()
            sendMessage( message, m.userConnection )
        }
    }

    def sendMessages( msg: String, connections: Iterable[ ( WebSocket, String ) ] ) = {
        for ( (socket, login) <- connections ) sendMessage( msg, socket )
    }

    def sendMessage( msg: String, socket: WebSocket ) = socket.send( msg )
}
