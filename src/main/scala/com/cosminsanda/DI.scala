package com.cosminsanda

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.rabbitmq.client.Connection
import com.typesafe.config.{Config, ConfigFactory}
import net.jodah.lyra.config.{RecoveryPolicy, RetryPolicy}
import net.jodah.lyra.util.Duration
import net.jodah.lyra.{ConnectionOptions, Connections}
import org.apache.logging.log4j.{LogManager, Logger}
import scalikejdbc.ConnectionPool

class DI extends AbstractModule {

    val logger: Logger = LogManager.getLogger( this.getClass.getName )

    override def configure(): Unit = {

        val conf: Config = ConfigFactory.load

        logger.info("Connecting to RabbitMQ.")

        val amqpConnection = Connections.create(
            new ConnectionOptions()
                .withHost(conf.getString("amqp.host")),
            new net.jodah.lyra.config.Config()
                .withRetryPolicy(new RetryPolicy().withMaxAttempts(-1).withInterval(Duration.seconds(1)))
                .withRecoveryPolicy(new RecoveryPolicy().withMaxAttempts(-1).withInterval(Duration.seconds(1)))
        )

        logger.info("Connected to RabbitMQ.")
        logger.info("Connecting to MySQL.")

        Class.forName("com.mysql.cj.jdbc.Driver")
        ConnectionPool.singleton(s"jdbc:mysql://${conf.getString("db.host")}:${conf.getString("db.port")}/" +
          s"${conf.getString("db.name")}?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false",
            conf.getString("db.user"), conf.getString("db.pass"))

        logger.info("Connected to MySQL.")

        bind(classOf[Connection]).toInstance(amqpConnection)
        bind(classOf[String]).annotatedWith(Names.named("queueName")).toInstance(conf.getString("amqp.queueName"))
    }
}
