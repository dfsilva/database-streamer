package br.com.diegosilva.database.streamer.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior, SupervisorStrategy}
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

  object State {
    val empty = State(lastId = 0, processQueue = Queue())
  }

  final case class State(lastId: Long,
                         processQueue: Queue[DatabaseNotification]
                        ) extends CborSerializable

  sealed trait Command extends CborSerializable

  final case class AddToProcess(message: DatabaseNotification) extends Command

  final case class AddedSucessfull(message: DatabaseNotification) extends Command

  final case class ProcessMessages() extends Command

  final case class WaitCommand() extends Command

  final case class ProcessMessage(message: DatabaseNotification) extends Command


  sealed trait Event extends CborSerializable

  final case class AddedToProcess(msg: DatabaseNotification) extends Event

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
    Behaviors.setup[Command] { context =>
      EventSourcedBehavior[Command, Event, State](
        PersistenceId("Publisher", id),
        State.empty,
        (state, command) => handlerCommands(id, state, command, context),
        (state, event) => handlerEvent(state, event))
        .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 5, keepNSnapshots = 3))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, randomFactor = 0.1))
    }
  }


  private def handlerCommands(id: String, state: State, command: Command,
                              context: ActorContext[Command]): Effect[Event, State] = {
    command match {
      case AddToProcess(message) => {
        log.info(s"Adding message to process {}", id)
        Effect.persist(AddedToProcess(message))
          .thenReply(context.self)(updated => {
            ProcessMessages()
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
      case AddedToProcess(message) => state.copy(processQueue = state.processQueue :+ message)
      case ProcessedSuccess(message, natsKey) => state.copy(processQueue = state.processQueue.filter(_.id != message.id))
    }
  }

}
