package br.com.diegosilva.database.streamer.actors


import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, PostStop, PreRestart, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import br.com.diegosilva.database.streamer.CborSerializable
import PublisherActor.AddedSucessfull
import br.com.diegosilva.database.streamer.actors.ResendActor.{ResendTimerKey, Start}
import br.com.diegosilva.database.streamer.db.DbExtension
import br.com.diegosilva.database.streamer.repo.EventsTableRepo
import io.circe.parser.decode
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object ListenerActor {

  implicit val timeout = Timeout(10.seconds)
  import br.com.diegosilva.database.streamer.api.CirceJsonProtocol._

  private val log = LoggerFactory.getLogger(ListenerActor.getClass)

  sealed trait Command extends CborSerializable

  final case object Start extends Command

  private case object StartTimerKey

  private case class PublisherResponse(databaseNotification: Seq[DatabaseNotification], exception: Option[Throwable] = None) extends Command

  def apply(resendActor:ActorRef[ResendActor.Command]): Behavior[ListenerActor.Command] = Behaviors.supervise(behaviors(resendActor))
    .onFailure(SupervisorStrategy.restart)

  def behaviors(resendActor:ActorRef[ResendActor.Command]): Behavior[ListenerActor.Command] = {
    Behaviors.withTimers { timers =>
      Behaviors.setup { context =>
        val connection = DbExtension.get(context.system).ds.getConnection
        val statement = connection.createStatement()
        statement.execute("LISTEN events_notify")
        statement.close()
        val pgconn = connection.unwrap(classOf[org.postgresql.PGConnection])
        val db = DbExtension.get(context.system).db()

        Behaviors.receiveMessage[ListenerActor.Command] {
          case Start =>
            timers.cancel(StartTimerKey)
            val pgNotifications = pgconn.getNotifications()

            if (!pgNotifications.isEmpty) {
              log.debug("Received notifications: {}", pgNotifications.length)

              val natsNotifications: Seq[DatabaseNotification] = pgNotifications.map(pgNotification => decode[DatabaseNotification](pgNotification.getParameter)).map{
                case Right(value) => Some(value)
                case Left(_) => None
              }
                .filter(_.isDefined).map(_.get)

              log.debug("Converted notifications: {}", natsNotifications.length)

              val topic = natsNotifications(0).topic
              val topicNotifications = natsNotifications.filter(_.topic == topic)
              val entityRef = ClusterSharding(context.system).entityRefFor(PublisherActor.EntityKey, topic)

              context.ask(entityRef, PublisherActor.AddToProcess(topicNotifications, _)) {
                case Success(value: AddedSucessfull) => PublisherResponse(value.message)
                case Failure(exception) => PublisherResponse(topicNotifications, Some(exception))
              }
            } else {
              log.debug("Scheduling Start Listener 2 seconds")
              timers.startSingleTimer(StartTimerKey, Start, 2.seconds)
            }
            Behaviors.same
          case PublisherResponse(notifications, exception) =>
            val ids:Seq[Long] = notifications.map(_.id)
            if (exception.isEmpty) {
              Await.result(db.run(EventsTableRepo.delete(ids)), 2.seconds)
            }
            log.debug("Scheduling Start Listener 2 seconds")
            timers.startSingleTimer(StartTimerKey, Start, 2.seconds)
            Behaviors.same
        }.receiveSignal {
          case (context, PostStop) =>
            log.info(s"Stoping Listener...")
            Behaviors.same
          case (context, PreRestart) =>
            context.log.info(s"Restarting Listener....")
            resendActor ! ResendActor.Start(context.self)
            Behaviors.same
        }
      }
    }
  }
}
