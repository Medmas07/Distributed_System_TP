import com.rabbitmq.client.*;

public class EmitLogDirect {

    private static final String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] args) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);

            String severity = args.length > 0 ? args[0] : "info";
            String message = args.length > 1 ? args[1] : "Log message";

            channel.basicPublish(EXCHANGE_NAME, severity, null, message.getBytes());

            System.out.println("Sent [" + severity + "] " + message);
        }
    }
}