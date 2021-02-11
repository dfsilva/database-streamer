package br.com.diegosilva.database.streamer.actors


import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior, PostStop, PreRestart, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import br.com.diegosilva.database.streamer.CborSerializable
import br.com.diegosilva.database.streamer.actors.PublisherActor.AddedSucessfull
import br.com.diegosilva.database.streamer.actors.ResendActor.{ResendTimerKey, Start}
import br.com.diegosilva.database.streamer.db.DbExtension
import br.com.diegosilva.database.streamer.repo.EventsTableRepo
import io.circe.parser.decode
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object ListenerActor {


  import br.com.diegosilva.database.streamer.Main.timeout
  import br.com.diegosilva.database.streamer.api.CirceJsonProtocol._

  private val log = LoggerFactory.getLogger(ListenerActor.getClass)

  sealed trait Command extends CborSerializable

  final case object Start extends Command

  private case object StartTimerKey

  private case class PublisherResponse(databaseNotification: DatabaseNotification, exception: Option[Throwable] = None) extends Command

  def apply(): Behavior[ListenerActor.Command] = Behaviors.supervise(behaviors())
    .onFailure(SupervisorStrategy.restart)

  def behaviors(): Behavior[ListenerActor.Command] = {
    Behaviors.withTimers { timers =>
      Behaviors.setup { context =>
        val connection = DbExtension.get(context.system).ds.getConnection
        val statement = connection.createStatement()
        statement.execute("LISTEN events_notify")
        statement.close()
        val pgconn = connection.unwrap(classOf[org.postgresql.PGConnection])

        Behaviors.receiveMessage[ListenerActor.Command] {
          case Start =>
            timers.cancel(StartTimerKey)
            val notifications = pgconn.getNotifications()
            if (!notifications.isEmpty) {
              log.debug("Received notifications: {}", notifications.length)

              val natsNotifications: Seq[DatabaseNotification] = notifications.map(pgNotification => decode[DatabaseNotification](pgNotification.getParameter)).map{
                case Right(value) => Some(value)
                case Left(_) => None
              }
                .filter(_.isDefined).map(_.get)

              natsNotifications.foreach(notification => {
                val entityRef = ClusterSharding(context.system).entityRefFor(PublisherActor.EntityKey, notification.topic)
                context.ask(entityRef, PublisherActor.AddToProcess(notification, _)) {
                  case Success(value: AddedSucessfull) => PublisherResponse(value.message)
                  case Failure(exception) => PublisherResponse(notification, Some(exception))
                }
              })
            } else {
              log.debug("Scheduling Start Listener 2 seconds")
              timers.startSingleTimer(StartTimerKey, Start, 2.seconds)
            }
            Behaviors.same
          case PublisherResponse(notification, exception) =>
            if (exception.isEmpty) {
              val db = DbExtension.get(context.system).db()
              Await.result(db.run(EventsTableRepo.delete(notification.id)), 500.milliseconds)
            }
            log.debug("Scheduling Start Listener 2 seconds")
            timers.startSingleTimer(StartTimerKey, Start, 2.seconds)
            Behaviors.same
        }.receiveSignal {
          case (context, PostStop) =>
            log.info(s"Stoping Listener...")
            context.spawn(ResendActor(), "resend-actor")
            Behaviors.same
          case (context, PreRestart) =>
            context.log.info(s"Restarting Listener....")
            context.self ! Start
            Behaviors.same
        }
      }
    }
  }
}
