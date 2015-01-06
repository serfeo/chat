organization := "org.serfeo.dev"

version := "0.1.0"

scalaVersion := "2.10.3"

scalacOptions := Seq( "-unchecked", "-deprecation", "-encoding", "utf8" )

resolvers ++= Seq(
    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    "Spray repository" at "http://repo.spray.io/"
)

libraryDependencies ++= {
    Seq(
        "io.spray" %% "spray-json" % "1.2.5",
        "com.typesafe.akka" %% "akka-actor" % "2.2.3"
    )
}
