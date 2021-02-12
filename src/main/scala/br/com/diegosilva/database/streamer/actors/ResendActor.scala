package br.com.diegosilva.database.streamer.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import br.com.diegosilva.database.streamer.CborSerializable
import PublisherActor.AddedSucessfull
import br.com.diegosilva.database.streamer.db.DbExtension
import br.com.diegosilva.database.streamer.repo.{Event, EventsTableRepo}
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object ResendActor {

  implicit val timeout = Timeout(10.seconds)

  private val log = LoggerFactory.getLogger(ResendActor.getClass)

  sealed trait Command extends CborSerializable

  private case class PublisherResponse(databaseNotification: Seq[DatabaseNotification], exception: Option[Throwable] = None) extends Command

  final case class Start(listenerActor: ActorRef[ListenerActor.Command]) extends Command

  private case object ResendTimerKey

  def apply(): Behavior[ResendActor.Command] = Behaviors.supervise(behaviors())
    .onFailure(SupervisorStrategy.restart)

  def behaviors(actorRef: ActorRef[ListenerActor.Command] = null, beforeEvents: Seq[Event] = Seq.empty): Behavior[ResendActor.Command] = {
    log.info("behaviors beforeEvents Size: {}", beforeEvents.size)
    Behaviors.withTimers { timers =>
      Behaviors.setup { context =>
        val db: Database = DbExtension.get(context.system).db()
        Behaviors.receiveMessage[ResendActor.Command] {
          case Start(listenerActor) => {
            log.debug("Resend Start event")
            timers.cancel(ResendTimerKey)
            if (beforeEvents.isEmpty) {
              log.debug("Size of beforeEvents {}", beforeEvents.size)
              val pendingMessages = Await.result(db.run(EventsTableRepo.pendingMessages(1000)), 2.seconds)
              log.debug("Size of pending messages {}", pendingMessages.size)
              if (!pendingMessages.isEmpty) {
                val topic = pendingMessages(0).topic
                val notifications = pendingMessages.filter(_.topic == topic).map(ev => DatabaseNotification(id = ev.id.get,
                  time = ev.createTime.toString,
                  topic = ev.topic,
                  old = ev.old,
                  current = ev.current))
                val entityRef = ClusterSharding(context.system).entityRefFor(PublisherActor.EntityKey, topic)
                context.ask(entityRef, PublisherActor.AddToProcess(notifications, _)) {
                  case Success(value: AddedSucessfull) => PublisherResponse(value.message)
                  case Failure(exception) => PublisherResponse(notifications, Some(exception))
                }
                behaviors(listenerActor, pendingMessages)
              } else {
                log.debug("No pending messages anymore, starting listener actor")
                listenerActor ! ListenerActor.Start
                Behaviors.same
              }
            } else {
              Behaviors.same
            }
          }
          case PublisherResponse(notifications, exception) =>
            val ids:Seq[Long] = notifications.map(_.id)
            if (exception.isEmpty) {
              Await.result(db.run(EventsTableRepo.delete(ids)), 5.seconds)
            } else {
              log.error("Error processing notifications {} {}", notifications.map(_.id), exception.get.getMessage)
            }
            val newEvents = beforeEvents.filter(ev => !ids.contains(ev.id.get))
            log.debug("New Events size {}", newEvents.size)
            if(newEvents.isEmpty){
              timers.startSingleTimer(ResendTimerKey, Start(actorRef), 1.seconds)
            }
            behaviors(actorRef, newEvents)
        }.receiveSignal {
          case (context, PostStop) =>
            context.log.info(s"Stoping Resend Actor...")
            Behaviors.stopped
          case (context, PreRestart) =>
            context.log.info(s"Restarting Resend Actor....")
            Behaviors.same
        }
      }
    }
  }

}
