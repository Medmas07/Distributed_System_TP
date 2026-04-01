# Environment Setup (RabbitMQ + PostgreSQL + Java)

# TP1

## Run RabbitMQ + Execute Producer / Consumer

### 1. Run RabbitMQ with Docker

```bash
docker run -d \
  --hostname rabbitmq \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

Web interface access:

```
http://localhost:15672
```

Login:

```
guest / guest
```

---

### 2. Project Structure

```
project/
 ├── lib/
 ├── Send.java
 ├── Receive.java
 ├── EmitLogDirect.java
 └── ReceiveLogsDirect.java
```

---

### 3. Compilation

Windows (PowerShell / CMD):

```bash
javac -cp ".;lib/*" *.java
```

Linux / Mac:

```bash
javac -cp ".:lib/*" *.java
```

---

### 4. Execution (TP1)

Terminal 1:

```bash
java -cp ".;lib/*" Receive
```

Terminal 2:

```bash
java -cp ".;lib/*" Send
```

Expected result:

```
Received: Hello World!
```

---

### 5. Execution (Part 2 — Routing)

Terminal 1:

```bash
java -cp ".;lib/*" ReceiveLogsDirect
```

Terminal 2:

```bash
java -cp ".;lib/*" EmitLogDirect error "Disk failure"
```

Expected result:

```
Received [error] Disk failure
```

---

### 6. Notes

* RabbitMQ must be running (Docker container active)
* Ports used:

  * 5672 → Java communication
  * 15672 → Web interface
* Without an active consumer (TP2), messages are lost
* TP2 queues are temporary (not visible after shutdown)

---

# TP2

## 1. Run RabbitMQ (Docker)

```bash
docker run -d \
  --hostname rabbit-host \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

---

## 2. Run PostgreSQL (Docker)

```bash
docker run -d \
  --name postgres-tp2 \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15
```

---

## 3. Create Databases

```bash
docker exec -it postgres-tp2 psql -U postgres
```

```sql
CREATE DATABASE sales_bo1;
CREATE DATABASE sales_bo2;
CREATE DATABASE sales_ho;
```

---

## 4. Create Tables

### BO1 / BO2

```sql
CREATE TABLE product_sales (
    sale_id INT PRIMARY KEY,
    product_name VARCHAR(100),
    quantity INT,
    unit_price NUMERIC(10,2),
    sale_date TIMESTAMP,
    synced BOOLEAN DEFAULT FALSE
);
```

### HO

```sql
CREATE TABLE consolidated_sales (
    id SERIAL PRIMARY KEY,
    branch_id VARCHAR(20),
    sale_id INT,
    product_name VARCHAR(100),
    quantity INT,
    unit_price NUMERIC(10,2),
    sale_date TIMESTAMP,
    UNIQUE (branch_id, sale_id)
);
```

---

## 5. Insert Test Data

```sql
INSERT INTO product_sales VALUES
(1, 'Laptop', 2, 3500.00, NOW(), false),
(2, 'Mouse', 5, 40.00, NOW(), false);
```

---

## 6. Compile the Java Project

```bash
javac -cp "lib/*" -d bin src/*.java
```

---

## 7. Run the Consumer (Head Office)

```bash
java -cp "bin;lib/*" HeadOfficeConsumer
```

---

## 8. Run the Producers (Branch Offices)

### BO1

```bash
java -cp "bin;lib/*" BranchProducer BO1 sales_bo1 bo1.sales
```

### BO2

```bash
java -cp "bin;lib/*" BranchProducer BO2 sales_bo2 bo2.sales
```

---

## 9. Verification

### RabbitMQ

* Queue: `ho.sales.queue`
* Messages: `Ready = 0`

### PostgreSQL (HO)

```sql
SELECT * FROM consolidated_sales;
```

Expected result:

* Data from BO1 and BO2 is present
* No duplication
