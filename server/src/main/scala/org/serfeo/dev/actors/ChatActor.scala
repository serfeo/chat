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
    case class ChatMessage( action: String, room: Int, to: String, from: String, text: String )
    case class SystemMessage( action: String, room: Int, value: String )
    case class SystemListMessage( action: String, room: Int, value: Iterable[ String ] )

    object ChatMessageJsonFormat extends DefaultJsonProtocol {
        implicit val typeMessageFormat = jsonFormat( TypeMessage, "action" )
        implicit val chatMessageFormat = jsonFormat( ChatMessage, "action", "room", "to", "from", "text" )
        implicit val systemMessageFormat = jsonFormat( SystemMessage, "action", "room", "value" )
        implicit val systemListMessageFormat = jsonFormat( SystemListMessage, "action", "room", "value" )
    }

    val defaultRoom = 0;
}

class ChatActor extends Actor {
    import org.serfeo.dev.actors.ChatActor._
    import org.serfeo.dev.actors.ChatActor.ChatMessageJsonFormat._
    import org.serfeo.dev.actors.UsersManager._
    import org.serfeo.dev.actors.SendActor._
    import org.serfeo.dev.ReactiveServer._

    var userManager: ActorRef = _
    var sendActor: ActorRef = _
    var logActor: ActorRef = _

    implicit val timeout = Timeout( 15 seconds )

    override def preStart(): Unit = {
        userManager = context.actorOf( Props[ UsersManager ], "users" )
        sendActor = context.actorOf( Props[ SendActor ], "send" )
        logActor = context.actorOf( Props[ LogActor ], "logging" )
    }

    def receive = {
        case m: Close => {
            for { usr <- ask( userManager, logoutByWebSocket( m.ws ) ).mapTo[ Option[ User ] ]
                  users <- ask( userManager, getUsersByRoom( defaultRoom ) ).mapTo[ List[ User ] ] }
                for ( user <- usr ) sendActor ! BroadcastExitMessage( user, defaultRoom, users )
        }

        case m: Message => {
            JsonParser( m.msg ).convertTo[ TypeMessage ] match {
                case TypeMessage( "message" ) => handleChatMessage( JsonParser( m.msg ).convertTo[ ChatMessage ] );
                case _ => handleSystemMessage( JsonParser( m.msg ).convertTo[ SystemMessage ], m.ws )
            }
        }

        case _ => println( "Unknown message format in ChatActor receive" )
    }

    def handleChatMessage( message: ChatMessage ) = message match {
        case ChatMessage( "message", room, to, from, text ) => {
            for( users <- ask( userManager, getUsersByRoom( room ) ).mapTo[ List[ User ] ] )
                sendActor ! BroadcastMessage( chatMessageFormat.write( message ).toString(), room, users )
        }

        case _ => println( "Unknown message format: " + message )
    }

    def handleSystemMessage( message: SystemMessage, socket: WebSocket ) = message match {
        case SystemMessage( "login", room, value ) => {
            for ( usr <- ask( userManager, getUserByLogin( value ) ).mapTo[ Option[ User ] ] )
                usr match {
                    case None => {
                        userManager ! login( value, socket )
                        for { users <- ask( userManager, getUsersByRoom( room ) ).mapTo[ List[ User ] ]
                              usr <- ask( userManager, getUserByLogin( value ) ).mapTo[ Option[ User ] ] }
                            for ( user <- usr ) sendActor ! BroadcastWelcomeMessage( user, room, users )
                    }
                    case Some( user ) =>
                        sendActor ! SendLoginErrorMessage( socket )
                }

        }

        case SystemMessage( "logout", room, value ) => {
            for { usr <- ask( userManager, getUserByLogin( value ) ).mapTo[ Option[ User ] ]
                  users <- ask( userManager, getUsersByRoom( room ) ).mapTo[ List[ User ] ] }
                for ( user <- usr ) sendActor ! BroadcastExitMessage( user, room, users )
        }

        case SystemMessage( "user-list", room, value ) => {
            for { usr <- ask( userManager, getUserByLogin( value ) ).mapTo[ Option[ User ] ]
                  users <- ask( userManager, getUsersByRoom( room ) ).mapTo[ List[ User ] ] }
                for ( user <- usr ) sendActor ! SendUserList( user, room, users )
        }

        case _ => println( "Unknown message format" + message )
    }
}
