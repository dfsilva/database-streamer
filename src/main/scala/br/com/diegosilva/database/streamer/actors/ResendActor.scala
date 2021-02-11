package br.com.diegosilva.database.streamer.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior, PostStop, PreRestart, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import br.com.diegosilva.database.streamer.CborSerializable
import br.com.diegosilva.database.streamer.actors.PublisherActor.AddedSucessfull
import br.com.diegosilva.database.streamer.db.DbExtension
import br.com.diegosilva.database.streamer.repo.{Event, EventsTableRepo}
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object ResendActor {

  import br.com.diegosilva.database.streamer.Main._

  private val log = LoggerFactory.getLogger(ResendActor.getClass)

  sealed trait Command extends CborSerializable

  private case class PublisherResponse(databaseNotification: DatabaseNotification, exception: Option[Throwable] = None) extends Command

  final case object Start extends Command

  final case object Stop extends Command

  private case object ResendTimerKey

  def apply(): Behavior[ResendActor.Command] = Behaviors.supervise(behaviors())
    .onFailure(SupervisorStrategy.restart)

  def behaviors(beforeEvents: Seq[Event] = Seq.empty): Behavior[ResendActor.Command] = {

    log.info("behaviors beforeEvents Size: {}", beforeEvents.size)

    Behaviors.withTimers { timers =>
      Behaviors.setup { context =>
        val db: Database = DbExtension.get(context.system).db()

        if (beforeEvents.isEmpty) {
          log.debug("Scheduling Resend to 500 milliseconds")
          timers.startSingleTimer(ResendTimerKey, Start, 500.milliseconds)
        }

        Behaviors.receiveMessage[ResendActor.Command] {
          case Start => {
            log.debug("Resend Start event")
            timers.cancel(ResendTimerKey)
            if (beforeEvents.isEmpty) {
              log.debug("Size of beforeEvents {}", beforeEvents.size)
              val pendingMessages = Await.result(db.run(EventsTableRepo.pendingMessages(1000)), 500.millis)
              log.debug("Size of pending messages {}", pendingMessages.size)
              if (!pendingMessages.isEmpty) {
                pendingMessages.foreach(event => {
                  val entityRef = ClusterSharding(context.system).entityRefFor(PublisherActor.EntityKey, event.topic)
                  val notification = DatabaseNotification(id = event.id.get,
                    time = event.createTime.toString,
                    topic = event.topic,
                    old = event.old,
                    current = event.current)
                  context.ask(entityRef, PublisherActor.AddToProcess(notification, _)) {
                    case Success(value: AddedSucessfull) => PublisherResponse(value.message)
                    case Failure(exception) => PublisherResponse(notification, Some(exception))
                  }
                })
                behaviors(pendingMessages)
              } else {
                log.debug("No pending messages anymore, starting listener actor")
                context.system..spawn(ListenerActor(), "listener-actor") ! ListenerActor.Start
                context.self ! Stop
                Behaviors.same
              }
            } else {
              Behaviors.same
            }
          }
          case PublisherResponse(notification, exception) =>
            if(exception.isEmpty){
              Await.result(db.run(EventsTableRepo.delete(notification.id)), 500.milliseconds)
            }else{
              log.error("Error processing notification {} {}", notification.id, exception.get.getMessage)
            }
            val newEvents = beforeEvents.filter(ev => ev.id.get != notification.id)
            log.debug("New Events size {}", newEvents.size)
            behaviors(newEvents)
          case Stop =>
            Behaviors.stopped
        }.receiveSignal {
          case (context, PostStop) =>
            context.log.info(s"Stoping Resend Actor...")
            Behaviors.stopped
          case (context, PreRestart) =>
            context.log.info(s"Restarting Resend Actor....")
            context.self ! Start
            Behaviors.same
        }
      }
    }
  }

}
