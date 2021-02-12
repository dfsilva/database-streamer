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

  final case class AddToProcess(messages: Seq[DatabaseNotification], replyTo: ActorRef[Command]) extends Command

  final case class AddSucessfull(message: Seq[DatabaseNotification]) extends Command

  final case class ProcessMessage() extends Command

  def apply(): Behavior[Command] = Behaviors.supervise(behaviors())
    .onFailure(SupervisorStrategy.restart)

  def behaviors(processQueue: Queue[DatabaseNotification] = Queue.empty): Behavior[Command] = {
    Behaviors.withTimers { timers =>
      Behaviors.setup { context =>
        val db: Database = DbExtension.get(context.system).db()
        Behaviors.receiveMessage[Command] {
          case AddToProcess(messages, replyTo) => {
            timers.cancel(TimerKey)
            replyTo ! AddSucessfull(messages)
            timers.startSingleTimer(TimerKey, ProcessMessage(), 2.seconds)
            behaviors(processQueue ++ messages)
          }
          case ProcessMessage() => {
            log.debug(s"Processing queue size ${processQueue.length}")
            processQueue.headOption match {
              case Some(notification) => {
                val natsMessage = NatsNotification(notification.old, notification.current).asJson.noSpaces
                log.debug("Sending message {} to topic {}", natsMessage, notification.topic)
                val natsKey = Await.result(NatsPublisher(NatsConnectionExtension.get(context.system).streamingConnection, notification.topic, natsMessage), 500.millis)
                log.debug("Message sent {}", natsKey)
                timers.startSingleTimer(TimerKey, ProcessMessage(), 500.millis)
                Await.result(db.run(EventsTableRepo.delete(notification.id)), 1.second)
                behaviors(processQueue.tail)
              }
              case _ => behaviors(processQueue)
            }
          }
        }.receiveSignal {
          case (context, PostStop) =>
            log.info(s"Stoping Processor...")
            Behaviors.same
          case (context, PreRestart) =>
            context.log.info(s"Restarting processor....")
            Behaviors.same
        }
      }
    }
  }
}
