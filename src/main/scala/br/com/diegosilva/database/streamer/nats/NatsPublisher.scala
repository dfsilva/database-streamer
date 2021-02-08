package br.com.diegosilva.database.streamer.nats


import br.com.diegosilva.database.streamer.nats.NatsPublisher.log
import io.nats.streaming.StreamingConnection
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, Promise}

object NatsPublisher {
  private val log = LoggerFactory.getLogger(NatsPublisher.getClass)

  def apply(connection: StreamingConnection, queue: String, content: String): Future[String] = new NatsPublisher(connection, queue, content).publish
}

class NatsPublisher(connection: StreamingConnection, queue: String, content: String) {

  import br.com.diegosilva.database.streamer.Main.executionContext

  def publish: Future[String] = {
    val p = Promise[String]()
    Future {
      log.info(s"Publicando na fila na fila $queue")

      throw new RuntimeException("teste erro")
      connection.publish(queue, content.getBytes, (nuid: String, ex: Exception) => {
        if (ex != null)
          p.failure(ex)
        else
          p.success(nuid)
      })
    }
    p.future
  }
}
