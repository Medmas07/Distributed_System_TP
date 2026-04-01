import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.AMQP;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;

public class BranchProducer {

    public static void main(String[] args) throws Exception {

        if (args.length != 3) {
            System.out.println("Usage: BO1 sales_bo1 bo1.sales");
            return;
        }

        String branchId = args[0];
        String dbName = args[1];
        String routingKey = args[2];

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(AppConfig.RABBIT_HOST);

        com.rabbitmq.client.Connection mqConnection = factory.newConnection();
        Channel channel = mqConnection.createChannel();

        java.sql.Connection dbConnection = DbUtil.getConnection(dbName);

        channel.exchangeDeclare(AppConfig.EXCHANGE_NAME, "direct", true);
        channel.queueDeclare(AppConfig.HO_QUEUE, true, false, false, null);

        channel.queueBind(AppConfig.HO_QUEUE, AppConfig.EXCHANGE_NAME, AppConfig.RK_BO1);
        channel.queueBind(AppConfig.HO_QUEUE, AppConfig.EXCHANGE_NAME, AppConfig.RK_BO2);

        String select = "SELECT * FROM product_sales WHERE synced = false";
        String update = "UPDATE product_sales SET synced = true WHERE sale_id = ?";

        PreparedStatement psSelect = dbConnection.prepareStatement(select);
        PreparedStatement psUpdate = dbConnection.prepareStatement(update);

        ResultSet rs = psSelect.executeQuery();

        while (rs.next()) {

            SaleRecord s = new SaleRecord(
                    branchId,
                    rs.getInt("sale_id"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getBigDecimal("unit_price"),
                    rs.getTimestamp("sale_date").toLocalDateTime()
            );

            String msg = MessageCodec.encode(s);

            channel.basicPublish(
                    AppConfig.EXCHANGE_NAME,
                    routingKey,
                    new AMQP.BasicProperties.Builder().deliveryMode(2).build(),
                    msg.getBytes(StandardCharsets.UTF_8)
            );

            psUpdate.setInt(1, s.saleId);
            psUpdate.executeUpdate();

            System.out.println("Envoye: " + s);
        }

        rs.close();
        psSelect.close();
        psUpdate.close();
        dbConnection.close();
        channel.close();
        mqConnection.close();
    }
}