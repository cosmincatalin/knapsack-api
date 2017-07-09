package com.cosminsanda

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, OK, RetryWith}
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

    val controller = injector.getInstance(classOf[Proxy])

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val serverSource = Http().bind(system.settings.config.getString("interface"), system.settings.config.getInt("port"))

    import KnapsackJsonProtocol._

    val requestHandler: HttpRequest => Future[HttpResponse] = {
        case HttpRequest(HttpMethods.POST, Uri.Path("/solve"), _, input, _) =>
            Unmarshal(input).to[Problem]
                .map(controller.solve)
                .map(problemId => okStr(problemId))
                .recoverWith {
                    case ex @ (_:DeserializationException | _:UnsupportedContentTypeException | _:IllegalRequestException) =>
                        Future(badReqMessage(s"The problem structure is invalid: ${ex.getMessage}"))
                    case ex: Throwable =>
                        logger.error("Unknown error", ex)
                        Future(errorMessage("Unknown error."))
                }
        case request@HttpRequest(HttpMethods.GET, Uri.Path("/solution"), _, _, _) =>
            val id = request.uri.query().get("id")
            if (id.isDefined)
                controller.getSolution(id.get) match {
                    case Left(sol) =>
                        sol match {
                            case solution: Some[List[Solution]] =>
                                if (solution.get.isEmpty) Future(retryMessage("The solution is not ready yet."))
                                else Future(okSolution(solution.get.head))
                            case None => Future(errorMessage("The solution will not be computed."))
                        }
                    case _ => Future(HttpResponse(StatusCodes.NotFound))
                }
            else Future(HttpResponse(StatusCodes.NotFound))

        case _ => Future(HttpResponse(StatusCodes.NotFound))
    }

    logger.info("Started API.")
    serverSource.to(Sink.foreach(_.handleWithAsyncHandler(requestHandler))).run()

    def okSolution(solution: Solution) = HttpResponse(OK, entity = HttpEntity(`application/json`, solution.toJson.compactPrint.getBytes))
    def okStr(obj: String) = HttpResponse(OK, entity = HttpEntity(`application/json`, obj.toJson.compactPrint.getBytes))
    def retryMessage(message: String) = HttpResponse(RetryWith, entity = HttpEntity(`application/json`, ApiMessage(message).toJson.compactPrint.getBytes()))
    def errorMessage(message: String) = HttpResponse(InternalServerError, entity = HttpEntity(`application/json`, ApiMessage(message).toJson.compactPrint.getBytes()))
    def badReqMessage(message: String) = HttpResponse(BadRequest, entity = HttpEntity(`application/json`, ApiMessage(message).toJson.compactPrint.getBytes()))

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
