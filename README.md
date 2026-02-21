Product POC – Spring Boot + MySQL + Aurora
🚀 Overview

This is a minimal Spring Boot proof of concept demonstrating:

Product CRUD (basic)

Spring Data JPA

Profile-based configuration

Local MySQL support

AWS Aurora MySQL support

Dockerized application

This project is intentionally simple and suitable for quick validation or demos — not production-ready.

🧱 Tech Stack

Java 17

Spring Boot

Spring Data JPA

MySQL / Aurora MySQL

Maven

Docker

📁 Project Structure
src/main/java/com/example/product
 ├── controller
 ├── entity
 ├── repository
 ├── service
 └── ProductApplication

src/main/resources
 ├── application.yml
 ├── application-local.yml
 └── application-dev.yml
⚙️ Profiles
Profile	Database
local	Local MySQL
dev	AWS Aurora MySQL
▶️ Running the Application
✅ Run with local profile
mvn spring-boot:run -Dspring-boot.run.profiles=local

or

java -jar target/product-poc.jar --spring.profiles.active=local
✅ Run with dev (Aurora)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
🗄️ Database Setup
Local MySQL

Create database:

CREATE DATABASE product;

Ensure credentials in:

application-local.yml
Aurora MySQL

Update in:

application-dev.yml

Replace:

endpoint

username

password

Aurora is MySQL-compatible, so same driver is used.

🔥 Table Creation

Hibernate auto-DDL is enabled:

spring:
  jpa:
    hibernate:
      ddl-auto: update

On startup, tables are created automatically if missing.

📬 API Testing (Postman)
➜ Create Product

POST

http://localhost:8080/products

Body:

{
  "name": "iPhone",
  "price": 999.99
}
➜ Get All Products

GET

http://localhost:8080/products
🐳 Docker
1. Build jar
mvn clean package
2. Build image
docker build -t product-poc:latest .
3. Run container (local profile)

Mac/Windows:

docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/product \
  product-poc:latest

Linux:

docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://172.17.0.1:3306/product \
  product-poc:latest
4. Run with Aurora
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  product-poc:latest
🧪 Quick Verification

On startup logs you should see:

The following profiles are active: <profile>

If not — your profile is not applied.

⚠️ Known Limitations (Intentional)

This is a POC. It does not include:

DTO layer

Validation

Exception handling

Flyway/Liquibase

Connection pool tuning

Security

🧠 Production Notes (Important)

If you ever evolve this:

Do not use ddl-auto=update in production

Use Flyway or Liquibase

Tune HikariCP

Add health checks

Add proper logging

👨‍💻 Author

POC created for rapid backend validation and environment testing.

Done. Minimal. Functional. Extend as needed.
