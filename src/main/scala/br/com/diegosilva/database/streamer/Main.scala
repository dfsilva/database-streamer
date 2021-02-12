package br.com.diegosilva.database.streamer

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SpawnProtocol}
import akka.actor.{Address, AddressFromURIString}
import akka.cluster.typed.{Cluster, JoinSeedNodes}
import akka.util.Timeout
import br.com.diegosilva.database.streamer.actors.{ListenerActor, ProcessActor, ResendActor}
import br.com.diegosilva.database.streamer.api.{Routes, Server}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object Main extends App {
  implicit val system = ActorSystem[SpawnProtocol.Command](Guardian(), "DatabaseStreamerSystem", ConfigFactory.load)
  implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("server.askTimeout"))
  implicit val scheduler = system.scheduler
  implicit val executionContext = system.executionContext
  implicit val classicSystem = system.classicSystem

}

object Guardian {
  private val log = LoggerFactory.getLogger(Guardian.getClass)

  def apply(): Behavior[SpawnProtocol.Command] = {
    Behaviors.setup { context =>

      val httpPort = context.system.settings.config.getInt("server.http.port")
      val seedNodes: Array[Address] =
        sys.env("SEED_NODES").split(",").map(AddressFromURIString.parse)

      Cluster(context.system).manager ! JoinSeedNodes(seedNodes)

      Server(Routes(), httpPort, context.system).start()

      val processActor: ActorRef[ProcessActor.Command] = context.spawn(ProcessActor(), "process-actor")

      val resendActor: ActorRef[ResendActor.Command] = context.spawn(ResendActor(processActor), "resend-actor")
      val listenerActor: ActorRef[ListenerActor.Command] = context.spawn(ListenerActor(resendActor, processActor), "listener-actor")

      resendActor ! ResendActor.Start(0, listenerActor)

      SpawnProtocol()
    }
  }
}
