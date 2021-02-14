package br.com.diegosilva.database.streamer.nats

import akka.actor.typed.{ActorSystem, Extension, ExtensionId}
import com.typesafe.config.Config
import io.nats.client.{Connection, ConnectionListener, Nats}
import io.nats.streaming.{Options, StreamingConnection, StreamingConnectionFactory}
import org.slf4j.LoggerFactory

class NatsConnectionExtensionImpl(config: Config, connection: Connection) extends Extension {
  var streamingConnection: StreamingConnection = null

  def connection(): StreamingConnection = {
    if (streamingConnection == null || streamingConnection.getNatsConnection == null) {
      streamingConnection = new StreamingConnectionFactory(new Options.Builder()
        .natsConn(connection)
        .clusterId(config.getString("nats.cluster.id"))
        .clientId(config.getString("nats.client.id"))
        .build()).createConnection()
    }
    streamingConnection
  }
}

object NatsConnectionExtension extends ExtensionId[NatsConnectionExtensionImpl] {

  private val log = LoggerFactory.getLogger(NatsConnectionExtension.getClass)

  override def createExtension(system: ActorSystem[_]): NatsConnectionExtensionImpl = {
    val config = system.settings.config;
    val options = new io.nats.client.Options.Builder().server(config.getString("nats.url"))
      .maxReconnects(-1)
      .reconnectBufferSize(-1)
      .maxControlLine(1024)
      .connectionListener((conn: Connection, eventType: ConnectionListener.Events) => {
        log.debug(eventType.toString)
      }).build()
    new NatsConnectionExtensionImpl(config, Nats.connect(options))
  }

  def get(system: ActorSystem[_]): NatsConnectionExtensionImpl = apply(system)
}
