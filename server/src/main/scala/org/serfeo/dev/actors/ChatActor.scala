package org.serfeo.dev.actors

import akka.actor.{ActorRef, Props, Actor}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import org.java_websocket.WebSocket
import spray.json._

import scala.concurrent.duration._

object ChatActor {
    import org.serfeo.dev.actors.UsersManager.User

    case class TypeMessage( action: String )
    case class ChatMessage( action: String, room: Int, to: String, from: String, text: String )
    case class SystemMessage( action: String, room: Int, value: String )
    case class SystemListMessage( action: String, room: Int, value: Iterable[ String ] )

    case class StartPrivateRoomMessage( action: String, room: Int, user1: String, user2: String )
    case class PrivateRoomStarting( action: String, room: Int, user1: User, user2: User )

    object ChatMessageJsonFormat extends DefaultJsonProtocol {
        implicit val typeMessageFormat = jsonFormat1( TypeMessage )
        implicit val chatMessageFormat = jsonFormat5( ChatMessage )
        implicit val systemMessageFormat = jsonFormat3( SystemMessage )
        implicit val systemListMessageFormat = jsonFormat3( SystemListMessage )
        implicit val startPrivateRoomMessageFormat = jsonFormat4( StartPrivateRoomMessage )
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
                case TypeMessage ( "start-private" ) => handlePrivateRoomMessage( JsonParser( m.msg ).convertTo[ StartPrivateRoomMessage ] );
                case _ => handleSystemMessage( JsonParser( m.msg ).convertTo[ SystemMessage ], m.ws )
            }
        }

        case _ => println( "Unknown message format in ChatActor receive" )
    }

    def handlePrivateRoomMessage( message: StartPrivateRoomMessage ) = message match {
        case StartPrivateRoomMessage( "start-private", room, user1, user2 ) => {
            for ( startPrivateRoom <- ask( userManager, createNewPrivateRoom( user1, user2 ) ).mapTo[ PrivateRoomStarting ] ) {
                val msg = startPrivateRoomMessageFormat.write( StartPrivateRoomMessage( "start-private", startPrivateRoom.room, startPrivateRoom.user1.login, startPrivateRoom.user2.login ) ).toString
                sendActor ! BroadcastMessage( msg, defaultRoom, List( startPrivateRoom.user1, startPrivateRoom.user2 ) )
            }
        }

        case _ => println( "Unknown message format: " + message )
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
