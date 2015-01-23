package org.serfeo.dev.actors

import akka.actor.Actor
import org.serfeo.dev.actors.SendActor.SendUserList
import org.serfeo.dev.actors.UsersManager.User

object SendActor {
    sealed trait SendEvent

    case class BroadcastWelcomeMessage( usr: User, room: Int, users : List[ User ] ) extends SendEvent
    case class BroadcastExitMessage( usr: User, room: Int, users : List[ User ] ) extends SendEvent
    case class BroadcastMessage( msg: String, room: Int, users : List[ User ] ) extends SendEvent
    case class SendUserList( usr : User, room: Int, users: List[ User ] ) extends SendEvent
}

class SendActor extends Actor {
    import org.serfeo.dev.actors.ChatActor.{SystemMessage,SystemListMessage,ChatMessageJsonFormat}
    import org.serfeo.dev.actors.SendActor.{BroadcastWelcomeMessage,BroadcastExitMessage,BroadcastMessage}

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
    }

    def sendMessages( msg: String, users: List[ User ] ) = {
        for ( user <- users ) user.sendMessage( msg )
    }
}
