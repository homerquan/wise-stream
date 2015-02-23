package io.wisesystems.stream

import akka.actor.ActorSystem
import akka.stream.actor.{ ActorPublisher, ActorSubscriber }
import scala.concurrent.Future
import scala.util.{ Failure, Success }
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{ OnCompleteSink, Source, Sink }
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.scalac.amqp.{ Connection, Message, Queue }
import io.scalac.amqp.{ Direct, Exchange, Queue }
import akka.stream.actor.ActorPublisherMessage.Request
import akka.stream.actor.ActorPublisherMessage.Cancel

/**
 * Event receiver
 * Receive data from mq and publish to follow actors
 * Created by homer on 2/20/15.
 */
class EventReceiver extends ActorPublisher[String] with LazyLogging {

  def receive = {
    case Cancel =>
      logger.info("[EventReceiver] cancel message received")
      context.stop(self)
    case _ =>
  }

  implicit val actorSystem = ActorSystem("wise")
  import actorSystem.dispatcher
  implicit val materializer = FlowMaterializer()

  val connection = Connection()
  val inboundExchange = Exchange("wise.inbound.event_exchange", Direct, true)
  val inboundQueue = Queue("wise.inbound.event_queue")

  setupRabbit() onComplete {
    case Success(_) =>
      logger.info("Exchanges, queues and bindings declared successfully.")
      // source is a rabbit consumer
      val rabbitConsumer = Source(connection.consume(inboundQueue.name))
      // a flow graph, sink to the scheduler which is a actor subscriber
      logger.info("Starting the flow")
      rabbitConsumer.map { elem => println(elem); elem }.to(Sink.publisher).run()
    case Failure(ex) =>
      logger.error("Failed to declare RabbitMQ infrastructure.", ex)
  }

  def setupRabbit(): Future[List[Queue.BindOk]] =
    Future.sequence(List(
      /* declare and bind inbound exchange and queue */
      Future.sequence {
        connection.exchangeDeclare(inboundExchange) ::
          connection.queueDeclare(inboundQueue) :: Nil
      } flatMap { _ =>
        Future.sequence {
          connection.queueBind(inboundQueue.name, inboundExchange.name, "") :: Nil
        }
      }
    )).map {
      _.flatten
    }
}
