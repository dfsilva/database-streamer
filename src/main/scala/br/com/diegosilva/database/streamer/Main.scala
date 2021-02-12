package br.com.diegosilva.database.streamer

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SpawnProtocol}
import akka.actor.{Address, AddressFromURIString}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{Cluster, JoinSeedNodes}
import akka.util.Timeout
import br.com.diegosilva.database.streamer.actors.ListenerActor.Start
import br.com.diegosilva.database.streamer.actors.{ListenerActor, PublisherActor, ResendActor}
import br.com.diegosilva.database.streamer.api.{Routes, Server}
import br.com.diegosilva.database.streamer.db.DbExtension
import br.com.diegosilva.database.streamer.repo.DbStreamRepo
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

  import Main.executionContext

  def apply(): Behavior[SpawnProtocol.Command] = {
    Behaviors.setup { context =>

      val httpPort = context.system.settings.config.getInt("server.http.port")
      val seedNodes: Array[Address] =
        sys.env("SEED_NODES").split(",").map(AddressFromURIString.parse)

      Cluster(context.system).manager ! JoinSeedNodes(seedNodes)

      PublisherActor.init(context.system)

      DbExtension.get(context.system).db().run(DbStreamRepo.list()).map(streams => {
        streams.foreach(dbStream => {
          ClusterSharding(context.system).entityRefFor(PublisherActor.EntityKey, dbStream.topic) ! PublisherActor.ProcessMessages()
        })
      })

      Server(Routes(), httpPort, context.system).start()

      val resendActor:ActorRef[ResendActor.Command] = context.spawn(ResendActor(), "resend-actor")
      val listenerActor:ActorRef[ListenerActor.Command] =  context.spawn(ListenerActor(resendActor), "listener-actor")

      resendActor ! ResendActor.Start(listenerActor)

      SpawnProtocol()
    }
  }
}
