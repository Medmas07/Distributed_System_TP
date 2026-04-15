import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MessageCodec {

    public static String encodeSaleRecord(SaleRecord saleRecord) {
        return saleRecord.branchId + ";" +
               saleRecord.saleId + ";" +
               saleRecord.productName + ";" +
               saleRecord.quantity + ";" +
               saleRecord.unitPrice + ";" +
               saleRecord.saleDate;
    }

    public static SaleRecord decodeSaleRecord(String payload) {
        String[] parts = payload.split(";");

        return new SaleRecord(
                parts[0],
                Integer.parseInt(parts[1]),
                parts[2],
                Integer.parseInt(parts[3]),
                new BigDecimal(parts[4]),
                LocalDateTime.parse(parts[5])
        );
    }

    public static String encode(SaleRecord s) {
        return encodeSaleRecord(s);
    }

    public static SaleRecord decode(String msg) {
        return decodeSaleRecord(msg);
    }
}
