package br.com.diegosilva.database.streamer.repo

import br.com.diegosilva.database.streamer.repo.PostgresProfile.api._
import slick.lifted.ProvenShape

case class TableStreams(id: Option[Long],
                        title: String,
                        description: Option[String],
                        table: String,
                        topic: String,
                        eventType: String)

class TableStreamsTable(tag: Tag) extends Table[TableStreams](tag, "table_streams") {

  def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

  def title: Rep[String] = column[String]("title")

  def description: Rep[String] = column[String]("description")

  def table: Rep[String] = column[String]("table")

  def topic: Rep[String] = column[String]("topic")

  def eventType: Rep[String] = column[String]("event_type")

  def * : ProvenShape[TableStreams] = (id.?, title, description.?, table, topic, eventType) <> (TableStreams.tupled, TableStreams.unapply)

}

object TableStreamsRepo {
  val deliveredMessages = TableQuery[TableStreamsTable]

  def add(deliveredMessage: TableStreams): DBIO[TableStreams] = {
    deliveredMessages returning deliveredMessages += deliveredMessage
  }

}