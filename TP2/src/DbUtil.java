import java.sql.Connection;
import java.sql.DriverManager;

public class DbUtil {

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (Exception e) {
            throw new RuntimeException("Driver PostgreSQL introuvable", e);
        }
    }

    public static Connection openConnection(String databaseName) throws Exception {
        return DriverManager.getConnection(
                AppConfig.jdbcUrl(databaseName),
                AppConfig.DB_USER,
                AppConfig.DB_PASS
        );
    }

    public static Connection getConnection(String dbName) throws Exception {
        return openConnection(dbName);
    }
}
