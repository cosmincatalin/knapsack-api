package com.cosminsanda

import javax.inject.Named

import akka.http.scaladsl.model.{ErrorInfo, IllegalRequestException, StatusCodes}
import com.google.inject.Inject
import com.rabbitmq.client.{Channel, Connection}
import org.joda.time.DateTime
import scalikejdbc._
import spray.json._

class Proxy @Inject()(val amqpConnection: Connection, @Named("queueName") val queueName: String) {

    val channel: Channel = amqpConnection.createChannel()
    channel.queueDeclare(queueName, true, false, false, null)
    channel.close()

    import KnapsackJsonProtocol._

    def solve(problem: Problem): String = {
        if (problem.items.isEmpty) throw new IllegalRequestException(ErrorInfo("No items provided."),StatusCodes.BadRequest)
        val problemId = java.util.UUID.randomUUID.toString

        val db: DB = DB(ConnectionPool.borrow())
        db.localTx { implicit session =>
            sql"INSERT INTO problems(id, problem) VALUES ($problemId, ${problem.toJson.compactPrint})".update().apply()
        }
        db.close()

        val items = problem.items.map(i => transport.Item(i.name, i.value, i.volume))
        val domainProblem = com.cosminsanda.transport.Problem(problemId, problem.volume, items)
        val channel = amqpConnection.createChannel()
        channel.basicPublish("", queueName, null, domainProblem.toByteArray)
        channel.close()
        problemId
    }

    def getSolution(id: String): Either[Option[List[Solution]], Exception] = {
        case class InternalSolution(solution: Option[String], solved: Option[DateTime], wontsolve: Option[DateTime])

        val db: DB = DB(ConnectionPool.borrow())
        val sol: Option[InternalSolution] = DB readOnly { implicit session =>
            sql"SELECT solution, solved, wontsolve FROM problems WHERE id = $id"
              .map(rs => InternalSolution(rs.stringOpt("solution"), rs.jodaDateTimeOpt("solved"), rs.jodaDateTimeOpt("wontsolve")))
              .single().apply()
        }
        db.close()

        sol match {
            case solution: Some[InternalSolution] =>
                if (solution.get.wontsolve.isDefined) {
                    Left(None)
                } else if (solution.get.solved.isDefined) {
                    Left(solution.map(_.solution.get).map(_.parseJson).map(_.convertTo[Solution]).map(List(_)))
                } else {
                    Left(Option(List.empty))
                }
            case _ => Right(new Exception("Problem does not exist."))
        }

    }
}
