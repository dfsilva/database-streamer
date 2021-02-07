package br.com.diegosilva.database.streamer.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior, PostStop, PreRestart, SupervisorStrategy}
import br.com.diegosilva.database.streamer.CborSerializable
import br.com.diegosilva.database.streamer.repo.Event
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory

object ListenerActor {

  import io.circe.syntax._
  private val log = LoggerFactory.getLogger(ListenerActor.getClass)

  sealed trait Command extends CborSerializable

  final case object StartListener extends Command

  def apply(datasource: HikariDataSource): Behavior[ListenerActor.Command] = Behaviors.supervise(behaviros(datasource))
    .onFailure(SupervisorStrategy.restart)

  def behaviros(datasource: HikariDataSource): Behavior[ListenerActor.Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage[ListenerActor.Command] {
        case StartListener =>
          val connection = datasource.getConnection()
          val statement = connection.createStatement()
          statement.execute("LISTEN events_notify")
          statement.close()
          while (true) {
            val pgconn = connection.unwrap(classOf[org.postgresql.PGConnection])
            val notifications = pgconn.getNotifications
            if (notifications != null && !notifications.isEmpty) {
              notifications.foreach(notification => {
                log.debug("Received notification: {}", notification)

                notification.getParameter.asJsonObject

                //                decode[Event](notification.getParameter) match {
                //                  case Right(event) => {
                //                    log.debug("New data: {}",event.newData)
                //                  }
                //                  case _ =>
                //                    log.error("Error parsing notification: {}", notification.getParameter)
                //                }
              })
            }
          }
          Behaviors.same
      }.receiveSignal {
        case (context, PostStop) =>
          context.log.info(s"Stoping actor...")
          Behaviors.same
        case (context, PreRestart) =>
          context.self ! StartListener
          Behaviors.same
      }
    }


}
