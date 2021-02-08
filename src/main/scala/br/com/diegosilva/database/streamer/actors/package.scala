package br.com.diegosilva.database.streamer

package object actors {

  case class DatabaseNotification(id: Long,
                                  time: String,
                                  topic: String,
                                  old: Option[String],
                                  current: Option[String]) extends CborSerializable

  case class NatsNotification(
                               old: Option[String],
                               current: Option[String]) extends CborSerializable
}
