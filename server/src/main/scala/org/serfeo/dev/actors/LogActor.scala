package org.serfeo.dev.actors

import akka.actor.Actor

class LogActor extends Actor {
    def receive = {
        case m: String => output( m )
        case _ => output( "Unknown message format" )
    }

    def output( msg: String ) = {
        println( msg );
    }
}
