package io.wisesystems.stream

import akka.actor.ActorLogging
import akka.stream.actor.{OneByOneRequestStrategy, ActorSubscriber}
import akka.stream.scaladsl.ImplicitFlowMaterializer

/**
 * Created by homer on 2/15/15.
 */
class Scheduler extends ActorSubscriber with ImplicitFlowMaterializer with ActorLogging{
  override def requestStrategy=OneByOneRequestStrategy
  override def receive= {
   //TBD a flow to output (e.g., notification and monitoring)
  }
}
