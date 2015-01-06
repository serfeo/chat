package org.serfeo.dev.actors

import akka.actor.{ActorRef, Props, Actor}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import org.java_websocket.WebSocket
import spray.json._

import scala.concurrent.duration._

object ChatActor {
    case class MessageBody( to: String, from: String, text: String )
    case class ChatMessage( action: String, body: MessageBody )

    object ChatMessageJsonFormat extends DefaultJsonProtocol {
        implicit val messageBodyFormat = jsonFormat(MessageBody, "to", "from", "text")
        implicit val chatMessageFormat = jsonFormat(ChatMessage, "action", "body")
    }
}

class ChatActor extends Actor {
    import org.serfeo.dev.actors.ChatActor.ChatMessage
    import org.serfeo.dev.actors.ChatActor.ChatMessageJsonFormat._
    import org.serfeo.dev.actors.ConnectionManager.{OpenConnection,CloseConnection,GetConnectionsExclude}
    import org.serfeo.dev.ReactiveServer.{Close, Message,Open}
    import org.serfeo.dev.actors.SendActor.{BroadcastExitMessage, BroadcastWelcomeMessage, BroadcastMessage}

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
            val message = JsonParser( m.msg ).convertTo[ ChatMessage ]
            logActor ! message

            message match {
                case ChatMessage( "login", body ) => {
                    connectionActor ! OpenConnection( m.ws, body.from )
                    for ( connections <- ask( connectionActor, GetConnectionsExclude( body.from ) ).mapTo[ Iterable[ ( WebSocket, String ) ] ] )
                        sendActor ! BroadcastWelcomeMessage( body.from, connections )
                }
                case ChatMessage( "logout", body ) => {
                    connectionActor ! CloseConnection( m.ws )
                    for ( connections <- ask( connectionActor, GetConnectionsExclude( body.from ) ).mapTo[ Iterable[ ( WebSocket, String ) ] ] )
                        sendActor ! BroadcastExitMessage( body.from, connections )
                }
                case ChatMessage( "message", body ) => {
                    for( connections <- ask( connectionActor, GetConnectionsExclude( body.from ) ).mapTo[ Iterable[ ( WebSocket, String ) ] ] )
                        sendActor ! BroadcastMessage( m.msg, connections )
                }

                case _ => println( "Unknown message format: " + message )
            }

        }
        case _ => println( "Unknown message format:" )
    }
}
