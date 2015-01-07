package org.serfeo.dev.actors

import akka.actor.{ActorRef, Props, Actor}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import org.java_websocket.WebSocket
import spray.json._

import scala.concurrent.duration._

object ChatActor {
    case class TypeMessage( action: String )
    case class ChatMessage( action: String, to: String, from: String, text: String )
    case class SystemMessage( action: String, value: String )
    case class SystemListMessage( action: String, value: Iterable[ String ] )

    object ChatMessageJsonFormat extends DefaultJsonProtocol {
        implicit val typeMessageFormat = jsonFormat(TypeMessage, "action")
        implicit val chatMessageFormat = jsonFormat(ChatMessage, "action", "to", "from", "text")
        implicit val systemMessageFormat = jsonFormat(SystemMessage, "action", "value")
        implicit val systemListMessageFormat = jsonFormat(SystemListMessage, "action", "value")
    }
}

class ChatActor extends Actor {
    import org.serfeo.dev.actors.ChatActor.{TypeMessage,ChatMessage,SystemMessage}
    import org.serfeo.dev.actors.ChatActor.ChatMessageJsonFormat._
    import org.serfeo.dev.actors.ConnectionManager.{OpenConnection,CloseConnection,GetConnectionsExclude,GetUserConnection}
    import org.serfeo.dev.ReactiveServer.{Close, Message,Open}
    import org.serfeo.dev.actors.SendActor.{BroadcastExitMessage, BroadcastWelcomeMessage, BroadcastMessage, SendUserList}

    var connectionActor: ActorRef = _
    var sendActor: ActorRef = _
    var logActor: ActorRef = _

    implicit val timeout = Timeout( 15 seconds )

    override def preStart(): Unit = {
        connectionActor = context.actorOf( Props[ ConnectionManager ], "connections" )
        sendActor = context.actorOf( Props[ SendActor ], "send" )
        logActor = context.actorOf( Props[ LogActor ], "logging" )
    }

    def receive = {
        case m: Close => {
            for { login <- ask( connectionActor, CloseConnection( m.ws ) ).mapTo[ String ]
                  connections <- ask( connectionActor, GetConnectionsExclude( login ) ).mapTo[ Iterable[ ( WebSocket, String ) ] ] } {
                sendActor ! BroadcastExitMessage( login, connections )
            }
        }
        case m: Open => connectionActor ! OpenConnection( m.ws, "" )
        case m: Message => {
            JsonParser( m.msg ).convertTo[ TypeMessage ] match {
                case TypeMessage( "message" ) => handleChatMessage( JsonParser( m.msg ).convertTo[ ChatMessage ] );
                case _ => handleSystemMessage( JsonParser( m.msg ).convertTo[ SystemMessage ], m.ws )
            }
        }
        case _ => println( "Unknown message format:" )
    }

    def handleChatMessage( message: ChatMessage ) = message match {
        case ChatMessage( "message", to, from, text ) => {
            for( connections <- ask( connectionActor, GetConnectionsExclude( from ) ).mapTo[ Iterable[ ( WebSocket, String ) ] ] )
                sendActor ! BroadcastMessage( chatMessageFormat.write( message ).toString(), connections )
        }

        case _ => println( "Unknown message format: " + message )
    }

    def handleSystemMessage( message: SystemMessage, socket: WebSocket ) = message match {
        case SystemMessage( "login", value ) => {
            connectionActor ! OpenConnection( socket, value )
            for ( connections <- ask( connectionActor, GetConnectionsExclude( value ) ).mapTo[ Iterable[ ( WebSocket, String ) ] ] )
                sendActor ! BroadcastWelcomeMessage( value, connections )
        }
        case SystemMessage( "logout", value ) => {
            connectionActor ! CloseConnection( socket )
            for ( connections <- ask( connectionActor, GetConnectionsExclude( value ) ).mapTo[ Iterable[ ( WebSocket, String ) ] ] )
                sendActor ! BroadcastExitMessage( value, connections )
        }
        case SystemMessage( "user-list", value ) => {
            for { connections <- ask( connectionActor, GetConnectionsExclude( value ) ).mapTo[ Iterable[ ( WebSocket, String ) ] ]
                  userConnections <- ask( connectionActor, GetUserConnection( value ) ).mapTo[ Iterable[ WebSocket ] ] } {
                for ( userConnection <- userConnections )
                    sendActor ! SendUserList( userConnection, connections.map( _._2 ) )
            }
        }

        case _ => println( "Unknown message format: " + message )
    }
}
