package br.com.diegosilva.database.streamer.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed._
import br.com.diegosilva.database.streamer.CborSerializable
import org.slf4j.LoggerFactory

object ProcessActor {

  private val log = LoggerFactory.getLogger(ProcessActor.getClass)

  sealed trait Command extends CborSerializable

  final case class ProcessMessage(message: DatabaseNotification, replyTo: ActorRef[Command]) extends Command

  final case class AddedSucessfull(message: DatabaseNotification) extends Command

  final case class ProcessMessages() extends Command

  def apply(): Behavior[ProcessActor.Command] = Behaviors.supervise(behaviors())
    .onFailure(SupervisorStrategy.restart)

  def behaviors(): Behavior[ProcessActor.Command] = {

    Behaviors.setup { context =>
      Behaviors.receiveMessage[ProcessActor.Command] {
        case _ =>
          Behaviors.same
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
