# MSetu (Mausam Setu)

MSetu is a Java 21/Spring Boot weather platform for checking current conditions, exploring a five-day forecast, saving places, and reviewing successful observations. Weather comes from OpenWeather; the app does not substitute demo values when the service is unavailable. Observations and forecasts are informational, not mountain safety advisories.

## Features and architecture

- Spring MVC + Thymeleaf pages for search, weather, saved locations, history and project details.
- `/api` REST routes with request validation and structured errors.
- OpenWeather Geocoding, Current Weather, and 5-day/3-hour Forecast requests through Spring `RestClient`, with API key and timeouts controlled by configuration. Manali searches are disambiguated to Himachal Pradesh, India.
- Spring Data JPA persistence to PostgreSQL (including Supabase) for shared saved locations and successful observation history. This installation has no user login, so records are shared among visitors.
- Responsive mountain visual design with graceful CSS background fallback colors.

Packages: `controller` handles MVC/REST and errors; `service` contains weather and saved-location behavior; `repository` owns persistence access; `entity` contains JPA records; `model` holds the external weather response shape. Templates and static resources are under `src/main/resources`.

## Prerequisites

- Java 21 (verify with `java -version`)
- Maven 3.9+ (or use the included Maven wrapper if added by your IDE)
- PostgreSQL locally, or a Supabase project
- An OpenWeather API key

## PostgreSQL and Supabase setup

MSetu reads `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` from the process environment. The JDBC URL must not contain credentials. Supabase connections require TLS; `DB_SSLMODE` defaults to `require`. The default database URL and username are for local development only.

For Supabase, open your project dashboard and choose **Connect**, then select **Session pooler**. Copy its host, port, database, and username. Use the Session Pooler for hosts with IPv4-only connectivity; direct database connections may require IPv6 or a separately enabled IPv4 add-on. Set `DB_URL` in JDBC form using the displayed values:

```text
jdbc:postgresql://<SESSION_POOLER_HOST>:<SESSION_POOLER_PORT>/<DATABASE>?sslmode=require
```

Copy the Session Pooler username exactly (it may include the project reference) and use your Supabase database password for `DB_PASSWORD`. The database is usually named `postgres` unless you selected another database. The app passes the SSL mode to PostgreSQL JDBC separately as well, defaulting to `require`.

Obtain an API key from [OpenWeather](https://openweathermap.org/api). In Windows PowerShell, set the variables in the same terminal that will run MSetu:

```powershell
$env:OPENWEATHER_API_KEY = "your_openweather_api_key"
$env:DB_URL = "jdbc:postgresql://<SESSION_POOLER_HOST>:<SESSION_POOLER_PORT>/<DATABASE>?sslmode=require"
$env:DB_USERNAME = "<SESSION_POOLER_USERNAME>"
$env:DB_PASSWORD = "<SUPABASE_DATABASE_PASSWORD>"
$env:DB_SSLMODE = "require"
```

For local PostgreSQL, omit `DB_URL` and `DB_USERNAME` to use the localhost defaults; set `DB_PASSWORD` as required by your local server. If local PostgreSQL has no TLS enabled, set `$env:DB_SSLMODE = "disable"` for local development only. Do not use `disable` for Supabase.

Hibernate creates or updates the entity tables on startup. `database/schema.sql` provides the equivalent PostgreSQL schema for explicit initialization; it does not create a database or drop existing data. Existing MySQL records must be backed up and migrated to PostgreSQL separately before switching the app to the new database.

**Never commit real database credentials or API keys.** Keep them in environment variables or a secret manager. Do not include them in logs, screenshots, or support messages. MSetu logs only whether the weather API key is configured, never its value. Stop any older MSetu process before restarting so it reads the current environment.

## Run and build

From the project directory:

```powershell
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). Build an executable JAR with `mvn clean package`, then run `java -jar target/summitsky-1.0.0.jar`.

## REST examples

```powershell
Invoke-RestMethod "http://localhost:8080/api/weather?city=Manali"
Invoke-RestMethod "http://localhost:8080/api/locations"
Invoke-RestMethod -Method Post -ContentType "application/json" -Body '{"city":"Leh"}' "http://localhost:8080/api/locations"
Invoke-RestMethod -Method Delete "http://localhost:8080/api/locations/1"
Invoke-RestMethod "http://localhost:8080/api/history?city=Manali"
```

## Tests

Run `mvn test`. Tests use an isolated H2 database and mocked weather HTTP responses, so they do not require a live Supabase database or OpenWeather API key. Run `mvn clean package` to test and build the executable application.

## Troubleshooting

- Weather API key not configured: set `OPENWEATHER_API_KEY` in the current terminal and restart.
- PostgreSQL connection failure: check the Session Pooler host, port, database and username under Supabase **Connect → Session pooler**, and confirm `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` are set in the launch terminal. Keep `DB_SSLMODE=require` for Supabase.
- API 401: the server reports the HTTP status and sanitized provider message. Verify the key's status in OpenWeather, set it in the launch terminal, and restart. Never paste the key into logs or support messages.
- API 404: OpenWeather could not resolve the location; check spelling or try a country-qualified location.
- API 429: OpenWeather rate-limited the request; wait briefly before retrying.

Weather lookups resolve names through OpenWeather Geocoding, then request current and forecast data by latitude/longitude with `units=metric`. `dt` is retained as the provider observation timestamp and displayed using the location's UTC offset. Current-condition `temp_min` and `temp_max` are deliberately not used as daily forecast extremes; forecast ranges come from forecast-list entries only. MSetu does not cache these responses.

To verify a live lookup after startup, run `Invoke-RestMethod "http://localhost:8080/api/weather?city=Manali"` in another PowerShell window. A successful response contains the provider's current observation; this project does not supply demo weather data.

## Screenshots

Add genuine screenshots captured from a running local instance to a `screenshots/` folder and link them here. No screenshots are included because this project has not been launched against a configured database/API key in this workspace.
