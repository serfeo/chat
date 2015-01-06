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

    var connectionActor: ActorRef = _
    var logActor: ActorRef = _
    implicit val timeout = Timeout( 15 seconds )

    override def preStart(): Unit = {
        connectionActor = context.actorOf( Props[ ConnectionManager ], "connections" )
        logActor = context.actorOf( Props[ LogActor ], "logging" )
    }

    def receive = {
        case m: Close => connectionActor ! CloseConnection( m.ws )
        case m: Open => connectionActor ! OpenConnection( m.ws, "" )
        case m: Message => {
            val message = JsonParser( m.msg ).convertTo[ ChatMessage ]
            logActor ! message

            message match {
                case ChatMessage( "login", body ) => connectionActor ! OpenConnection( m.ws, body.from )
                case ChatMessage( "logout", body ) => connectionActor ! CloseConnection( m.ws )
                case ChatMessage( "message", body ) => {
                    for( future <- ask( connectionActor, GetConnectionsExclude( body.from ) ).mapTo[ Iterable[ WebSocket ] ] )
                        for ( socket <- future ) socket.send( m.msg );
                }

                case _ => println( "Unknown message format: " + message )
            }

        }
        case _ => println( "Unknown message format:" )
    }
}