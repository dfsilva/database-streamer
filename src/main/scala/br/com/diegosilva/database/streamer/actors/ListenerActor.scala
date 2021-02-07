package br.com.diegosilva.database.streamer.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior, PostStop}
import br.com.diegosilva.database.streamer.CborSerializable
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory

object ListenerActor {

  private val log = LoggerFactory.getLogger(ListenerActor.getClass)

  sealed trait Command extends CborSerializable

  final case object Start extends Command

  def apply(datasource: HikariDataSource): Behavior[ListenerActor.Command] = Behaviors.setup { context =>

    Behaviors.receiveMessage[ListenerActor.Command] {
      case Start =>
        val connection = datasource.getConnection()
        val statement = connection.createStatement()
        statement.execute("LISTEN events_notify")
        statement.close()
        while (true) {
          val pgconn = connection.unwrap(classOf[org.postgresql.PGConnection])
          val notifications = pgconn.getNotifications
          if (notifications != null) {
            notifications.foreach(notification => {
              log.debug("Received notification: {}", notification)
            })
          }
        }
        Behaviors.same
    }.receiveSignal {
      case (context, PostStop) =>
        context.log.info(s"Stoping actor...")
        Behaviors.same
    }
  }

}
