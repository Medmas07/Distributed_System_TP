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

    public static Connection getConnection(String dbName) throws Exception {
        return DriverManager.getConnection(
                AppConfig.jdbcUrl(dbName),
                AppConfig.DB_USER,
                AppConfig.DB_PASS
        );
    }
}