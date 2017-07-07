package com.cosminsanda

import com.rabbitmq.client.{Channel, Connection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Controller(val amqpConnection: Connection, val queueName: String) {

    val channel: Channel = amqpConnection.createChannel()
    channel.queueDeclare(queueName, true, false, false, null)
    channel.close()

    def solve(problem: Future[Problem]): Future[String] =
        problem.map(p => {
            val problemId = java.util.UUID.randomUUID.toString
            val items = p.items.map(i => transport.Item(i.name, i.value, i.volume))
            val domainProblem = com.cosminsanda.transport.Problem(problemId, p.volume, items)
            val channel = amqpConnection.createChannel()
            channel.basicPublish("", queueName, null, domainProblem.toByteArray)
            channel.close()
            problemId
        })

}
