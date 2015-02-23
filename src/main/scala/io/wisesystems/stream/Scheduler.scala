package io.wisesystems.stream

import akka.stream.actor.{ OneByOneRequestStrategy, ActorSubscriber }
import akka.stream.actor.ActorSubscriberMessage.{ OnNext, OnError }
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
 * Created by homer on 2/15/15.
 */
class Scheduler extends ActorSubscriber with LazyLogging {
  val requestStrategy = OneByOneRequestStrategy
  def receive = {
    case OnNext(event: String) =>
      logger.info("[Scheduler] received an event: {}", event)
      //TBD schedule policy
    case OnError(err: Exception) =>
      logger.error("Error in Scheduler", err)
      context.stop(self)
    case _ =>
  }
}