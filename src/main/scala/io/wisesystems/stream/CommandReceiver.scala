package io.wisesystems.stream

import com.google.common.util.concurrent.AbstractScheduledService.Scheduler

import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.util.ByteString
import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{OnCompleteSink, Source, Sink}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.scalac.amqp.{Connection, Message, Queue}
import io.scalac.amqp.{Direct, Exchange, Queue}


object RabbitRegistry {
  val inboundExchange = Exchange("wise.inbound.event_exchange", Direct, true)
  val inboundQueue = Queue("wise.inbound.event_queue")
}

/**
 * Command receiver
 * Created by homer on 2/20/15.
 */
class CommandReceiver  extends App with FlowFactory with LazyLogging {

  implicit val actorSystem = ActorSystem("wise")

  import actorSystem.dispatcher

  implicit val materializer = FlowMaterializer()

  val connection = Connection()

  setupRabbit() onComplete {
    case Success(_) =>
      logger.info("Exchanges, queues and bindings declared successfully.")

      // source is a rabbit consumer
      val rabbitConsumer = Source(connection.consume(RabbitRegistry.inboundQueue.name))


      // a flow graph, sink to the scheduler which is a actor subscriber
      val subscriber=ActorSubscriber(system.actorOf(Props[Scheduler],"Scheduler"))
      val flow = rabbitConsumer to subscriber   //add via proceesses

      logger.info("Starting the flow")
      flow.run()

      logger.info("Starting the trial run")
      trialRun()
    case Failure(ex) =>
      logger.error("Failed to declare RabbitMQ infrastructure.", ex)
  }

  def setupRabbit(): Future[List[Queue.BindOk]] =
    Future.sequence(List(
      /* declare and bind inbound exchange and queue */
      Future.sequence {
        connection.exchangeDeclare(RabbitRegistry.inboundExchange) ::
          connection.queueDeclare(RabbitRegistry.inboundQueue) :: Nil
      } flatMap { _ =>
        Future.sequence {
          connection.queueBind(RabbitRegistry.inboundQueue.name, RabbitRegistry.inboundExchange.name, "") :: Nil
        }
      }
    )).map { _.flatten }
}
