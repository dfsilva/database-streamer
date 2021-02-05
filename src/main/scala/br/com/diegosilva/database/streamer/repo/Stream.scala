package br.com.diegosilva.database.streamer.repo

import br.com.diegosilva.database.streamer.repo.PostgresProfile.api._
import slick.lifted.ProvenShape

case class Stream(id: Option[Long],
                  title: String,
                  description: Option[String],
                  table: String,
                  topic: String)

class StreamsTable(tag: Tag) extends Table[Stream](tag, "streams") {

  def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

  def title: Rep[String] = column[String]("title")

  def description: Rep[String] = column[String]("description")

  def table: Rep[String] = column[String]("table_name")

  def topic: Rep[String] = column[String]("topic")

  def * : ProvenShape[Stream] = (id.?, title, description.?, table, topic) <> (Stream.tupled, Stream.unapply)

}

object StreamsRepo {
  val table = TableQuery[StreamsTable]

  def add(stream: Stream): DBIO[Stream] = {
    table returning table += stream
  }

}