package br.com.diegosilva.database.streamer.api


import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route
import br.com.diegosilva.database.streamer.db.DbExtension
import br.com.diegosilva.database.streamer.repo.TriggersRepo
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.slf4j.LoggerFactory

object Routes {
  def apply(): Route = new Routes().routes
}

class Routes() extends FailFastCirceSupport with CirceJsonProtocol {

  import akka.http.scaladsl.server._
  import Directives._
  import br.com.diegosilva.database.streamer.Main._
  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
  import io.circe.generic.auto._

  private lazy val log = LoggerFactory.getLogger(getClass)
  private val sharding = ClusterSharding(system)
  private val connection = DbExtension(system).connection()


  val errorHandler = ExceptionHandler {
    case ex =>
      extractUri { uri =>
        log.error(ex.getMessage, ex)
        complete(HttpResponse(InternalServerError, entity = s"Ocorreu algum erro inesperado: ${ex.getMessage} ao acessar a uri: $uri"))
      }
  }

  val routes: Route =
    handleExceptions(errorHandler) {
      concat(
        cors() {
          pathPrefix("api") {
            concat(
              pathPrefix("streams") {
                post {
                  entity(as[AddTableStream]) { data =>
                    complete(TriggersRepo.createTrigger(data.table, data.topic, connection.getConnection))
                  }
                }
              }
            )
          }
        },
        get {
          (pathEndOrSingleSlash & redirectToTrailingSlashIfMissing(StatusCodes.TemporaryRedirect)) {
            getFromResource("web/index.html")
          } ~ {
            getFromResourceDirectory("web")
          }
        }
      )
    }
}
