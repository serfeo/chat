package org.serfeo.dev.actors

import akka.actor.Actor
import org.java_websocket.WebSocket
import org.java_websocket.framing.CloseFrame
import org.serfeo.dev.actors.SendActor.{SendLoginErrorMessage, SendUserList}
import org.serfeo.dev.actors.UsersManager.User
import spray.json.DefaultJsonProtocol

object SendActor {
    sealed trait SendEvent

    case class BroadcastWelcomeMessage( usr: User, room: Int, users : List[ User ] ) extends SendEvent
    case class BroadcastExitMessage( usr: User, room: Int, users : List[ User ] ) extends SendEvent
    case class BroadcastMessage( msg: String, room: Int, users : List[ User ] ) extends SendEvent
    case class SendUserList( usr : User, room: Int, users: List[ User ] ) extends SendEvent

    case class SendLoginErrorMessage( socket: WebSocket )
    case class LoginErrorMessage( errorType: String )

    object SendActorJsonProtocol extends DefaultJsonProtocol {
        implicit val loginErrorMessageFormat = jsonFormat( LoginErrorMessage, "errorType" )
    }
}

class SendActor extends Actor {
    import org.serfeo.dev.actors.ChatActor.{SystemMessage,SystemListMessage,ChatMessageJsonFormat}
    import org.serfeo.dev.actors.SendActor._
    import org.serfeo.dev.actors.SendActor.SendActorJsonProtocol.loginErrorMessageFormat

    def receive = {
        case m: BroadcastWelcomeMessage => {
            val message = ChatMessageJsonFormat.systemMessageFormat.write( SystemMessage( "login", m.room, m.usr.login ) ).toString()
            sendMessages( message, m.users )
        }
        case m: BroadcastExitMessage => {
            val message = ChatMessageJsonFormat.systemMessageFormat.write( SystemMessage( "logout", m.room, m.usr.login ) ).toString()
            sendMessages( message, m.users )
        }
        case m: BroadcastMessage => {
            sendMessages( m.msg, m.users )
        }
        case m: SendUserList => {
            val message = ChatMessageJsonFormat.systemListMessageFormat.write( SystemListMessage( "user-list", m.room, m.users.map( _.login ) ) ).toString()
            sendMessages( message, List( m.usr ) )
        }
        case m: SendLoginErrorMessage => {
            m.socket.send( loginErrorMessageFormat.write( LoginErrorMessage( "LOGIN_IN_USE" ) ).toString )
            m.socket.close( CloseFrame.REFUSE )
        }
    }

    def sendMessages( msg: String, users: List[ User ] ) = {
        for ( user <- users ) user.sendMessage( msg )
    }
}
