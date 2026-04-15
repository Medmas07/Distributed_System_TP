import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcPreparedTesting {

    public static void main(String[] args) {

        String url = AppConfig.jdbcUrl("sales_ho");
        String user = AppConfig.DB_USER;
        String password = AppConfig.DB_PASS;

        String sql = "INSERT INTO consolidated_sales " +
                     "(branch_id, sale_id, product_name, quantity, unit_price, sale_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(sql)) {

            for (int i = 1; i <= 10; i++) {
                pst.setString(1, "TEST");
                pst.setInt(2, 900000 + i);
                pst.setString(3, "sample_product_" + i);
                pst.setInt(4, i);
                pst.setBigDecimal(5, new BigDecimal("10.00"));
                pst.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                pst.executeUpdate();
            }

        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(JdbcPreparedTesting.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
