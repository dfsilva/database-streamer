package br.com.diegosilva.database.streamer.actors


import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, PostStop, PreRestart, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import br.com.diegosilva.database.streamer.CborSerializable
import PublisherActor.AddSucessfull
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

  def apply(resendActor:ActorRef[ResendActor.Command], processActor: ActorRef[ProcessActor.Command]): Behavior[ListenerActor.Command] = Behaviors.supervise(behaviors(resendActor, processActor))
    .onFailure(SupervisorStrategy.restart)

  def behaviors(resendActor:ActorRef[ResendActor.Command], processActor: ActorRef[ProcessActor.Command]): Behavior[ListenerActor.Command] = {
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
            val pgNotifications = pgconn.getNotifications()

            if (!pgNotifications.isEmpty) {
              log.debug("Received notifications: {}", pgNotifications.length)
              val natsNotifications: Seq[DatabaseNotification] = pgNotifications.map(pgNotification => decode[DatabaseNotification](pgNotification.getParameter)).map{
                case Right(value) => Some(value)
                case Left(_) => None
              }
                .filter(_.isDefined).map(_.get)

              log.debug("Converted notifications: {}", natsNotifications.length)
              val topics:Map[String, Seq[DatabaseNotification]] = natsNotifications.groupBy(_.topic)
              topics.foreach(map =>{
                processActor ! ProcessActor.ProcessMessages(map._1, map._2)
              })
              timers.startSingleTimer(StartTimerKey, Start, 2.seconds)
            } else {
              log.debug("Scheduling Start Listener 2 seconds")
              timers.startSingleTimer(StartTimerKey, Start, 2.seconds)
            }
            Behaviors.same
        }.receiveSignal {
          case (context, PostStop) =>
            log.info(s"Stoping Listener...")
            Behaviors.same
          case (context, PreRestart) =>
            context.log.info(s"Restarting Listener....")
            resendActor ! ResendActor.Start(0, context.self)
            Behaviors.same
        }
      }
    }
  }
}
