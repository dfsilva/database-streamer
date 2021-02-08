package br.com.diegosilva.database.streamer.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior, PostStop, PreRestart, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import br.com.diegosilva.database.streamer.CborSerializable
import com.zaxxer.hikari.HikariDataSource
import io.circe.parser.decode
import org.slf4j.LoggerFactory

object ListenerActor {

  import br.com.diegosilva.database.streamer.api.CirceJsonProtocol._

  private val log = LoggerFactory.getLogger(ListenerActor.getClass)

  sealed trait Command extends CborSerializable

  final case object StartListener extends Command

  def apply(datasource: HikariDataSource): Behavior[ListenerActor.Command] = Behaviors.supervise(behaviors(datasource))
    .onFailure(SupervisorStrategy.restart)

  def behaviors(datasource: HikariDataSource): Behavior[ListenerActor.Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage[ListenerActor.Command] {
        case StartListener =>
          context.log.info(s"Start Listener....")
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
                decode[DatabaseNotification](notification.getParameter) match {
                  case Right(event) => {
                    val entityRef = ClusterSharding(context.system).entityRefFor(PublisherActor.EntityKey, event.topic)
                    entityRef ! PublisherActor.AddToProcess(event)
                  }
                  case Left(error) =>
                    log.error("Error parsing notification: {} {}", notification.getParameter, error.getMessage)
                }
              })
            }
          }
          Behaviors.same
      }.receiveSignal {
        case (context, PostStop) =>
          context.log.info(s"Stoping Listener...")
          Behaviors.same
        case (context, PreRestart) =>
          context.log.info(s"Restarting Listener....")
          context.self ! StartListener
          Behaviors.same
      }
    }
}
