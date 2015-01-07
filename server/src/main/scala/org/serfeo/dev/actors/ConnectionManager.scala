package org.serfeo.dev.actors

import scala.collection.mutable.Map

import akka.actor.Actor
import org.java_websocket.WebSocket
import org.serfeo.dev.actors.ConnectionManager.{GetUserConnection, GetConnectionsExclude, CloseConnection, OpenConnection}

object ConnectionManager {
    sealed trait ConnectionEvent

    case class OpenConnection( socket: WebSocket, login: String ) extends ConnectionEvent
    case class CloseConnection( socket: WebSocket ) extends ConnectionEvent
    case class GetConnectionsExclude( login: String ) extends ConnectionEvent
    case class GetUserConnection( login: String ) extends ConnectionEvent
}

class ConnectionManager extends Actor {
    private val connectionPool = Map[ WebSocket, String ]()

    def receive = {
        case m: OpenConnection => connectionPool += ( m.socket -> m.login )
        case m: CloseConnection => {
            val login = connectionPool.getOrElse( m.socket, "" )
            connectionPool -= m.socket
            sender ! login
        }
        case m: GetConnectionsExclude => sender ! ( connectionPool.filter( !_._2.equals( m.login ) ) )
        case m: GetUserConnection => sender ! ( connectionPool.filter( _._2.equals( m.login ) ).map( _._1 ) )
    }
}
