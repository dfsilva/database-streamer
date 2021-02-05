package br.com.diegosilva.database.streamer.repo

import akka.persistence.jdbc.db.SlickExtension
import com.zaxxer.hikari.HikariDataSource
import slick.jdbc.hikaricp.HikariCPJdbcDataSource

object TriggersRepo {

  import br.com.diegosilva.database.streamer.Main._

  def createTrigger(table: String): Unit = {

    val database = SlickExtension(system).database(system.settings.config.getConfig("jdbc-journal"))
    val dataSource: HikariDataSource = database.database.source.asInstanceOf[HikariCPJdbcDataSource].ds

    dataSource.getConnection



  }


}
