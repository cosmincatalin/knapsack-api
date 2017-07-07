package com.cosminsanda

import akka.http.scaladsl.model.{ErrorInfo, IllegalRequestException, StatusCodes}
import com.rabbitmq.client.{Channel, Connection}
import scalikejdbc._
import spray.json._

class Controller(val amqpConnection: Connection, val queueName: String, val dbPool: ConnectionPool.type ) {

    val channel: Channel = amqpConnection.createChannel()
    channel.queueDeclare(queueName, true, false, false, null)
    channel.close()

    import KnapsackJsonProtocol._

    def solve(problem: Problem): String = {
        if (problem.items.isEmpty) throw new IllegalRequestException(ErrorInfo("No items provided."),StatusCodes.BadRequest)
        val problemId = java.util.UUID.randomUUID.toString

        val dBConnection = ConnectionPool.borrow()
        val db: DB = DB(dBConnection)
        db.localTx { implicit sesssion =>
            sql"INSERT INTO problems(id, problem) VALUES ($problemId, ${problem.toJson.compactPrint})".update().apply()
        }

        val items = problem.items.map(i => transport.Item(i.name, i.value, i.volume))
        val domainProblem = com.cosminsanda.transport.Problem(problemId, problem.volume, items)
        val channel = amqpConnection.createChannel()
        channel.basicPublish("", queueName, null, domainProblem.toByteArray)
        channel.close()
        problemId
    }
}
