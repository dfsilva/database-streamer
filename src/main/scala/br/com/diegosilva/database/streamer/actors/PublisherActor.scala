package br.com.diegosilva.database.streamer.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors
import br.com.diegosilva.database.streamer.CborSerializable
import br.com.diegosilva.database.streamer.db.DbExtension
import br.com.diegosilva.database.streamer.nats.{NatsConnectionExtension, NatsPublisher}
import br.com.diegosilva.database.streamer.repo.EventsTableRepo
import io.circe.syntax.EncoderOps
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.Database

import scala.collection.immutable.Queue
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object PublisherActor {

  import br.com.diegosilva.database.streamer.api.CirceJsonProtocol._

  private val log = LoggerFactory.getLogger(PublisherActor.getClass)

  private case object TimerKey

  sealed trait Command extends CborSerializable

  final case class AddToQueue(messages: Set[DatabaseNotification]) extends Command

  final case class ProcessMessage() extends Command

  def apply(): Behavior[Command] = Behaviors.supervise(behaviors())
    .onFailure(SupervisorStrategy.restartWithBackoff(minBackoff = 5.seconds, maxBackoff = 60.seconds, randomFactor = .3))

  def behaviors(processQueue: Set[DatabaseNotification] = Set.empty): Behavior[Command] = {
    log.debug(s"ProcessMessage, behaviors, size ${processQueue.size}")
    Behaviors.withTimers { timers =>
      Behaviors.setup { context =>
        val db: Database = DbExtension.get(context.system).db()
        Behaviors.receiveMessage[Command] {
          case AddToQueue(messages) => {
            timers.cancel(TimerKey)
            timers.startSingleTimer(TimerKey, ProcessMessage(), 2.seconds)
            behaviors(processQueue ++ messages)
          }
          case ProcessMessage() => {
            log.debug(s"ProcessMessage, queuesize ${processQueue.size}")
            timers.cancel(TimerKey)
            processQueue.headOption match {
              case Some(notification) => {
                val natsMessage = NatsNotification(notification.old, notification.current).asJson.noSpaces
                log.debug("Sending message {} to topic {}", natsMessage, notification.topic)
                val natsKey = Await.result(NatsPublisher(NatsConnectionExtension.get(context.system).connection(), notification.topic, natsMessage), 500.millis)
                log.debug("Message sent {}", natsKey)
                Await.result(db.run(EventsTableRepo.delete(notification.id)), 1.second)
                timers.startSingleTimer(TimerKey, ProcessMessage(), 50.millis)
                behaviors(processQueue.tail)
              }
              case _ => behaviors(processQueue)
            }
          }
        }.receiveSignal {
          case (context, PostStop) =>
            log.info(s"Stoping Publisher, size: ${processQueue.size}")
            Behaviors.same
          case (context, PreRestart) =>
            log.info(s"Restarting Publisher, size: ${processQueue.size}")
            timers.startSingleTimer(TimerKey, AddToQueue(processQueue), 500.millis)
            Behaviors.same
        }
      }
    }
  }
}
