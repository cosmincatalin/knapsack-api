package com.cosminsanda

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.Unmarshaller.UnsupportedContentTypeException
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.google.inject.Guice
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.logging.log4j.{LogManager, Logger}
import spray.json._

import scala.concurrent.Future
import scala.language.postfixOps

object App extends App {

    val logger: Logger = LogManager.getLogger( this.getClass.getName )
    val conf: Config = ConfigFactory.load
    val injector = Guice.createInjector(new DI)

    val controller = injector.getInstance(classOf[Controller])

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val serverSource = Http().bind(system.settings.config.getString("interface"), system.settings.config.getInt("port"))

    import KnapsackJsonProtocol._

    val requestHandler: HttpRequest => Future[HttpResponse] = {
        case HttpRequest(HttpMethods.POST, Uri.Path("/solve"), _, input, _) =>
            Unmarshal(input).to[Problem]
                .map(controller.solve)
                .map(problemId =>
                    HttpResponse(StatusCodes.OK,
                        entity = HttpEntity(ContentTypes.`application/json`,problemId.toJson.compactPrint.getBytes))
                )
                .recoverWith {
                    case ex @ (_:DeserializationException | _:UnsupportedContentTypeException | _:IllegalRequestException) =>
                        Future(HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
                            ContentTypes.`application/json`, ApiMessage(s"The problem structure is invalid: ${ex.getMessage}").toJson.compactPrint.getBytes())))
                    case _ =>
                        val ex = new Exception("Unknown error.")
                        logger.error(ex.getMessage, ex)
                        Future(HttpResponse(StatusCodes.InternalServerError))
                }
        case request@HttpRequest(HttpMethods.GET, Uri.Path("/solution"), _, _, _) =>
            val id = request.uri.query().get("id")
            if (id.isDefined) {
                controller.getSolution(id.get) match {
                    case Left(sol) =>
                        sol match {
                            case solution: Some[List[Solution]] =>
                                if (solution.get.isEmpty) {
                                    Future(HttpResponse(StatusCodes.RetryWith,
                                        entity = HttpEntity(ContentTypes.`application/json`, ApiMessage(s"The solution is not ready yet.").toJson.compactPrint.getBytes())))
                                } else {
                                    Future(HttpResponse(StatusCodes.OK,
                                        entity = HttpEntity(ContentTypes.`application/json`, solution.get.head.toJson.compactPrint.getBytes)))
                                }
                            case None => Future(HttpResponse(StatusCodes.InternalServerError,
                                entity = HttpEntity(ContentTypes.`application/json`, ApiMessage(s"The solution will not be computed.").toJson.compactPrint.getBytes())))
                        }
                    case _ => Future(HttpResponse(StatusCodes.InternalServerError,
                        entity = HttpEntity(ContentTypes.`application/json`, ApiMessage(s"The solution will not be computed.").toJson.compactPrint.getBytes())))
                }
            } else {
                Future(HttpResponse(StatusCodes.NotFound))
            }

        case _ => Future(HttpResponse(StatusCodes.NotFound))
    }

    serverSource.to(Sink.foreach(_.handleWithAsyncHandler(requestHandler))).run()
}

case class Problem(volume: Int, items: List[Item])
case class Item(name: String, value: Int, volume: Int)
case class Solution(items: List[Item])
case class ApiMessage(message: String)

object KnapsackJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val itemFormat: RootJsonFormat[Item] = jsonFormat3(Item)
    implicit val problemFormat: RootJsonFormat[Problem] = jsonFormat2(Problem)
    implicit val solutionFormat: RootJsonFormat[Solution] = jsonFormat1(Solution)
    implicit val apiMessageFormat: RootJsonFormat[ApiMessage] = jsonFormat1(ApiMessage)
}


