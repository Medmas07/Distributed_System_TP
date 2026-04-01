import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MessageCodec {

    public static String encode(SaleRecord s) {
        return s.branchId + ";" +
               s.saleId + ";" +
               s.productName + ";" +
               s.quantity + ";" +
               s.unitPrice + ";" +
               s.saleDate;
    }

    public static SaleRecord decode(String msg) {
        String[] p = msg.split(";");

        return new SaleRecord(
                p[0],
                Integer.parseInt(p[1]),
                p[2],
                Integer.parseInt(p[3]),
                new BigDecimal(p[4]),
                LocalDateTime.parse(p[5])
        );
    }
}