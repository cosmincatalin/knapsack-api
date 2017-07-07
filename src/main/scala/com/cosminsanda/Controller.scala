package com.cosminsanda

import akka.http.scaladsl.model.{ErrorInfo, IllegalRequestException, StatusCodes}
import com.rabbitmq.client.{Channel, Connection}

class Controller(val amqpConnection: Connection, val queueName: String) {

    val channel: Channel = amqpConnection.createChannel()
    channel.queueDeclare(queueName, true, false, false, null)
    channel.close()

    def solve(problem: Problem): String = {
        if (problem.items.isEmpty) throw new IllegalRequestException(ErrorInfo("No items provided."),StatusCodes.BadRequest)
        val problemId = java.util.UUID.randomUUID.toString
        val items = problem.items.map(i => transport.Item(i.name, i.value, i.volume))
        val domainProblem = com.cosminsanda.transport.Problem(problemId, problem.volume, items)
        val channel = amqpConnection.createChannel()
        channel.basicPublish("", queueName, null, domainProblem.toByteArray)
        channel.close()
        problemId
    }
}
