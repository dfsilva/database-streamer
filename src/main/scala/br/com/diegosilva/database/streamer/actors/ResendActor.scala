package br.com.diegosilva.database.streamer.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import br.com.diegosilva.database.streamer.CborSerializable
import br.com.diegosilva.database.streamer.db.DbExtension
import br.com.diegosilva.database.streamer.repo.EventsTableRepo
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object ResendActor {

  implicit val timeout = Timeout(10.seconds)

  private val log = LoggerFactory.getLogger(ResendActor.getClass)

  sealed trait Command extends CborSerializable

  private case class PublisherResponse(databaseNotification: Seq[DatabaseNotification], exception: Option[Throwable] = None) extends Command

  final case class Start(lastId: Long) extends Command

  private case object ResendTimerKey

  def apply(processActor: ActorRef[ProcessActor.Command]): Behavior[ResendActor.Command] = Behaviors.supervise(behaviors(processActor = processActor))
    .onFailure(SupervisorStrategy.restart)

  def behaviors(processActor: ActorRef[ProcessActor.Command]): Behavior[ResendActor.Command] = {
    Behaviors.withTimers { timers =>
      Behaviors.setup { context =>
        val db: Database = DbExtension.get(context.system).db()
        Behaviors.receiveMessage[ResendActor.Command] {
          case Start(lastId) => {
            timers.cancel(ResendTimerKey)
            val pendingMessages = Await.result(db.run(EventsTableRepo.pendingMessages(lastId, 1000)), 2.seconds)
            if (!pendingMessages.isEmpty) {
              val notifications = pendingMessages.map(ev => DatabaseNotification(id = ev.id.get,
                time = ev.createTime.toString,
                topic = ev.topic,
                old = ev.old,
                current = ev.current))
              val topics: Map[String, Seq[DatabaseNotification]] = notifications.groupBy(_.topic)
              topics.foreach(map => {
                processActor ! ProcessActor.ProcessMessages(map._1, map._2)
              })
              timers.startSingleTimer(ResendTimerKey, Start(pendingMessages.last.id.get), 2.seconds)
              Behaviors.same
            } else {
              timers.startSingleTimer(ResendTimerKey, Start(lastId), 2.seconds)
              Behaviors.same
            }
          }
        }.receiveSignal {
          case (context, PostStop) =>
            context.log.info(s"Stoping Resend Actor...")
            Behaviors.same
          case (context, PreRestart) =>
            context.log.info(s"Restarting Resend Actor....")
            context.self ! Start(0)
            Behaviors.same
        }
      }
    }
  }

}
