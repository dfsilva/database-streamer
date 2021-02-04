package br.com.diegosilva.database.streamer.migration

import akka.actor.typed.{ActorSystem, Extension, ExtensionId}
import akka.persistence.jdbc.db.SlickExtension
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import slick.jdbc.hikaricp.HikariCPJdbcDataSource

import scala.util.{Failure, Success, Try}

class MigrationExtensiomImpl(system: ActorSystem[_]) extends Extension

object MigrationExtension extends ExtensionId[MigrationExtensiomImpl] {

  private val log = LoggerFactory.getLogger(MigrationExtension.getClass)

  override def createExtension(system: ActorSystem[_]): MigrationExtensiomImpl = {

    val database = SlickExtension(system).database(system.settings.config.getConfig("jdbc-journal"))

    val flyway = Flyway.configure()
      .dataSource(database.database.source.asInstanceOf[HikariCPJdbcDataSource].ds).load()

    Try(flyway.migrate()) match {
      case Success(_) => log.info("Migration success")
      case Failure(e) =>
        log.error("Migration failed", e)
    }
    new MigrationExtensiomImpl(system)
  }

  def get(system: ActorSystem[_]): MigrationExtensiomImpl = apply(system)
}
