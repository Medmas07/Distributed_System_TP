import com.rabbitmq.client.*;

public class ReceiveLogsDirect {

    private static final String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] args) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);

        String queueName = channel.queueDeclare().getQueue();

        String[] severities = {"error","warning"}; // filtrage

        for (String severity : severities) {
            channel.queueBind(queueName, EXCHANGE_NAME, severity);
        }

        System.out.println("Waiting for logs...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println("Received [" + delivery.getEnvelope().getRoutingKey() + "] " + message);
        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }
}