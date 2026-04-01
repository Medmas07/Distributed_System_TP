import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

public class HeadOfficeConsumer {

    public static void main(String[] args) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(AppConfig.RABBIT_HOST);

        com.rabbitmq.client.Connection mqConnection = factory.newConnection();
        Channel channel = mqConnection.createChannel();

        channel.exchangeDeclare(AppConfig.EXCHANGE_NAME, "direct", true);
        channel.queueDeclare(AppConfig.HO_QUEUE, true, false, false, null);

        channel.queueBind(AppConfig.HO_QUEUE, AppConfig.EXCHANGE_NAME, AppConfig.RK_BO1);
        channel.queueBind(AppConfig.HO_QUEUE, AppConfig.EXCHANGE_NAME, AppConfig.RK_BO2);

        channel.basicConsume(AppConfig.HO_QUEUE, false, (tag, delivery) -> {

            String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);

            try {
                SaleRecord s = MessageCodec.decode(msg);

                insert(s);

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                System.out.println("Recu: " + s);

            } catch (Exception e) {
                e.printStackTrace();
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            }

        }, tag -> {});
    }

    private static void insert(SaleRecord s) throws Exception {

        Connection conn = DbUtil.getConnection("sales_ho");

        String sql = "INSERT INTO consolidated_sales " +
                "(branch_id, sale_id, product_name, quantity, unit_price, sale_date) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setString(1, s.branchId);
        ps.setInt(2, s.saleId);
        ps.setString(3, s.productName);
        ps.setInt(4, s.quantity);
        ps.setBigDecimal(5, s.unitPrice);
        ps.setTimestamp(6, Timestamp.valueOf(s.saleDate));

        ps.executeUpdate();

        ps.close();
        conn.close();
    }
}