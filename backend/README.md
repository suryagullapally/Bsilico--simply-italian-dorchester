# Basilico Backend

Spring Boot foundation for the Basilico restaurant platform.

## Required Software

- Java 21
- Maven Wrapper, included as `./mvnw`
- Docker

You do not need a globally installed Maven if you use the wrapper.

## Local Environment

Configuration is read from environment variables. Copy the example file if you want local shell defaults:

```sh
cp .env.example .env
```

The local defaults are:

```txt
DB_HOST=localhost
DB_PORT=5432
DB_NAME=basilico_db
DB_USER=basilico
DB_PASSWORD=change-me
```

Do not commit a real `.env` file.

## Start PostgreSQL

```sh
docker compose up -d
```

This starts PostgreSQL on `localhost:5432` with a named Docker volume so local data survives container restarts.

To check the container:

```sh
docker compose ps
```

## Run Spring Boot

```sh
./mvnw spring-boot:run
```

The application runs on port `8080`.

Health URLs:

- http://localhost:8080/api/health
- http://localhost:8080/actuator/health

## Public Menu API

The public menu API returns active menu categories and active menu items.
Items that are active but temporarily unavailable are still returned with `available: false` so the frontend can show a sold-out state later.

- `GET /api/menu`
- `GET /api/menu/items/{slug}`
- `GET /api/menu/customizers/create-your-own`

Prices are stored and exposed in integer pennies:

```json
{
  "pricePence": 899
}
```

The Create Your Own customizer exposes:

```json
{
  "basePricePence": 700,
  "extraToppingPricePence": 125
}
```

Frontend applications should format pence into GBP for display.

## Development Admin Menu API

Important security warning: `/api/admin/**` is currently not authenticated. These endpoints are for local development only and must not be exposed publicly until Spring Security is implemented.

- `GET /api/admin/menu/items`
- `GET /api/admin/menu/items/{id}`
- `POST /api/admin/menu/items`
- `PUT /api/admin/menu/items/{id}`
- `PATCH /api/admin/menu/items/{id}/availability`
- `PATCH /api/admin/menu/items/{id}/active`
- `PATCH /api/admin/menu/items/{id}/featured`

Menu items are never hard-deleted through the API. Set `active` to `false` to archive an item and remove it from the public menu. Set `available` to `false` when a dish is temporarily sold out but should remain visible for future sold-out UI.

Slugs are editable on create only for this first admin API. Existing item slugs are intentionally immutable on `PUT` so public product URLs remain stable while order and admin history are still being designed.

## Stop PostgreSQL

```sh
docker compose down
```

To remove the local database volume as well:

```sh
docker compose down -v
```

## Tests and Build

```sh
./mvnw test
./mvnw clean package
```

## Project Architecture

- `com.basilico.backend` contains the Spring Boot application entry point.
- `config` contains shared application configuration such as local-development CORS.
- `health` contains the public API health check.
- `menu` contains the menu database model, repositories, DTOs, services and REST controllers.
- `src/main/resources/db/migration` contains Flyway database migrations.

Domain packages such as order, booking, payment and admin users will be added when those features are built.
