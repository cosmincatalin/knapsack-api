package com.cosminsanda

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.logging.log4j.{LogManager, Logger}
import spray.json._

import scala.concurrent.Future
import scala.language.postfixOps

object App extends App {

    val logger: Logger = LogManager.getLogger( this.getClass.getName )
    val conf: Config = ConfigFactory.load

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val serverSource = Http().bind(system.settings.config.getString("interface"), system.settings.config.getInt("port"))

    import KnapsackJsonProtocol._

    val requestHandler: HttpRequest => Future[HttpResponse] = {
        case HttpRequest(HttpMethods.POST, Uri.Path("/solve"), _, input, _) =>
            val problem = Unmarshal(input).to[Problem]
            logger.info(problem)
            Future(HttpResponse(StatusCodes.OK))
        case request@HttpRequest(HttpMethods.GET, Uri.Path("/solution"), _, _, _) =>
            val id = request.uri.query().get("id")
            if (id.isDefined) {
                if (id.toString.length == 0) {
                    Future(HttpResponse(StatusCodes.RetryWith))
                } else {
                    val solution = Solution(List(Item("knife", 10, 10)))
                    Future(HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, SolutionJsonFormat.write(solution).toJson.compactPrint.getBytes)))
                }
            } else {
                Future(HttpResponse(StatusCodes.NotFound))
            }

        case _ => Future(HttpResponse(StatusCodes.NotFound))
    }


    serverSource.to(Sink.foreach(_.handleWithAsyncHandler(requestHandler))).run()
}

case class Problem(size: Int, items: List[Item])
case class Item(name: String, value: Int, volume: Int)
case class Solution(items: List[Item])

object KnapsackJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val itemFormat: RootJsonFormat[Item] = jsonFormat3(Item)
    implicit val problemFormat: RootJsonFormat[Problem] = jsonFormat2(Problem)
    implicit val solutionFormat: RootJsonFormat[Solution] = rootFormat(lazyFormat(jsonFormat1(Solution)))
    implicit object SolutionJsonFormat extends JsonWriter[Solution]{
        def write(solution: Solution): JsValue = JsObject(
                "items" -> JsArray(solution.items.map(item => JsObject(
                    "name" -> JsString(item.name),
                    "value" -> JsNumber(item.value),
                    "volume" -> JsNumber(item.volume)
                )).toVector)
            ).toJson
    }
}


