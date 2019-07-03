package example

import java.net.InetAddress

import akka.actor.{Actor, ActorLogging, Props}
import example.ExampleActor.{EventCalculate, EventCalculateResponse, EventGet, EventGetResponse, EventIncrement}
import net.liftweb.json.{DefaultFormats, Serialization}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object ExampleActor {
  def props : Props = Props[ExampleActor]

  val Delay = 3 second

  case class EventIncrement( add: Integer )
  case class EventGet( id: Int )
  case class EventGetResponse( id: Int, count: Integer, hostname: String )
  case class EventCalculate( json: String )
  case class EventCalculateResponse( json: String )
}

class ExampleActor extends  Actor with ActorLogging {
  implicit val executor = ExecutionContext.global
  implicit val formats = DefaultFormats
  val hostname = InetAddress.getLocalHost().getHostName().toUpperCase()

  def logInfo( count: Int ): Int = {
    log.info( s"Count is now ${count}" )
    count
  }

  override def receive: Receive =  (logInfo _ andThen increment)(0)

  def parse( json: String ): Option[ List[CalculationRequest] ] = {
    val result = try {
      val parsedResult = Serialization.read[List[CalculationRequest]](json)
      Some( parsedResult )
    } catch {
      case e => None
    }
    result
  }

  def increment( count: Int ): Receive = {
    case EventIncrement( add )  =>
      context.system.getScheduler.scheduleOnce( ExampleActor.Delay, self, EventIncrement( 1 )  )
      context.become( (logInfo _ andThen increment)( count + add ) )

    case EventGet( id ) =>
      log.info( s"Returning count: ${count} from ${hostname}" )
      sender ! EventGetResponse( id, count, hostname )

    case EventCalculate( json ) =>
      parse( json ) match {
        case Some( requests ) =>
          val results = for{
            request <- requests
          } yield new CalculationResult( request.id, request.param_1 * request.param_2 )
          log.info( s"Returning results: ${results} from ${hostname}" )
          val responseJson = Serialization.write( results )
          sender ! EventCalculateResponse( responseJson )
        case None =>
          sender ! EventCalculateResponse( s"""{ "message": "Unknown request" }""" )
      }

  }
}
