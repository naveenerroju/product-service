# Products Application

A Spring Boot CRUD application for managing products with AWS RDS MySQL.

## Prerequisites

- Java 17 or later
- Maven 3.6+
- Docker (optional, for local MySQL)
- AWS Account (for RDS setup)
- MySQL Workbench (for database management)

## Project Structure

```
products/
├── src/main/java/com.github.products/
│   ├── controller/ProductController.java
│   ├── dto/Product.java
│   ├── repository/ProductRepository.java
│   ├── service/ProductService.java
│   └── ProductsApplication.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   ├── application-dev.yml
│   └── application-test.yml
├── Dockerfile
├── pom.xml
└── README.md
```

## Environment Profiles

| Profile | Database | Purpose |
|---------|----------|---------|
| `local` | H2 (In-memory) | Local development |
| `dev` | MySQL (Local/Docker) | Development testing |
| `test` | AWS RDS MySQL | Integration testing |

---

## AWS RDS Setup (Step-by-Step)

### Step 1 — Create the RDS Instance

1. Login to **AWS Console** → navigate to **RDS**
2. Click **"Create database"** → choose **Standard create**
3. Engine: **MySQL Community**
4. Template: **Free tier** (db.t4g.micro)
5. Settings:
   - DB instance identifier: `product-service`
   - Master username: `admin`
   - Master password: `<your-secure-password>`
6. Under **Connectivity**:
   - Set **Public access: Yes**
   - Assign a security group (see Step 2)
   - DB subnet group: use a group that contains **public subnets** (subnets with a route to an Internet Gateway)
7. Click **Create database** and wait ~5 minutes for status to show **Available**

> ⚠️ **Important — Subnet Warning:** Your DB subnet group must have a **public subnet in the same Availability Zone** where RDS is placed. Even with Public access enabled, if the RDS instance lands in a private subnet (no IGW route), external connections will fail. Check your subnets' route tables to confirm they have a `0.0.0.0/0 → igw-xxxx` route.

---

### Step 2 — Configure Security Group

Create or use a security group with the following **inbound rules**:

| Type | Protocol | Port | Source |
|------|----------|------|--------|
| MySQL/Aurora | TCP | 3306 | 0.0.0.0/0 |
| SSH | TCP | 22 | 0.0.0.0/0 |
| HTTP | TCP | 80 | 0.0.0.0/0 |
| PostgreSQL | TCP | 5432 | 0.0.0.0/0 |

> For production, restrict the source to your specific IP instead of `0.0.0.0/0`.

---

### Step 3 — Create the Database Schema

Connect to RDS using **MySQL Workbench**:

**Connection settings:**
- Hostname: `product-service.cnwagymg8eyz.ap-south-1.rds.amazonaws.com`
- Port: `3306`
- Username: `admin`
- SSL tab → SSL CA File: path to `global-bundle.pem` (downloaded from AWS)

Once connected, create your database:

```sql
CREATE DATABASE productdb;
```

Create the product table:

```sql
CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    price DOUBLE
);
```

Insert sample data:

```sql
INSERT INTO product (name, price) VALUES
('iPhone 15', 999.99),
('Samsung Galaxy S24', 849.99),
('MacBook Pro', 1999.99),
('Dell XPS 15', 1499.99),
('Sony WH-1000XM5', 349.99);
```

---

### Step 4 — Configure Spring Boot (application-test.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://${RDS_HOSTNAME}:3306/${RDS_DB_NAME}
    username: ${RDS_USERNAME}
    password: ${RDS_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true

server:
  port: 8080
```

> ⚠️ **Common Mistakes to Avoid:**
> - Do NOT use `H2Dialect` when connecting to MySQL — always use `MySQLDialect`
> - Do NOT use `create-drop` for `ddl-auto` in persistent environments — it drops all tables on restart. Use `update` for dev/test
> - The JDBC URL **must** start with `jdbc:mysql://` and end with the database name (e.g. `/productdb`)
> - Do NOT connect to the `mysql` system database — always create and use your own database

---

### Step 5 — Run the Application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/products | Get all products |
| GET | /api/products/{id} | Get product by ID |
| POST | /api/products | Create new product |
| PUT | /api/products/{id} | Update product |
| DELETE | /api/products/{id} | Delete product |

### Sample Request Body

```json
{
  "name": "Laptop",
  "price": 999.99
}
```

### Testing with curl

```bash
# Create product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","price":1299.99}'

# Get all products
curl http://localhost:8080/api/products

# Get by ID
curl http://localhost:8080/api/products/1

# Update product
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Gaming Laptop","price":1499.99}'

# Delete product
curl -X DELETE http://localhost:8080/api/products/1
```

---

## Local Development Setup

### Profile: local (H2 Database)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

H2 Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:productdb`
- Username: `sa`
- Password: `password`

### application-local.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:productdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: password
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  h2:
    console:
      enabled: true
      path: /h2-console
```

### Profile: dev (Local MySQL via Docker)

```bash
docker run --name mysql-dev \
  -e MYSQL_DATABASE=productdb \
  -e MYSQL_USER=devuser \
  -e MYSQL_PASSWORD=devpass \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -p 3306:3306 \
  -d mysql:8.0

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### application-dev.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/productdb?useSSL=false&serverTimezone=UTC
    username: devuser
    password: devpass
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
```

---

## Troubleshooting

### `claims to not accept jdbcUrl`
Your JDBC URL is missing the `jdbc:mysql://` prefix. The URL must follow this exact format:
```
jdbc:mysql://<host>:<port>/<database_name>
```

### `Access denied for user 'admin'@'%' to database 'mysql'`
You're connecting to the MySQL system database. Make sure your JDBC URL ends with your own database name (e.g. `/productdb`), not `/mysql`.

### `Unable to build Hibernate SessionFactory`
You're likely using `H2Dialect` while connecting to MySQL. Change `database-platform` to `org.hibernate.dialect.MySQLDialect`.

### RDS Connection Timeout / Cannot Connect from Workbench
- Check that **Public access** is set to **Yes** on the RDS instance
- Verify the subnet your RDS instance is in has a route table with `0.0.0.0/0 → igw-xxxx`
- Confirm security group inbound rules allow port **3306**

---

## Security Best Practices

- Never commit credentials to Git — use environment variables or AWS Secrets Manager
- Restrict security group source to your specific IP in production (not `0.0.0.0/0`)
- Use private subnets for RDS in production
- Enable encryption at rest and automated backups for production databases
- Use `validate` for `ddl-auto` in production (never `create-drop`)

---

## Clean Up AWS Resources

```bash
# Delete RDS instance
aws rds delete-db-instance \
  --db-instance-identifier product-service \
  --skip-final-snapshot

# Delete security group
aws ec2 delete-security-group \
  --group-id <security-group-id>
```
