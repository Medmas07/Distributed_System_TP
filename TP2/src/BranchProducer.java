import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.AMQP;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class BranchProducer {

    private static final long POLL_INTERVAL_MS = 2000;
    private static final DateTimeFormatter HOUR_MINUTE_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    public static void main(String[] args) throws Exception {

        if (args.length != 3 && args.length != 5) {
            printUsage();
            return;
        }

        String branchId = args[0];
        String dbName = args[1];
        String routingKey = args[2];
        AvailabilityWindow hoWindow;

        try {
            hoWindow = parseAvailabilityWindow(args);
        } catch (IllegalArgumentException ex) {
            System.out.println("Arguments invalides: " + ex.getMessage());
            printUsage();
            return;
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(AppConfig.RABBIT_HOST);

        com.rabbitmq.client.Connection mqConnection = factory.newConnection();
        Channel channel = mqConnection.createChannel();

        channel.exchangeDeclare(AppConfig.EXCHANGE_NAME, AppConfig.EXCHANGE_TYPE, true);
        channel.queueDeclare(AppConfig.HO_QUEUE, true, false, false, null);
        // true : queue durable
        // false : non exclusive
        // false : ne pas supprimer automatiquement
        // null : pas d’options supplémentaires
        channel.queueBind(AppConfig.HO_QUEUE, AppConfig.EXCHANGE_NAME, AppConfig.RK_BO1);
        channel.queueBind(AppConfig.HO_QUEUE, AppConfig.EXCHANGE_NAME, AppConfig.RK_BO2);

        String select = "SELECT sale_id, product_name, quantity, unit_price, sale_date, xmin::text AS row_version " +
                        "FROM product_sales ORDER BY sale_id";

        //         Rôle de xmin

        // xmin est une colonne système interne à PostgreSQL.
        // Elle change quand la ligne est modifiée.

        // Le programme s’en sert comme version technique.

        // Exemple :

        // une ligne a xmin = 701
        // après un UPDATE, elle peut passer à xmin = 702

        // Donc si xmin change, le programme considère que la ligne a changé.
        try (java.sql.Connection dbConnection = DbUtil.openConnection(dbName);
             PreparedStatement psSelect = dbConnection.prepareStatement(select)) {

            Map<Integer, String> sentVersions = new HashMap<>();
            boolean windowWasOpen = true;

            System.out.println("Surveillance active pour " + branchId + " (" + dbName + ").");
            if (hoWindow == null) {
                System.out.println("Toute modification BO sera envoyee a HC.");
            } else {
                System.out.println("Envoi vers HC uniquement entre " +
                        hoWindow.start + " et " + hoWindow.end + ".");
            }

            while (true) {
                boolean windowIsOpen = hoWindow == null || hoWindow.isOpenNow();

                if (windowIsOpen) {
                    if (!windowWasOpen) {
                        System.out.println("HO disponible: reprise de la synchro.");
                    }
                    publishChangedRows(branchId, routingKey, channel, psSelect, sentVersions);
                } else if (windowWasOpen) {
                    System.out.println("HO indisponible: pause de la synchro jusqu'a la prochaine fenetre.");
                }

                windowWasOpen = windowIsOpen;
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
                      //  deliveryMode(2)
                        //Le message est marqué persistant.
                        //Cela signifie : RabbitMQ doit essayer de conserver le message sur disque, pas uniquement en mémoire.
                        new AMQP.BasicProperties.Builder().deliveryMode(2).build(),
                        msg.getBytes(StandardCharsets.UTF_8)
                );

                sentVersions.put(saleId, currentVersion);
                System.out.println("Envoye (insert/update): " + s);
            }
        }
    }

    private static void printUsage() {
        System.out.println("Usage: BranchProducer <branchId> <dbName> <routingKey> [hoStartHour] [hoEndHour]");
        System.out.println("Exemples:");
        System.out.println("  BranchProducer BO1 sales_bo1 bo1.sales");
        System.out.println("  BranchProducer BO1 sales_bo1 bo1.sales 09 11");
        System.out.println("  BranchProducer BO2 sales_bo2 bo2.sales 09:30 11:30");
    }

    private static AvailabilityWindow parseAvailabilityWindow(String[] args) {
        if (args.length == 3) {
            return null;
        }

        LocalTime start = parseHourOrHourMinute(args[3]);
        LocalTime end = parseHourOrHourMinute(args[4]);
        return new AvailabilityWindow(start, end);
    }

    private static LocalTime parseHourOrHourMinute(String value) {
        if (value.matches("\\d{1,2}")) {
            int hour = Integer.parseInt(value);
            if (hour < 0 || hour > 23) {
                throw new IllegalArgumentException("heure hors intervalle [0..23]: " + value);
            }
            return LocalTime.of(hour, 0);
        }

        if (value.matches("\\d{1,2}:\\d{2}")) {
            return LocalTime.parse(value, HOUR_MINUTE_FORMAT);
        }

        throw new IllegalArgumentException("format heure invalide: " + value +
                " (attendu: HH ou HH:mm)");
    }

    private static class AvailabilityWindow {
        private final LocalTime start;
        private final LocalTime end;

        private AvailabilityWindow(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }

        private boolean isOpenNow() {
            LocalTime now = LocalTime.now();

            if (start.equals(end)) {
                return true;
            }

            if (start.isBefore(end)) {
                return !now.isBefore(start) && now.isBefore(end);
            }

            return !now.isBefore(start) || now.isBefore(end);
        }
    }
}
