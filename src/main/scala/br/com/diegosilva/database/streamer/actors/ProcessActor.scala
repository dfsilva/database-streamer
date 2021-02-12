package br.com.diegosilva.database.streamer.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors
import br.com.diegosilva.database.streamer.CborSerializable
import org.slf4j.LoggerFactory

object ProcessActor {

  private val log = LoggerFactory.getLogger(ProcessActor.getClass)

  sealed trait Command extends CborSerializable

  final case class ProcessMessages(topic: String, message: Seq[DatabaseNotification]) extends Command

  final case class PublisherResponse(response: PublisherActor.Command) extends Command


  def apply(): Behavior[ProcessActor.Command] = Behaviors.supervise(behaviors())
    .onFailure(SupervisorStrategy.restart)

  def behaviors(actors: Map[String, ActorRef[PublisherActor.Command]] = Map.empty): Behavior[ProcessActor.Command] = {

    Behaviors.setup { context =>
      Behaviors.receiveMessage[Command] {
        case ProcessMessages(topic, messages) =>
          if (actors.contains(topic)) {
            actors(topic) ! PublisherActor.AddToQueue(messages.toSet)
            Behaviors.same
          } else {
            val publishActor: ActorRef[PublisherActor.Command] = context.spawn(PublisherActor(), s"actor-$topic")
            publishActor ! PublisherActor.AddToQueue(messages.toSet)
            behaviors(actors + (topic -> publishActor))
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
