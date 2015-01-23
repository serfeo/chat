package org.serfeo.dev.actors

import akka.actor.Actor
import org.java_websocket.WebSocket
import scala.collection.mutable.{ListBuffer, Set}

object UsersManager {
    case class User( login: String, socket: WebSocket, rooms: Set[ Int ] ) {
        def sendMessage( msg: String ) = { socket.send( msg ) }
    }

    case class login( login: String, socket: WebSocket )
    case class logoutFromRoom( login: String, room: Int )
    case class logoutByLogin( login: String )
    case class logoutByWebSocket( socket: WebSocket )

    case class getUsersByRoom( room: Int )
    case class getUserByLogin( login: String )
}

class UsersManager extends Actor {
    import org.serfeo.dev.actors.UsersManager._
    import org.serfeo.dev.actors.ChatActor._

    val users = ListBuffer[ User ]()

    def receive = {
        case m: login => users += User( m.login, m.socket, Set( defaultRoom ) )

        case m: logoutFromRoom => {
            val usr = users.find( _.login == m.login )
            for ( user <- usr ) user.rooms -= m.room
            sender ! usr
        }

        case m: logoutByLogin => {
            val usr = users.find( _.login == m.login )
            for ( user <- usr ) users -= user
            sender ! usr
        }

        case m: logoutByWebSocket => {
            val usr = users.find( _.socket == m.socket )
            for ( user <- usr ) users -= user
            sender ! usr
        }

        case m: getUsersByRoom => sender ! ( users.filter( _.rooms.contains( m.room ) ).toList )

        case m: getUserByLogin => sender ! users.find( _.login == m.login )
    }
}
