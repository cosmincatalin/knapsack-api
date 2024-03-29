FROM java:8

ENV AMQP_HOST="messagebus"
ENV AMPQ_QUEUE="knapsack"
ENV KAPP_INTERFACE="0.0.0.0"
ENV KAPP_PORT=5000
ENV DB_HOST="db"
ENV DB_NAME="knapsack"
ENV DB_USER="root"
ENV DB_PORT=3306
ENV DB_PASS=""

EXPOSE $KAPP_PORT

VOLUME /app

WORKDIR /app

CMD ["java", "-jar", "knapsack-api.jar"]