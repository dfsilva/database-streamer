package br.com.diegosilva.database.streamer

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, SpawnProtocol}
import akka.actor.{Address, AddressFromURIString}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{Cluster, JoinSeedNodes}
import akka.persistence.jdbc.db.SlickExtension
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import slick.jdbc.hikaricp.HikariCPJdbcDataSource

import scala.util.{Failure, Success, Try}

object Main extends App {
  implicit val system = ActorSystem[SpawnProtocol.Command](Guardian(), "MetamorphosisSystem", ConfigFactory.load)
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

      val database = SlickExtension(context.system).database(context.system.settings.config.getConfig("jdbc-journal"))

      val flyway = Flyway.configure()
        .dataSource(database.database.source.asInstanceOf[HikariCPJdbcDataSource].ds).load()

      Try(flyway.migrate()) match {
        case Success(_) => log.error("Migration success")
        case Failure(e) =>
          log.error("Migration failed", e)
      }

      SpawnProtocol()
    }
  }
}
