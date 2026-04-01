import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SaleRecord {

    public String branchId;
    public int saleId;
    public String productName;
    public int quantity;
    public BigDecimal unitPrice;
    public LocalDateTime saleDate;

    public SaleRecord(String branchId, int saleId, String productName,
                      int quantity, BigDecimal unitPrice, LocalDateTime saleDate) {
        this.branchId = branchId;
        this.saleId = saleId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.saleDate = saleDate;
    }

    @Override
    public String toString() {
        return branchId + " | " + saleId + " | " + productName;
    }
}