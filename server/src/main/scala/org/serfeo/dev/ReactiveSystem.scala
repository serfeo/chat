package org.serfeo.dev

import akka.actor.{ActorSystem, Props}
import org.serfeo.dev.actors.ChatActor

object ReactiveSystem extends App {
    implicit lazy val system = ActorSystem( "chat" )

    private val rs = new ReactiveServer( Configuration.portWs )
    rs.forResource( "/chat", Some( system.actorOf( Props[ ChatActor ] ) ) )
    rs.start

    sys.addShutdownHook( { system.shutdown; rs.stop } )
}

object Configuration {
    import com.typesafe.config.ConfigFactory

    private val config = ConfigFactory.load
    config.checkValid( ConfigFactory.defaultReference )

    val host = config.getString("chat.host")
    val portWs   = config.getInt("chat.ports.ws")
}
