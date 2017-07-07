package com.cosminsanda

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.rabbitmq.client.Connection
import com.typesafe.config.{Config, ConfigFactory}
import net.jodah.lyra.config.RecoveryPolicy
import net.jodah.lyra.util.Duration
import net.jodah.lyra.{ConnectionOptions, Connections}
import scalikejdbc.ConnectionPool

class DI extends AbstractModule {

    override def configure(): Unit = {

        val conf: Config = ConfigFactory.load

        val amqpConnection = Connections.create(
            new ConnectionOptions().withHost(conf.getString("amqp.host")),
            new net.jodah.lyra.config.Config()
                .withRecoveryPolicy(new RecoveryPolicy()
                    .withBackoff(Duration.seconds(1), Duration.seconds(30))
                    .withMaxAttempts(20)
                ))

        Class.forName("com.mysql.cj.jdbc.Driver")
        ConnectionPool.singleton(s"jdbc:mysql://${conf.getString("db.host")}:${conf.getString("db.port")}/" +
          s"${conf.getString("db.name")}?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false",
            conf.getString("db.user"), conf.getString("db.pass"))

        bind(classOf[Connection]).toInstance(amqpConnection)
        bind(classOf[String]).annotatedWith(Names.named("queueName")).toInstance(conf.getString("amqp.queueName"))
    }
}
