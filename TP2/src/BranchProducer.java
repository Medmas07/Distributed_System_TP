import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.AMQP;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class BranchProducer {

    private static final long POLL_INTERVAL_MS = 2000;

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

        channel.exchangeDeclare(AppConfig.EXCHANGE_NAME, AppConfig.EXCHANGE_TYPE, true);
        channel.queueDeclare(AppConfig.HO_QUEUE, true, false, false, null);

        channel.queueBind(AppConfig.HO_QUEUE, AppConfig.EXCHANGE_NAME, AppConfig.RK_BO1);
        channel.queueBind(AppConfig.HO_QUEUE, AppConfig.EXCHANGE_NAME, AppConfig.RK_BO2);

        String select = "SELECT sale_id, product_name, quantity, unit_price, sale_date, xmin::text AS row_version " +
                        "FROM product_sales ORDER BY sale_id";

        try (java.sql.Connection dbConnection = DbUtil.openConnection(dbName);
             PreparedStatement psSelect = dbConnection.prepareStatement(select)) {

            Map<Integer, String> sentVersions = new HashMap<>();

            System.out.println("Surveillance active pour " + branchId + " (" + dbName + ").");
            System.out.println("Toute modification BO sera envoyee a HC.");

            while (true) {
                publishChangedRows(branchId, routingKey, channel, psSelect, sentVersions);
                Thread.sleep(POLL_INTERVAL_MS);
            }
        }
    }

    private static void publishChangedRows(
            String branchId,
            String routingKey,
            Channel channel,
            PreparedStatement psSelect,
            Map<Integer, String> sentVersions
    ) throws SQLException, IOException {

        try (ResultSet rs = psSelect.executeQuery()) {
            while (rs.next()) {

                int saleId = rs.getInt("sale_id");
                String currentVersion = rs.getString("row_version");
                String lastVersion = sentVersions.get(saleId);

                if (currentVersion.equals(lastVersion)) {
                    continue;
                }

                SaleRecord s = new SaleRecord(
                        branchId,
                        saleId,
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price"),
                        rs.getTimestamp("sale_date").toLocalDateTime()
                );

                String msg = MessageCodec.encodeSaleRecord(s);

                channel.basicPublish(
                        AppConfig.EXCHANGE_NAME,
                        routingKey,
                        new AMQP.BasicProperties.Builder().deliveryMode(2).build(),
                        msg.getBytes(StandardCharsets.UTF_8)
                );

                sentVersions.put(saleId, currentVersion);
                System.out.println("Envoye (insert/update): " + s);
            }
        }
    }
}
