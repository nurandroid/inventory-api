# Inventory API

Учебный REST API для управления товарами, остатками и заказами.

## Возможности

- Создание, просмотр, изменение и удаление товаров.
- Оформление и просмотр заказов.
- Проверка наличия товаров и списание остатков при оформлении заказа.
- Расчёт стоимости заказа по ценам каталога.
- Сохранение цены каждой позиции на момент оформления заказа.
- Откат всей операции при недостатке товара.
- Проверка входных данных и обработка ошибок.

## Технологии

- Java 17, Spring Boot 4.1.2.
- Spring Web MVC, Spring Data JPA, Hibernate, Bean Validation.
- PostgreSQL 18, Flyway.
- JUnit, Mockito, интеграционные тесты.
- Maven Wrapper, Docker, Docker Compose.

## Запуск в Docker

Docker Compose запускает два сервиса:

- `app` — приложение, доступное по адресу http://127.0.0.1:8080.
- `db` — PostgreSQL с отдельной базой `inventory_docker_db`.

Данные PostgreSQL сохраняются в именованном томе `postgres_data`.
Порт базы не публикуется на компьютере.
### Подготовка

Нужны JDK 17 и запущенный Docker Desktop с Docker Compose.
Переменная `JAVA_HOME` должна указывать на папку JDK.

Команды ниже предназначены для PowerShell. Выполняйте их из корня проекта.

1. Создайте локальный файл настроек:

   ```powershell
   Copy-Item .env.example .env
   ```

   Если `.env` уже существует, используйте его без перезаписи.

2. В файле `.env` задайте пароль PostgreSQL:

   ```dotenv
   DOCKER_DB_PASSWORD='your_password_here'
   ```

   При первом запуске этот пароль будет установлен для пользователя
   `inventory`. При последующих запусках используйте тот же пароль:
   изменение `.env` не меняет пароль в уже созданной базе.

   `.env` исключён из Git. Не публикуйте его содержимое.

3. Соберите JAR приложения:

   ```powershell
   .\mvnw.cmd package
   ```

   Сборка запускает тесты. Для тестов требуется локальная PostgreSQL
   и соответствующая конфигурация подключения.
   Пароль локальной базы передаётся отдельно от пароля базы Docker.

4. Проверьте конфигурацию и запустите контейнеры:

   ```powershell
   docker compose config --quiet
   docker compose up -d --build
   ```

   Dockerfile копирует готовый JAR из `target`.
   После изменения кода сначала повторите сборку Maven,
   затем сборку и запуск Docker Compose.

### Проверка запуска

```powershell
docker compose ps -a
docker compose logs --tail=100 app
curl.exe -i http://127.0.0.1:8080/api/products
```

При успешном запуске API возвращает HTTP 200.
В новой базе список товаров пуст: `[]`.

Flyway автоматически применяет миграции при запуске приложения.

### Остановка и повторный запуск

```powershell
docker compose down
docker compose up -d
```

Команда `down` без параметра `-v` сохраняет том с данными.
Параметр `-v` удаляет том базы вместе с данными.
## Тесты

Для тестов нужна локальная PostgreSQL на `localhost:5432`
с отдельной базой `inventory_test_db` и пользователем `postgres`.

Если базы ещё нет, создайте её в PostgreSQL:

```sql
CREATE DATABASE inventory_test_db;
```

Используйте отдельную базу для тестов: интеграционные проверки изменяют данные.

В PowerShell задайте пароль локального пользователя `postgres`
через скрытый ввод:

```powershell
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Local PostgreSQL password' -AsSecureString)).Password
```

Запустите тесты:

```powershell
.\mvnw.cmd test
```

Или выполните сборку JAR с тестами:

```powershell
.\mvnw.cmd package
```

`DB_PASSWORD` действует в текущем терминале.
Файл `.env` используется Docker Compose и не загружается Maven автоматически.
`DOCKER_DB_PASSWORD` задаёт пароль отдельной базы в Docker.
## Пример работы с API

Команды выполняются в PowerShell при запущенном приложении.

### Создать товар

```powershell
$productBody = @{ name = 'Demo product'; quantity = 10; price = 125.50 } | ConvertTo-Json
$product = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/api/products' -ContentType 'application/json' -Body $productBody
$product
```

### Оформить заказ

Используем идентификатор созданного товара.
Цену приложение берёт из каталога.

```powershell
$orderBody = @{ items = @(@{ productId = $product.id; quantity = 2 }) } | ConvertTo-Json -Depth 5
$order = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/api/orders' -ContentType 'application/json' -Body $orderBody
$order | ConvertTo-Json -Depth 5
```

Ожидаемая сумма заказа — 251.00.
Остаток созданного товара уменьшится с 10 до 8.

### Прочитать заказ и список товаров

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/orders/$($order.id)" | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/products' | ConvertTo-Json -Depth 5
```

Повторный POST создаёт новый товар или заказ.
## Проверка зависимостей

30.10.2026 устранено подключение тестовой зависимости `json-smart`,
для которой IntelliJ сообщал о CVE-2026-113602.

- `json-smart` исключён из тестовых стартеров Spring Boot.
- Дерево Maven подтверждает отсутствие `json-smart` и `accessors-smart`.
- JsonPath обновлён до версии 3.0.0.
- Проверки JSON в тестах контроллера используют Jackson 3.
- После изменений все 20 тестов прошли без ошибок и пропусков.
- Команда `.\mvnw.cmd clean package` завершилась с `BUILD SUCCESS`.

После обновления Maven-проекта предупреждение IntelliJ исчезло.
Эта проверка подтверждает устранение указанной зависимости,
но не заменяет полный аудит безопасности проекта.