package br.com.diegosilva.database.streamer.repo

import br.com.diegosilva.database.streamer.repo.PostgresProfile.api._
import slick.lifted.ProvenShape

case class StreamTbl(id: Option[Long],
                     title: String,
                     description: Option[String],
                     table: String,
                     topic: String)

class StreamsTable(tag: Tag) extends Table[StreamTbl](tag, "streams") {

  def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

  def title: Rep[String] = column[String]("title")

  def description: Rep[String] = column[String]("description")

  def table: Rep[String] = column[String]("table_name")

  def topic: Rep[String] = column[String]("topic")

  def * : ProvenShape[StreamTbl] = (id.?, title, description.?, table, topic) <> (StreamTbl.tupled, StreamTbl.unapply)

}

object StreamTblRepo {
  val table = TableQuery[StreamsTable]

  def add(stream: StreamTbl): DBIO[StreamTbl] = {
    table returning table += stream
  }

}