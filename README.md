# Inventory API

A simple REST API for products, stock, and orders.

## What the application can do

- Create, view, update, and delete products.
- Create and view orders.
- Check stock before creating an order.
- Reduce stock after a successful order.
- Calculate the order total.
- Save the product price used in each order.
- Cancel the whole order if there is not enough stock.
- Check input data and return errors.

## Technologies

- Java 17
- Spring Boot 4.1.2
- Spring Web MVC
- Spring Data JPA and Hibernate
- Bean Validation
- PostgreSQL 18
- Flyway
- JUnit and Mockito
- Maven Wrapper
- Docker and Docker Compose

## Run the application with Docker

Docker Compose starts two services:

- `app` — the application: http://127.0.0.1:8080
- `db` — PostgreSQL with the `inventory_docker_db` database

PostgreSQL data is saved in the `postgres_data` volume.
The database port is not opened on the host computer.

### Before you start

You need:

- JDK 17;
- Docker Desktop with Docker Compose;
- `JAVA_HOME` set to your JDK folder.

Run these PowerShell commands from the project root.

1. Create the `.env` file:

   ```powershell
   Copy-Item .env.example .env

 If `.env` already exists, keep it.

2. Open `.env` and set the database password:

```dotenv
DOCKER_DB_PASSWORD='your_password_here'
```
Use the same password every time you start the existing database.
Changing `.env` does not change the password in an existing database.
`.env` is not stored in Git. Do not share it.

3. Build the JAR without running tests:

```powershell
.\mvnw.cmd package -DskipTests
```

This command builds the JAR without running tests. Therefore, local PostgreSQL and the `inventory_test_db` database are not required for the first Docker start.

4. Check the configuration and start the containers:

```powershell
docker compose config --quiet
docker compose up -d --build
```

The Dockerfile copies the JAR from `target`.
After code changes, build the JAR again before rebuilding and starting Docker Compose.

### Check that the application is running

```powershell
docker compose ps -a
docker compose logs --tail=100 app
curl.exe -i http://127.0.0.1:8080/api/products
```

A successful request returns HTTP 200.
On a new database, the product list is empty: `[]`.

Flyway applies database migrations automatically when the application starts.

### Stop and start again

```powershell
docker compose down
docker compose up -d
```

The `down` command keeps the database volume and its data.
The `-v` option removes the volume and all database data.
## Product price migration (V2)

On a new database, Flyway runs all migrations automatically.

If the `products` table already contains products after V1, prepare the data before running V2:

1. Stop the application and make a database backup.
2. Add the column `price NUMERIC(12, 2)` and allow `NULL` values.
3. Add prices for all existing products using an approved price list.
4. Make sure every product has a price greater than zero.
5. Start the application to run the migration.

V2 checks the prices and then adds the `NOT NULL` rule.
If a price is missing, zero, or negative, the migration stops with an error.
Prepare the prices and run the migration again.
The migration does not create default prices.

### Databases where the old V2 was already applied

The V2 file was changed during this project.
For an existing database, Flyway will report a checksum mismatch.

Before running `repair` once:

- make a database backup;
- check that `price` is `NUMERIC(12, 2)` and `NOT NULL`;
- check that there are no `NULL` or non-positive prices;
- run `validate` and make sure the mismatch is only for V2.

Then run `repair` with the same migration files and run `validate` again.

`repair` updates the Flyway history. It does not change table data or structure, and it does not fill in product prices. It does not run automatically when the application starts.

Future database changes must use new migration files.

Six `ProductPriceMigrationTest` tests check the empty database, prepared prices, rollback and retry, zero prices, and database constraints.
## Tests

Local tests require PostgreSQL running on `localhost:5432`.

Use a separate database named `inventory_test_db` and the `postgres` user.

If the database does not exist, create it in PostgreSQL:

```sql
CREATE DATABASE inventory_test_db;
```

Integration tests change data, so do not use your main development database.

In PowerShell, set the password for the local `postgres` user:

```powershell
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Local PostgreSQL password' -AsSecureString)).Password
```

Run the tests:

```powershell
.\mvnw.cmd test
```

Or build the JAR and run the tests:

```powershell
.\mvnw.cmd package
```

`DB_PASSWORD` is available only in the current terminal.
The `.env` file is used by Docker Compose and is not loaded by Maven.
`DOCKER_DB_PASSWORD` is used only for the Docker database.

## Product validation and error responses

Product names must not be blank and can contain up to 255 characters.
The stock quantity must be zero or greater.
The product price must be greater than zero and have no more than two decimal places.

The application checks product data before saving it. The database also enforces rules for non-blank names, non-negative stock, and positive prices.

Validation errors return HTTP 400 with a `message` field and an `errors` object. The `errors` object contains messages for invalid fields.
Other handled API errors return a JSON response with a `message` field.

`ProductControllerTest` has four tests. They check that product creation and updates accept names with 255 characters and reject names with 256 characters.

## API documentation — Swagger UI and OpenAPI

After the application starts, you can open:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Swagger UI describes the product and order operations, request fields, response models, and status codes.

To send a request:

1. Open an operation.
2. Click `Try it out`.
3. Enter the request data.
4. Click `Execute`.

The real response appears under `Server response`.
`Example Value` shows an example only.

POST, PUT, and DELETE requests change data in the connected database.
Sending the same POST request again can create another product or order.
## API example

Run these PowerShell commands while the application is running.

### Create a product

```powershell
$productBody = @{ name = 'Demo product'; quantity = 10; price = 125.50 } | ConvertTo-Json
$product = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/api/products' -ContentType 'application/json' -Body $productBody
$product
```

### Create an order

The application takes the price from the product catalog.

```powershell
$orderBody = @{ items = @(@{ productId = $product.id; quantity = 2 }) } | ConvertTo-Json -Depth 5
$order = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/api/orders' -ContentType 'application/json' -Body $orderBody
$order | ConvertTo-Json -Depth 5
```

The expected order total is `251.00`.
The product quantity decreases from 10 to 8.

### Read the order and products

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/orders/$($order.id)" | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/products' | ConvertTo-Json -Depth 5
```

Sending the same POST request again creates another product or order.
## Dependency check

The test dependency `json-smart` was removed after IntelliJ reported a security warning.

The following changes were made:

- removed `json-smart` and `accessors-smart` from the test dependency tree;
- updated JsonPath to version 3.0.0;
- changed JSON checks in controller tests to use Jackson 3;
- verified that Maven no longer includes the removed dependencies.

All 30 tests passed after these changes.

This check confirms that the reported dependency was removed. It is not a complete security audit.

## Automatic checks (CI)

GitHub Actions runs checks when code is pushed and when a Pull Request is opened or updated.

The check runs on Ubuntu 24.04 with Java 17 and:

- starts a separate PostgreSQL 18 database named `inventory_test_db`;
- runs all tests;
- builds the application JAR with Maven.

The command used by CI is:

```text
./mvnw --batch-mode --no-transfer-progress clean package
```

The results and logs are available on the repository's `Actions` page.