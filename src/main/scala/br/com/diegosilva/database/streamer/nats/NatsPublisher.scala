package br.com.diegosilva.database.streamer.nats


import br.com.diego.silva.nats.NatsStreamConnectionWrapper
import org.slf4j.LoggerFactory

import scala.compat.java8.FutureConverters
import scala.concurrent.Future

object NatsPublisher {
  private val log = LoggerFactory.getLogger(NatsPublisher.getClass)

  def apply(connection: NatsStreamConnectionWrapper, queue: String, content: String): Future[String] = new NatsPublisher(connection, queue, content).publish
}

class NatsPublisher(connection: NatsStreamConnectionWrapper, queue: String, content: String) {

  def publish: Future[String] = FutureConverters.toScala(connection.publishAsync(queue, content.getBytes))
}
