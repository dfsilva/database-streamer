package br.com.diegosilva.database.streamer.api


import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route
import br.com.diegosilva.database.streamer.db.DbExtension
import br.com.diegosilva.database.streamer.repo.{StreamTbl, StreamTblRepo, TriggersRepo}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.slf4j.LoggerFactory

object Routes {
  def apply(): Route = new Routes().routes
}

class Routes() extends FailFastCirceSupport  {

  import br.com.diegosilva.database.streamer.api.CirceJsonProtocol._
  import akka.http.scaladsl.server._
  import Directives._
  import br.com.diegosilva.database.streamer.Main._
  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
  import io.circe.generic.auto._

  private lazy val log = LoggerFactory.getLogger(getClass)
  private val sharding = ClusterSharding(system)
  private val connection = DbExtension(system).dataSource()
  private val db = DbExtension(system).db()


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
                concat(
                  post {
                    entity(as[AddTableStream]) { data =>
                      val con = connection.getConnection

                      val action = for {
                        createFunctio <- TriggersRepo.createFunction(data.table, data.topic, con)
                        createTrigger <- TriggersRepo.createTrigger(data.schema, data.table, con)
                        _ <- db.run(StreamTblRepo.add(StreamTbl(id = None, title = data.title, description = data.description, table = data.table, topic = data.topic)))
                      } yield (createFunctio && createTrigger)
                      complete(action)
                    }
                  },
                  delete {
                    path(Segment / Segment) { (schema, tableName) =>
                      val con = connection.getConnection
                      val action = for {
                        deleteTrigger <- TriggersRepo.deleteTrigger(schema, tableName, con)
                        deleteFunction <- TriggersRepo.deleteFunction(tableName, con)
                      } yield (deleteFunction && deleteTrigger)
                      complete(action)
                    }
                  }
                )
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
