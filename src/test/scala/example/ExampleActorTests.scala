package example

import java.net.InetAddress

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import example.ExampleActor.{EventCalculate, EventCalculateResponse, EventGet, EventGetResponse}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.slf4j.{Logger, LoggerFactory}

class ExampleActorTests extends TestKit(ActorSystem("TestDockerSystem"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  private val Log: Logger = LoggerFactory.getLogger( this.getClass )

  val hostname = InetAddress.getLocalHost().getHostName().toUpperCase()

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Example actor" must {
    "reply to EventGet without id parameter" in {
      val echo = system.actorOf(ExampleActor.props)
      Log.info( "Testing EventGet(0) ")
      echo ! EventGet( 0 )
      expectMsg( EventGetResponse( 0, 0, hostname ) )
    }
  }

  "Example actor" must {
    "reply to EventGet with id parameter" in {
      val echo = system.actorOf(ExampleActor.props)
      val id = 15
      Log.info( s"Testing EventGet(${id}) ")
      echo ! EventGet( id )
      expectMsg( EventGetResponse( id, 0, hostname ) )
    }
  }

  "Example actor" must {
    "reply to EventCalculate with incorrect json" in {
      val echo = system.actorOf(ExampleActor.props)
      val json = "{ foo bar ["
      Log.info( s"Testing json: ${json} ")
      echo ! EventCalculate( json )
      expectMsg( EventCalculateResponse( s"""{ "message": "Unknown request" }""" ) )
    }
  }

  "Example actor" must {
    "reply to EventCalculate with empty json" in {
      val echo = system.actorOf(ExampleActor.props)
      val json = "{}"
      Log.info( s"Testing json: ${json} ")
      echo ! EventCalculate( json )
      expectMsg( EventCalculateResponse( s"""{ "message": "Unknown request" }""" ) )
    }
  }

  "Example actor" must {
    "reply to EventCalculate with empty request list" in {
      val echo = system.actorOf(ExampleActor.props)
      val jsonRequest = s"""[]"""
      val jsonResponse = s"""[]"""
      Log.info( s"Testing json: ${jsonRequest} ")
      echo ! EventCalculate( jsonRequest )
      expectMsg( EventCalculateResponse( jsonResponse ) )
    }
  }

  "Example actor" must {
    "reply to EventCalculate with single request" in {
      val echo = system.actorOf(ExampleActor.props)
      val id = 7
      val param_1 = 14
      val param_2 = 32
      val calcResult = param_1 * param_2
      val jsonRequest = s"""[ { "id": ${id}, "param_1": ${param_1}, "param_2": ${param_2} } ]"""
      val jsonResponse = s"""[{"id":${id},"result":${calcResult}}]"""
      Log.info( s"Testing json: ${jsonRequest} ")
      echo ! EventCalculate( jsonRequest )
      expectMsg( EventCalculateResponse( jsonResponse ) )
    }
  }

  "Example actor" must {
    "reply to EventCalculate with multiple requests" in {
      val echo = system.actorOf(ExampleActor.props)
      val id = 7
      val param_1 = 14
      val param_2 = 32
      val calcResult = param_1 * param_2
      val id_2 = 15
      val param_3 = 11
      val param_4 = 25
      val calcResult_2 = param_3 * param_4
      val jsonRequest = s"""[ { "id": ${id}, "param_1": ${param_1}, "param_2": ${param_2} } ,""" +
        s""" { "id": ${id_2}, "param_1": ${param_3}, "param_2": ${param_4} } ]"""
      val jsonResponse = s"""[{"id":${id},"result":${calcResult}},{"id":${id_2},"result":${calcResult_2}}]"""
      Log.info( s"Testing json: ${jsonRequest} ")
      echo ! EventCalculate( jsonRequest )
      expectMsg( EventCalculateResponse( jsonResponse ) )
    }
  }
}
