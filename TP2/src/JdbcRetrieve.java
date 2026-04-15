import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcRetrieve {

    public static void main(String[] args) {

        String url = AppConfig.jdbcUrl("sales_ho");
        String user = AppConfig.DB_USER;
        String password = AppConfig.DB_PASS;

        String query = "SELECT sale_id, product_name FROM consolidated_sales ORDER BY sale_id LIMIT 20";

        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                System.out.print(rs.getInt(1));
                System.out.print(": ");
                System.out.println(rs.getString(2));
            }

        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(JdbcRetrieve.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
