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
import example.ExampleActor._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object Main {
  private val Log: Logger = LoggerFactory.getLogger( this.getClass )

  def main( args: Array[String] ) {
    Log.info( "Starting up Akka Docker container ...")

    val hostname = InetAddress.getLocalHost().getHostName().toUpperCase()
    val port = 9090
    val interface = "0.0.0.0"

    implicit val system = ActorSystem( "DockerSystem" )
    implicit val executor = system.dispatcher
    implicit val timeout = Timeout(10 seconds)
    implicit val fm = ActorMaterializer()

    val actor = system.actorOf( ExampleActor.props, name = classOf[ExampleActor].getSimpleName )
    system.getScheduler.scheduleOnce( ExampleActor.Delay, actor, EventIncrement( 1 ) )

    def getMethod( id: Int = 0 ) = {
      val future: Future[Any] = actor ? EventGet( id )
      onSuccess( future ) { result =>
        result match {
          case EventGetResponse( id, count, hostname ) =>
            val forId = if( id != 0 ) s""" , "id": ${id} """ else ""
            complete(HttpEntity(ContentTypes.`application/json`, s"""{ "count": ${count} , "hostname": "${hostname}" ${forId} }"""))
          case _ =>
            complete(HttpEntity(ContentTypes.`application/json`, s"""{ "message": "Unknown request" }"""))
        }
      }
    }

    def postMethod( requestJson: String ) = {
      val future: Future[Any] = actor ? EventCalculate( requestJson )
      onSuccess( future ) { result =>
        result match {
          case EventCalculateResponse( json ) =>
            complete(HttpEntity(ContentTypes.`application/json`, json ))
          case _ =>
            complete(HttpEntity(ContentTypes.`application/json`, s"""{ "message": "Unknown request" }"""))
        }
      }
    }

    val route: Route =
      get {
        path( "get"  ) {
          // using 0 as default if parameter 'id' is not provided in the request
          parameters( 'id.as[Int] ? 0 ) { id =>
            getMethod( id )
          }
        }
      } ~
      post {
        path( "calculate" ) {
          entity(as[String]) { entity =>
            postMethod( entity )
          }
        }
      }

    Log.info( s"Binding HTTP service to ${hostname}:${port} on interface '${interface}'" )
    val bindingFuture = Http().bindAndHandle( route, interface, port )
    Await.result( bindingFuture, 60 minutes )
  }
}
