package br.com.diegosilva.database.streamer.api


import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.persistence.jdbc.db.SlickExtension
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.slf4j.LoggerFactory

object Routes {
  def apply() = new Routes()
}

class Routes() extends FailFastCirceSupport with CirceJsonProtocol {

  import akka.http.scaladsl.server._
  import Directives._
  import br.com.diegosilva.database.streamer.Main._
  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

  private lazy val log = LoggerFactory.getLogger(getClass)
  private val sharding = ClusterSharding(system)
  private val database = SlickExtension(system).database(system.settings.config.getConfig("jdbc-journal")).database

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
