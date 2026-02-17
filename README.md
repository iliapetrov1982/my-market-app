# 🛒 My Market App (Reactive, WebFlux + R2DBC)

Spring Boot приложение — витрина интернет-магазина с корзиной и заказами.

Проект полностью переведён на **реактивный стек**:
- Spring WebFlux (контроллеры возвращают `Mono<String>`)
- Spring Data R2DBC (без JPA / JDBC)
- PostgreSQL 16
- Liquibase
- TransactionalOperator для реактивных транзакций
- Thymeleaf (server-side rendering)

---

# 🚀 Технологический стек

## Runtime
- Java 21
- Spring Boot 4.0.2
- Spring WebFlux
- Spring Data R2DBC
- Thymeleaf
- PostgreSQL 16
- Liquibase
- Gradle

## Тестирование
- JUnit 5
- Mockito
- Hamcrest
- Testcontainers (PostgreSQL)

---

# 🗄️ База данных

Liquibase автоматически применяет миграции при старте приложения.

### Основные таблицы:
- `items`
- `cart_items`
- `orders`
- `order_items`

### Миграции:
- `001-init-schema.yaml`
- `002-seed-items.yaml`

Seed-данные:
- применяются автоматически
- используются в интеграционных тестах
- не мокируются

---

# ▶️ Запуск приложения

## 1️⃣ Поднять PostgreSQL

```bash
docker compose up -d
```

## 2️⃣ Запустить приложение

```bash
./gradlew bootRun
```

После старта:

```
http://localhost:8080
```

Liquibase применится автоматически.

---

# 🧪 Запуск тестов

## Все тесты

```bash
./gradlew test
```

## Один конкретный тест

```bash
./gradlew test --tests "de.petrov.ya.java.mymarketapp.service.ItemsServiceTest"
```

## С логами

```bash
./gradlew test --info
```

---

# 🧪 Как устроены тесты

### Интеграционные
- Testcontainers (PostgreSQL)
- @ServiceConnection
- Liquibase применяется автоматически

### Сервисные
- Mockito
- проверка бизнес-логики
- проверка транзакций
- проверка реактивных цепочек (Mono / Flux)

### Контроллеры
- WebFlux
- POST формы через `exchange.getFormData()`

---

# 🌐 Основные страницы

- `/` или `/items` — витрина
- `/items/{id}` — карточка товара
- `/cart/items` — корзина
- `/orders` — список заказов
- `/orders/{id}` — детали заказа

---

# 🔎 Проверка работы через curl

## Витрина

```bash
curl -i "http://localhost:8080/items"
```

Поиск:

```bash
curl -i "http://localhost:8080/items?search=cap"
```

Сортировка:

```bash
curl -i "http://localhost:8080/items?sort=PRICE"
```

Пагинация:

```bash
curl -i "http://localhost:8080/items?pageNumber=2&pageSize=10"
```

---

## Добавить товар в корзину

```bash
curl -i -X POST "http://localhost:8080/items" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "id=2&action=PLUS&search=&sort=NO&pageNumber=1&pageSize=5"
```

Уменьшить:

```bash
curl -i -X POST "http://localhost:8080/items" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "id=2&action=MINUS&search=&sort=NO&pageNumber=1&pageSize=5"
```

---

## Карточка товара

```bash
curl -i "http://localhost:8080/items/2"
```

Из карточки:

```bash
curl -i -X POST "http://localhost:8080/items/2" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "action=PLUS"
```

---

## Корзина

```bash
curl -i "http://localhost:8080/cart/items"
```

Изменить корзину:

```bash
curl -i -X POST "http://localhost:8080/cart/items" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "id=2&action=PLUS"
```

Удалить:

```bash
curl -i -X POST "http://localhost:8080/cart/items" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "id=2&action=DELETE"
```

---

## Оформить заказ

```bash
curl -i -X POST "http://localhost:8080/buy"
```

---

## Список заказов

```bash
curl -i "http://localhost:8080/orders"
```

Детали заказа:

```bash
curl -i "http://localhost:8080/orders/1"
```

---

# 🔁 Архитектура

## Контроллеры
- WebFlux
- возвращают `Mono<String>`
- POST формы читаются через `exchange.getFormData()`

## Репозитории
- R2DBC
- SQL через `DatabaseClient`
- limit/offset для пагинации
- сортировка в SQL

## BuyService
- транзакция через `TransactionalOperator`
- создаётся `Order`
- сохраняются `OrderItem`
- очищается корзина
- всё реактивно

---

# 🐳 Docker

PostgreSQL:
- image: `postgres:16`
- database: `my-market-app`
- user: `my_market_user`
- password: `my_market_pass`

Приложение:
- Java 21
- порт 8080
- Liquibase применяется автоматически
