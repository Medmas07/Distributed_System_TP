public class AppConfig {

    // RabbitMQ
    public static final String RABBIT_HOST = "localhost";
    public static final int RABBIT_PORT = 5672;
    public static final String RABBIT_USER = "guest";
    public static final String RABBIT_PASS = "guest";

    public static final String EXCHANGE_NAME = "sales.sync";
    public static final String EXCHANGE_TYPE = "direct";
    public static final String HO_QUEUE = "ho.sales.queue";

    public static final String RK_BO1 = "bo1.sales";
    public static final String RK_BO2 = "bo2.sales";

    // PostgreSQL
    public static final String DB_HOST = "localhost";
    public static final int DB_PORT = 5432;
    public static final String DB_USER = "postgres";
    public static final String DB_PASS = "postgres";

    public static String jdbcUrl(String dbName) {
        return "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + dbName;
    }
}