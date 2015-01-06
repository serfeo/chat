package reactive

import scala.collection.mutable.Map

import akka.actor.Actor
import org.java_websocket.WebSocket
import reactive.ReactiveServer._
import spray.json._

object ChatActor {
    case class MessageBody( to: String, from: String, text: String )
    case class ChatMessage( action: String, body: MessageBody )

    object ChatMessageJsonFormat extends DefaultJsonProtocol {
        implicit val messageBodyFormat = jsonFormat(MessageBody, "to", "from", "text")
        implicit val chatMessageFormat = jsonFormat(ChatMessage, "action", "body")
    }
}

class ChatActor extends Actor {
    import reactive.ChatActor.ChatMessageJsonFormat._
    import reactive.ChatActor.{ChatMessage}

    val connections = Map[ String, WebSocket ]()

    def receive = {
        case m: Message => {
            // log the message
            println( m.msg )

            val message = JsonParser( m.msg ).convertTo[ ChatMessage ]
            message match {
                case ChatMessage( "login", body ) => connections += ( body.from -> m.ws )
                case ChatMessage( "logout", body ) => connections -= body.from
                case ChatMessage( "message", body ) => {
                    for ( ( user, socket ) <- connections.filterKeys( !_.equals( body.from ) ) )
                        socket.send( m.msg );
                }

                case _ => println( "Unknown message format: " + message )
            }

        }
        case _ => println( "Unknown message format:" )
    }
}
