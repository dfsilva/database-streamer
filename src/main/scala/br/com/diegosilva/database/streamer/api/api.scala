package br.com.diegosilva.database.streamer

package object api {

  case class AddTableStream(title: String,
                            description: Option[String],
                            table: String,
                            topic: String) extends CborSerializable


}
