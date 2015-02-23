package io.wisesystems

import akka.actor.{ Props, ActorSystem }
import akka.stream.actor.{ ActorSubscriber, ActorPublisher }
import akka.stream.FlowMaterializer
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.wisesystems.stream.{ Scheduler, NotificationGenerator, EventReceiver }

/**
 * Created by homer on 2/22/15.
 */
object Main extends App with LazyLogging {
  implicit val actorSystem = ActorSystem("wise")
  implicit val materializer = FlowMaterializer()
  Pipeline.eventsNetwork(actorSystem)
}

/**
 * Topology of the pipeline network
 */
object Pipeline {
  def eventsNetwork(system: ActorSystem) {
    val eventReceiverActor = system.actorOf(Props[EventReceiver])
    val schedulerActor = system.actorOf(Props[Scheduler])
    //    val notificationGeneratorActor=system.actorOf(Props[NotificationGenerator])
    val eventReceiver = ActorPublisher[String](eventReceiverActor)
    val scheduler = ActorSubscriber[String](schedulerActor)
    system.log.info("Scheduler subscribe EventReceiver")
    eventReceiver.subscribe(scheduler)
  }
}
