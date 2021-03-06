package br.com.diegosilva.database.streamer.api

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.model.HttpMethods.{DELETE, GET, OPTIONS, POST, PUT}
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.headers.{`Access-Control-Allow-Credentials`, `Access-Control-Allow-Headers`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Origin`}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, options, respondWithHeaders}
import akka.http.scaladsl.server.{Directive0, Route}
import br.com.diegosilva.database.streamer.db.DbExtension
import br.com.diegosilva.database.streamer.repo.{DbStream, DbStreamRepo, TriggersRepo}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.slf4j.LoggerFactory

import akka.http.scaladsl.server._
import Directives._

trait CORSHandler{

  private val corsResponseHeaders = List(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Credentials`(true),
    `Access-Control-Allow-Headers`("Authorization",
      "Content-Type", "X-Requested-With")
  )

  private def addAccessControlHeaders: Directive0 = {
    respondWithHeaders(corsResponseHeaders)
  }

  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(StatusCodes.OK).
      withHeaders(`Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)))
  }

  def corsHandler(r: Route): Route = addAccessControlHeaders {
    preflightRequestHandler ~ r
  }

  def addCORSHeaders(response: HttpResponse):HttpResponse =
    response.withHeaders(corsResponseHeaders)

}

object Routes {
  def apply(): Route = new Routes().routes
}

class Routes() extends FailFastCirceSupport with CORSHandler {


  import br.com.diegosilva.database.streamer.Main._
  import io.circe.generic.auto._

  private lazy val log = LoggerFactory.getLogger(getClass)
  private val sharding = ClusterSharding(system)
  private val dataSource = DbExtension(system).dataSource()
  private val db = DbExtension(system).db()


  val errorHandler = ExceptionHandler {
    case ex =>
      extractUri { uri =>
        log.error(ex.getMessage, ex)
        complete(HttpResponse(InternalServerError, entity = s"Houston, we have a problem: ${ex.getMessage} ao acessar a uri: $uri"))
      }
  }

  val routes: Route = corsHandler(
    concat(
        handleExceptions(errorHandler) {
          pathPrefix("api") {
            concat(
              pathPrefix("streams") {
                concat(
                  get {
                    complete(db.run(DbStreamRepo.list()))
                  },
                  post {
                    entity(as[AddTableStream]) { data =>
                      val con = dataSource.getConnection
                      val action = for {
                        _ <- TriggersRepo.create(data.schema, data.table, data.topic, data.delete, data.insert, data.update, con)
                        dbStream <- db.run(DbStreamRepo.add(DbStream(
                          title = data.title,
                          description = data.description,
                          table = data.table,
                          topic = data.topic,
                          schema = data.schema,
                          insert = data.insert,
                          update = data.update,
                          delete = data.delete)))
                      } yield dbStream
                      complete(action)
                    }
                  },

                  put {
                    entity(as[AddTableStream]) { data =>
                      val action = for {
                        _ <- TriggersRepo.delete(data.schema, data.topic, data.table, dataSource.getConnection)
                        _ <- db.run(DbStreamRepo.delete(data.topic))
                        _ <- TriggersRepo.create(data.schema, data.table, data.topic, data.delete, data.insert, data.update, dataSource.getConnection)
                        dbStream <- db.run(DbStreamRepo.add(DbStream(
                          title = data.title,
                          description = data.description,
                          table = data.table,
                          topic = data.topic,
                          schema = data.schema,
                          insert = data.insert,
                          update = data.update,
                          delete = data.delete)))
                      } yield dbStream
                      complete(action)
                    }
                  },

                  delete {
                    path(Segment / Segment / Segment) { (schema, table, topic) =>
                      val con = dataSource.getConnection
                      val action = for {
                        _ <- TriggersRepo.delete(schema, topic, table, con)
                        _ <- db.run(DbStreamRepo.delete(topic))
                      } yield ("ok")
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
  )
}
