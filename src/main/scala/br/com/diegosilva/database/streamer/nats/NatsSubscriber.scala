package br.com.diegosilva.database.streamer


import br.com.diegosilva.database.streamer.NatsSubscriber.log
import io.nats.streaming.{Message, StreamingConnection, Subscription, SubscriptionOptions}
import org.slf4j.LoggerFactory

object NatsSubscriber {
  private val log = LoggerFactory.getLogger(NatsSubscriber.getClass)
  private var subscribers = Map[String, Subscription]()

  def apply(connection: StreamingConnection,
            queue: String,
            uuid: String): NatsSubscriber = {
    if (subscribers.contains(uuid)) {
      subscribers(uuid).unsubscribe()
      val subscriber = new NatsSubscriber(connection, queue, uuid)
      subscribers = (subscribers + (uuid -> subscriber.subscription))
      subscriber
    } else {
      val subscriber = new NatsSubscriber(connection, queue, uuid)
      subscribers = (subscribers + (uuid -> subscriber.subscription))
      subscriber
    }
  }
}

class NatsSubscriber(connection: StreamingConnection, queue: String, uuid: String) {

  import br.com.diegosilva.database.streamer.Main._

  log.info(s"Subscrevendo na fila $queue uid $uuid")
  val subscription = connection.subscribe(queue, (msg: Message) => {
    log.info(s"Recebeu mensagem $msg na fila $queue")

  }, new SubscriptionOptions.Builder().durableName(s"durable_$uuid").manualAcks().build())
}
