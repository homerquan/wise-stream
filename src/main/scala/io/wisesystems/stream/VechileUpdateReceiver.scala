package io.wisesystems.stream

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.Source

object VehicleUpdateReceiver {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("Sys")
    import system.dispatcher

    implicit val materializer = FlowMaterializer()

    //TBD
  }
}
