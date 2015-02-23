package io.wisesystems.stream

import akka.actor.ActorSystem
import akka.stream.actor.{ ActorPublisher, ActorSubscriber }
import akka.util.ByteString
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
class EventReceiver extends ActorPublisher[String] with FlowFactory with LazyLogging {

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

  setupRabbit() onComplete {
    case Success(_) =>
      logger.info("Exchanges, queues and bindings declared successfully.")

      val rabbitConsumer = Source(connection.consume(inboundQueue.name))
      val rabbitPublisher = Sink(connection.publish(outboundExchange.name))

      val flow = rabbitConsumer via consumerMapping via domainProcessing via publisherMapping to Sink.publisher

      logger.info("Starting the flow")
      flow.run()

    //      logger.info("Starting the trial run")
    //      trialRun()
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
      },

      /* declare and bind outbound exchange and queues */
      Future.sequence {
        connection.exchangeDeclare(outboundExchange) ::
          connection.queueDeclare(outOkQueue) ::
          connection.queueDeclare(outNokQueue) :: Nil
      } flatMap { _ =>
        Future.sequence {
          connection.queueBind(outOkQueue.name, outboundExchange.name, outOkQueue.name) ::
            connection.queueBind(outNokQueue.name, outboundExchange.name, outNokQueue.name) :: Nil
        }
      }
    )).map { _.flatten }

  /**
   * Trial run of couple of messages just to show that streaming through RabbitMQ actually works here.
   *
   * We're setting up two streams here:
   * 1. Stream publishing trial messages to RabbitMQ inbound exchange.
   * 2. Stream consuming the trial messages from one of the outbound queues.
   *
   * Both streams are set up to die after performing their purpose.
   */
  def trialRun() = {
    val trialMessages = "{\"truckId\":\"DKBH-86\",\"velocity\":\"0.0\",\"lat\":\"-33.45944\",\"lon\":\"-70.62888\",\"dateNum\":\"312\",\"hour\":\"14\",\"minute\":\"44\",\"second\":\"18\"}" ::
      "{\"truckId\":\"DKBH-86\",\"velocity\":\"0.0\",\"lat\":\"-33.45943\",\"lon\":\"-70.62887\",\"dateNum\":\"312\",\"hour\":\"14\",\"minute\":\"45\",\"second\":\"18\"}" ::
      Nil

    /* publish couple of trial messages to the inbound exchange */
    Source(trialMessages).
      map(msg => Message(ByteString(msg))).
      runWith(Sink(connection.publish(inboundExchange.name, "")))

    /* log the trial messages consumed from the queue */
    Source(connection.consume(outOkQueue.name)).
      take(trialMessages.size).
      map(msg => logger.info(s"'${msg.message.body.utf8String}' delivered to ${outOkQueue.name}")).
      runWith(new OnCompleteSink({
        case Success(_)  => logger.info("Trial run finished. You can now go to http://localhost:15672/ and try publishing messages manually.")
        case Failure(ex) => logger.error("Trial run finished with error.", ex)
      }))
  }
}
