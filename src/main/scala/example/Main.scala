package example

import java.net.InetAddress

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

// https://www.scala-sbt.org/sbt-native-packager/formats/docker.html
// https://buildmedia.readthedocs.org/media/pdf/sbt-native-packager/latest/sbt-native-packager.pdf
// https://doc.akka.io/docs/akka-http/current/introduction.html

/*
Build in sbt
docker:publishLocal   will publish to local Docker node

Running

docker run -p 8080:8080 scala-akka-kubernetes-example:0.1
curl 192.168.99.100:8080/get  or use browser

make sure no container is already running and binding to the port
docker container list
docker container list --all
docker container prune    will remove all stopped containers
docker image prune        will remove previous not used versions
 */

object Main {
  private val Log: Logger = LoggerFactory.getLogger( this.getClass )

  def main( args: Array[String] ) {
    Log.info( "Starting up Akka Docker container ...")
    Log.info("You can check counter at: http://<IP of your Docker VM>:8080/get ")
    Log.info("<IP of your Docker VM> is usually 192.168.99.100")

    val hostname = InetAddress.getLocalHost().getHostName().toUpperCase()
    val port = 8080

    implicit val system = ActorSystem( "DockerSystem" )
    implicit val executor = system.dispatcher
    implicit val timeout = Timeout(10 seconds)
    implicit val fm = ActorMaterializer()

    val actor = system.actorOf( ExampleActor.props, name = classOf[ExampleActor].getSimpleName )
    system.getScheduler.scheduleOnce( ExampleActor.Delay, actor, "increment" )

    val route: Route =
      get {
        path( "get" ) {
          val future: Future[Any] = actor ? "get"
          onSuccess( future ) { result =>
            complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Count is now ${result}"))
          }
        }
      }

    Log.info( s"Binding HTTP service to ${hostname}:${port}" )
    val bindingFuture = Http().bindAndHandle( route, "0.0.0.0", port )
    Await.result( bindingFuture, 5 minutes )
  }
}
