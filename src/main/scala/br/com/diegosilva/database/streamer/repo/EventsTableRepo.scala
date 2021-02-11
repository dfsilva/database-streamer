package br.com.diegosilva.database.streamer.repo

import br.com.diegosilva.database.streamer.repo.PostgresProfile.api._
import slick.jdbc.GetResult
import slick.lifted.ProvenShape

import java.sql.Timestamp

case class Event(id: Option[Long],
                 createTime: Timestamp,
                 topic: String,
                 old: Option[String],
                 current: Option[String])

class EventsTable(tag: Tag) extends Table[Event](tag, "events") {

  def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

  def createTime: Rep[Timestamp] = column[Timestamp]("create_time")

  def topic: Rep[String] = column[String]("topic")

  def old: Rep[String] = column[String]("old")

  def current: Rep[String] = column[String]("current")

  def * : ProvenShape[Event] = (id.?, createTime, topic, old.?, current.?) <> (Event.tupled, Event.unapply)

}

object EventsTableRepo {
  val table = TableQuery[EventsTable]

  implicit val getResultEvent = GetResult(r => Event(r.<<, r.<<, r.<<, r.<<, r.<<))

  def add(event: Event): DBIO[Event] = {
    table returning table += event
  }

  def delete(id: Long): DBIO[Int] = {
    table.filter(_.id === id).delete
  }

  def lastEvent(): DBIO[Option[Event]] = {
    sql"""select e.id, e.create_time, e.topic, e.old, e.current
      from events e order by id desc limit 1""".as[Event].headOption
  }

  def pendingMessages(limit: Int): DBIO[Vector[Event]] = {
    sql"""select e.id, e.create_time, e.topic, e.old, e.current
      from events e order by id asc limit $limit""".as[Event]
  }

}