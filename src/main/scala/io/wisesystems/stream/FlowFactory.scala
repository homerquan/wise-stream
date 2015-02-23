package io.wisesystems.stream
import scala.concurrent.ExecutionContext

import akka.stream.scaladsl.Flow
import akka.util.ByteString

import com.typesafe.scalalogging.slf4j.LazyLogging

import io.scalac.amqp._


/**
 * This is our flow factory.
 *
 * Here we are only applying some simple filtering, logging and mapping,
 * but the idea is that this part as the meat of your application.
 *
 * Depending on your domain you could for example call some external services or actors here.
 */
trait FlowFactory extends LazyLogging {

  val inboundExchange = Exchange("wise.inbound.event_exchange", Fanout, true)
  val inboundQueue = Queue("wise.inbound.event_queue")
  val outboundExchange = Exchange("wise.outbound.exchange", Direct, true)
  val outOkQueue = Queue("wise.ok.queue")
  val outNokQueue = Queue("wise.nok.queue")

  /** Flow responsible for mapping the incoming RabbitMQ message to our domain input. */
  def consumerMapping: Flow[Delivery, ByteString] =
    Flow[Delivery].map(_.message.body)

  /** Flow performing our domain processing. */
  def domainProcessing(implicit ex: ExecutionContext): Flow[ByteString, CensoredMessage] =
    Flow[ByteString].

      // to string
      map { _.utf8String }.

      map {elem => println(elem); elem}.

      // do something time consuming
      mapAsync { DomainService.expensiveCall }.

      // classify message
      map { DomainService.classify }

  /** Flow responsible for mapping the domain processing result into a RabbitMQ message. */
  def publisherMapping: Flow[CensoredMessage, Routed] =
    Flow[CensoredMessage] map(cMsg => cMsg match {
      case MessageSafe(msg) => Routed(routingKey = outOkQueue.name, Message(body = ByteString(cMsg.message)))
      case MessageThreat(msg) => Routed(routingKey = outNokQueue.name, Message(body = ByteString(cMsg.message)))
    })

}