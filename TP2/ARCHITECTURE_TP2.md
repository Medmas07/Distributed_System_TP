# TP2 - Architecture et Appels

## 1) Carte du code (fichiers, variables, signatures, appels)

```mermaid
classDiagram
    class AppConfig {
        +String RABBIT_HOST
        +int RABBIT_PORT
        +String RABBIT_USER
        +String RABBIT_PASS
        +String EXCHANGE_NAME
        +String EXCHANGE_TYPE
        +String HO_QUEUE
        +String RK_BO1
        +String RK_BO2
        +String DB_HOST
        +int DB_PORT
        +String DB_USER
        +String DB_PASS
        +String jdbcUrl(String dbName)
    }

    class DbUtil {
        +Connection openConnection(String databaseName)
        +Connection getConnection(String dbName)
    }

    class SaleRecord {
        +String branchId
        +int saleId
        +String productName
        +int quantity
        +BigDecimal unitPrice
        +LocalDateTime saleDate
        +SaleRecord(String,int,String,int,BigDecimal,LocalDateTime)
        +String toString()
    }

    class MessageCodec {
        +String encodeSaleRecord(SaleRecord saleRecord)
        +SaleRecord decodeSaleRecord(String payload)
        +String encode(SaleRecord s)
        +SaleRecord decode(String msg)
    }

    class BranchProducer {
        -long POLL_INTERVAL_MS
        +main(String[] args)
        -publishChangedRows(String branchId, String routingKey, Channel channel, PreparedStatement psSelect, Map~Integer,String~ sentVersions)
    }

    class HeadOfficeConsumer {
        +main(String[] args)
        -insertSaleRecord(SaleRecord s)
        -insert(SaleRecord s)
    }

    class JdbcPreparedTesting {
        +main(String[] args)
    }

    class JdbcRetrieve {
        +main(String[] args)
    }

    BranchProducer --> AppConfig : lit constantes MQ
    BranchProducer --> DbUtil : openConnection(dbName)
    BranchProducer --> MessageCodec : encodeSaleRecord(s)
    BranchProducer --> SaleRecord : construit message metier

    HeadOfficeConsumer --> AppConfig : lit constantes MQ
    HeadOfficeConsumer --> MessageCodec : decodeSaleRecord(msg)
    HeadOfficeConsumer --> DbUtil : openConnection(\"sales_ho\")
    HeadOfficeConsumer --> SaleRecord : consomme objet metier

    JdbcPreparedTesting --> AppConfig : jdbcUrl/DB_USER/DB_PASS
    JdbcRetrieve --> AppConfig : jdbcUrl/DB_USER/DB_PASS
    DbUtil --> AppConfig : jdbcUrl + credentials
```

### Graphe d'appels (qui appelle quoi)

```mermaid
flowchart TD
    BPmain["BranchProducer.main(args)"] --> BPdb["DbUtil.openConnection(dbName)"]
    BPmain --> BPpoll["while(true) + POLL_INTERVAL_MS"]
    BPpoll --> BPpub["publishChangedRows(...)"]
    BPpub --> BPsql["SELECT ... xmin::text AS row_version FROM product_sales"]
    BPpub --> BPrecord["new SaleRecord(...)"]
    BPrecord --> BPenc["MessageCodec.encodeSaleRecord(s)"]
    BPenc --> BPsend["channel.basicPublish(exchange, routingKey, msg)"]

    HCmain["HeadOfficeConsumer.main(args)"] --> HCconsume["channel.basicConsume(HO_QUEUE, autoAck=false, callback)"]
    HCconsume --> HCdec["MessageCodec.decodeSaleRecord(msg)"]
    HCdec --> HCins["insertSaleRecord(s)"]
    HCins --> HCdb["DbUtil.openConnection('sales_ho')"]
    HCins --> HCupsert["INSERT ... ON CONFLICT(branch_id, sale_id) DO UPDATE ..."]
    HCconsume --> HCack["basicAck / basicNack"]

    JPmain["JdbcPreparedTesting.main(args)"] --> JPins["INSERT INTO consolidated_sales ... (PreparedStatement)"]
    JRmain["JdbcRetrieve.main(args)"] --> JRsel["SELECT sale_id, product_name FROM consolidated_sales ..."]
```

## 2) Architecture runtime (consumer, channel, exchange, queue)

```mermaid
flowchart LR
    subgraph BO1_DB["PostgreSQL: sales_bo1"]
        BO1T["table: product_sales"]
    end

    subgraph BO2_DB["PostgreSQL: sales_bo2"]
        BO2T["table: product_sales"]
    end

    subgraph BO1_APP["BranchProducer BO1"]
        BO1P["poll + publish\nrouting key = bo1.sales"]
    end

    subgraph BO2_APP["BranchProducer BO2"]
        BO2P["poll + publish\nrouting key = bo2.sales"]
    end

    subgraph MQ["RabbitMQ"]
        EX["Exchange: sales.sync (direct)"]
        Q["Queue: ho.sales.queue"]
    end

    subgraph HC_APP["HeadOfficeConsumer"]
        HC["basicConsume + basicAck/basicNack"]
    end

    subgraph HO_DB["PostgreSQL: sales_ho"]
        HOT["table: consolidated_sales\nUPSERT on (branch_id, sale_id)"]
    end

    BO1T --> BO1P
    BO2T --> BO2P
    BO1P -->|bo1.sales| EX
    BO2P -->|bo2.sales| EX
    EX -->|binding bo1.sales| Q
    EX -->|binding bo2.sales| Q
    Q --> HC
    HC --> HOT
```

### Sequence simplifiee (update BO -> update HC)

```mermaid
sequenceDiagram
    participant BO as BranchProducer (BO1/BO2)
    participant BODB as PostgreSQL BO
    participant EX as Exchange sales.sync
    participant Q as Queue ho.sales.queue
    participant HC as HeadOfficeConsumer
    participant HODB as PostgreSQL HO

    loop toutes les 2 secondes
        BO->>BODB: SELECT product_sales + xmin(row_version)
        BODB-->>BO: lignes inserees/modifiees
        BO->>EX: basicPublish(msg SaleRecord, routingKey bo1/bo2)
    end

    EX->>Q: routage direct via bindings
    HC->>Q: basicConsume(autoAck=false)
    Q-->>HC: delivery(msg)
    HC->>HODB: INSERT ... ON CONFLICT DO UPDATE
    HC->>Q: basicAck
```
