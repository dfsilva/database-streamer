package br.com.diegosilva.database.streamer.nats

import akka.actor.typed.{ActorSystem, Extension, ExtensionId}
import br.com.diego.silva.nats.NatsStreamConnectionWrapper
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

class NatsConnectionExtensionImpl(config: Config, streamingConnection: NatsStreamConnectionWrapper) extends Extension {

  def connection(): NatsStreamConnectionWrapper = streamingConnection
}

object NatsConnectionExtension extends ExtensionId[NatsConnectionExtensionImpl] {

  private val log = LoggerFactory.getLogger(NatsConnectionExtension.getClass)

  override def createExtension(system: ActorSystem[_]): NatsConnectionExtensionImpl = {
    val config = system.settings.config
    new NatsConnectionExtensionImpl(config, new NatsStreamConnectionWrapper(
      config.getString("nats.url"),
      config.getString("nats.cluster.id"), config.getString("nats.client.id")))
  }

  def get(system: ActorSystem[_]): NatsConnectionExtensionImpl = apply(system)
}
