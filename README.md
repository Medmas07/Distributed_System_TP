# Lancement de l’environnement TP2 (RabbitMQ + PostgreSQL + Java)

# TP1
## Lancer RabbitMQ + Exécuter Producer / Consumer

### 1. Lancer RabbitMQ avec Docker

```bash
docker run -d \
  --hostname rabbitmq \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

Accès interface web :

```
http://localhost:15672
```

Login :

```
guest / guest
```

---

### 2. Structure du projet

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

Windows (PowerShell / CMD) :

```bash
javac -cp ".;lib/*" *.java
```

Linux / Mac :

```bash
javac -cp ".:lib/*" *.java
```

---

### 4. Exécution (TP1)

Terminal 1 :

```bash
java -cp ".;lib/*" Receive
```

Terminal 2 :

```bash
java -cp ".;lib/*" Send
```

Résultat attendu :

```
Received: Hello World!
```

---

### 5. Exécution (part2 — Routing)

Terminal 1 :

```bash
java -cp ".;lib/*" ReceiveLogsDirect
```

Terminal 2 :

```bash
java -cp ".;lib/*" EmitLogDirect error "Disk failure"
```

Résultat attendu :

```
Received [error] Disk failure
```

---

### 6. Remarques

* RabbitMQ doit être actif (Docker lancé)
* Port utilisé :

  * 5672 → communication Java
  * 15672 → interface web
* Sans consumer actif (TP2), les messages sont perdus
* Les queues du TP2 sont temporaires (non visibles après arrêt)

# TP2
## 1. Lancer RabbitMQ (Docker)

```Powershell
docker run -d \
  --hostname rabbit-host \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
  ```

  
## 2. Lancer PostgreSQL (Docker)
```Powershell
docker run -d \
  --name postgres-tp2 \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15

  ```
## 3. Créer les bases de données
```Powershell
docker exec -it postgres-tp2 psql -U postgres
```
```SQL
CREATE DATABASE sales_bo1;
CREATE DATABASE sales_bo2;
CREATE DATABASE sales_ho;
```
## 4. Créer les tables
BO1 / BO2
```SQL
CREATE TABLE product_sales (
    sale_id INT PRIMARY KEY,
    product_name VARCHAR(100),
    quantity INT,
    unit_price NUMERIC(10,2),
    sale_date TIMESTAMP,
    synced BOOLEAN DEFAULT FALSE
);
```
HO
```SQL
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
## 5. Ajouter des données de test
```SQL
INSERT INTO product_sales VALUES
(1, 'Laptop', 2, 3500.00, NOW(), false),
(2, 'Mouse', 5, 40.00, NOW(), false);
```
## 6. Compiler le projet Java
```Powershell
javac -cp "lib/*" -d bin src/*.java
```
## 7. Lancer le Consumer (Head Office)
```Powershell
java -cp "bin;lib/*" HeadOfficeConsumer
```
## 8. Lancer les Producers (Branch Offices)
BO1
```Powershell
java -cp "bin;lib/*" BranchProducer BO1 sales_bo1 bo1.sales
```
BO2
```Powershell
java -cp "bin;lib/*" BranchProducer BO2 sales_bo2 bo2.sales
```
## 9. Vérification
RabbitMQ
Queue : ho.sales.queue
Messages : Ready = 0
PostgreSQL (HO)
```SQL
SELECT * FROM consolidated_sales;
```
Résultat attendu :

données de BO1 et BO2 présentes
aucune duplication