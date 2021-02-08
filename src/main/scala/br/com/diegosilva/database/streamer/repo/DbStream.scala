package br.com.diegosilva.database.streamer.repo

import br.com.diegosilva.database.streamer.repo.PostgresProfile.api._
import slick.lifted.ProvenShape

case class DbStream(topic: String,
                    table: String,
                    schema: String,
                    title: String,
                    description: Option[String],
                    delete: Boolean = false,
                    insert: Boolean = false,
                    update: Boolean = false)

class DbStreamTable(tag: Tag) extends Table[DbStream](tag, "streams") {


  def topic: Rep[String] = column[String]("topic", O.PrimaryKey)

  def table: Rep[String] = column[String]("stream_table")

  def schema: Rep[String] = column[String]("stream_schema")

  def title: Rep[String] = column[String]("title")

  def description: Rep[String] = column[String]("description")

  def delete: Rep[Boolean] = column[Boolean]("delete")

  def insert: Rep[Boolean] = column[Boolean]("insert")

  def update: Rep[Boolean] = column[Boolean]("update")

  def * : ProvenShape[DbStream] = (topic, table, schema, title, description.?, delete, insert, update) <> (DbStream.tupled, DbStream.unapply)

}

object DbStreamRepo {
  val table = TableQuery[DbStreamTable]

  def add(stream: DbStream): DBIO[DbStream] = {
    table returning table += stream
  }

  def list(): DBIO[Seq[DbStream]] = {
    table.result
  }

  def delete(topic: String): DBIO[Int] = {
    table.filter(_.topic === topic).delete
  }

}