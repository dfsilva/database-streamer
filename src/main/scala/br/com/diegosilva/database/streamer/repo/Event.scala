package br.com.diegosilva.database.streamer.repo

import br.com.diegosilva.database.streamer.repo.PostgresProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp

case class Event(id: Option[Long],
                 createTime: Timestamp,
                 topic: String,
                 body: String)

class EventsTable(tag: Tag) extends Table[Event](tag, "events") {

  def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

  def createTime: Rep[Timestamp] = column[Timestamp]("create_time")

  def topic: Rep[String] = column[String]("topic")

  def body: Rep[String] = column[String]("body")

  def * : ProvenShape[Event] = (id.?, createTime, topic, body) <> (Event.tupled, Event.unapply)

}

object EventsTableRepo {
  val table = TableQuery[EventsTable]

  def add(event: Event): DBIO[Event] = {
    table returning table += event
  }

}