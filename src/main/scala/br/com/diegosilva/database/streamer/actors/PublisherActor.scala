package br.com.diegosilva.database.streamer.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed._
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import br.com.diegosilva.database.streamer.CborSerializable
import br.com.diegosilva.database.streamer.nats.{NatsConnectionExtension, NatsPublisher}
import io.circe.syntax.EncoderOps
import org.slf4j.LoggerFactory

import scala.collection.immutable.Queue
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object PublisherActor {

  import br.com.diegosilva.database.streamer.api.CirceJsonProtocol._

  private val log = LoggerFactory.getLogger(PublisherActor.getClass)

  private case object TimerKey

  object State {
    val empty = State(lastId = 0, processQueue = Queue())
  }

  final case class State(lastId: Long,
                         processQueue: Queue[DatabaseNotification]
                        ) extends CborSerializable

  sealed trait Command extends CborSerializable

  final case class AddToProcess(message: Seq[DatabaseNotification], replyTo: ActorRef[Command]) extends Command

  final case class AddedSucessfull(message: Seq[DatabaseNotification]) extends Command

  final case class ProcessMessages() extends Command

  final case class WaitCommand() extends Command

  final case class ProcessMessage(message: DatabaseNotification) extends Command

  sealed trait Event extends CborSerializable

  final case class AddedToProcess(msg: Seq[DatabaseNotification]) extends Event

  final case class ProcessedSuccess(msg: DatabaseNotification, natsKey: String) extends Event

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Publisher")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContent =>
      Behaviors
        .supervise(PublisherActor(entityContent.entityId))
        .onFailure[Exception](SupervisorStrategy.restart)
    })
  }

  def apply(id: String): Behavior[Command] = {
    log.info("Creating publisher Actor {}..........", id)
    Behaviors.withTimers { timers =>
      Behaviors.setup[Command] { context =>
        EventSourcedBehavior[Command, Event, State](
          PersistenceId("Publisher", id),
          State.empty,
          (state, command) => handlerCommands(id, state, command, timers, context),
          (state, event) => handlerEvent(state, event))
          .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 1000, keepNSnapshots = 3))
          .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, randomFactor = 0.1))
          .receiveSignal {
            case (context, PostStop) =>
              log.info(s"Stoping publisher {}", id)
              Behaviors.same
            case (context, PreRestart) =>
              log.info(s"Restarting Publisher {}", id)
              Behaviors.same
          }
      }
    }
  }

  private def handlerCommands(id: String, state: State, command: Command,
                              timer: TimerScheduler[Command],
                              context: ActorContext[Command]): Effect[Event, State] = {

    command match {
      case AddToProcess(message, replyTo) => {
        timer.cancel(TimerKey)
        log.info(s"Adding message to process {}", id)
        Effect.persist(AddedToProcess(message))
          .thenReply(replyTo)(updated => {
            timer.startSingleTimer(TimerKey, ProcessMessages(), 2.seconds)
            AddedSucessfull(message)
          })
      }

      case ProcessMessages() => {
        log.info(s"Processsing messages {}", id)
        log.info(s"waiting ${state.processQueue.length}")
        state.processQueue.headOption match {
          case Some(notification) => {
            val natsMessage = NatsNotification(notification.old, notification.current).asJson.noSpaces
            log.debug("Sending message {} to topic {}", natsMessage, notification.topic)
            val natsKey = Await.result(NatsPublisher(NatsConnectionExtension.get(context.system).streamingConnection, notification.topic, natsMessage), 500.millis)
            log.debug("Message sent {}", natsKey)
            Effect.persist(ProcessedSuccess(notification, natsKey)).thenReply(context.self)(updated => {
              ProcessMessages()
            })
          }
          case _ => Effect.none
        }
      }
    }
  }

  private def handlerEvent(state: State, event: Event): State = {
    event match {
      case AddedToProcess(message) => state.copy(processQueue = state.processQueue ++ message)
      case ProcessedSuccess(message, natsKey) => state.copy(processQueue = state.processQueue.filter(_.id != message.id))
    }
  }

}
