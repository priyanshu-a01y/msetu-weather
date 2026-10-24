# MSetu (Mausam Setu)

MSetu is a Java 21/Spring Boot weather platform for checking current conditions, exploring a five-day forecast, saving places, and reviewing successful observations. Weather comes from OpenWeather; the app does not substitute demo values when the service is unavailable. Observations and forecasts are informational, not mountain safety advisories.

## Features and architecture

- Spring MVC + Thymeleaf pages for search, weather, saved locations, history and project details.
- `/api` REST routes with request validation and structured errors.
- OpenWeather Geocoding, Current Weather, and 5-day/3-hour Forecast requests through Spring `RestClient`, with API key and timeouts controlled by configuration. Manali searches are disambiguated to Himachal Pradesh, India.
- Spring Data JPA persistence to MySQL for shared saved locations and successful observation history. This installation has no user login, so records are shared among visitors.
- Responsive mountain visual design with graceful CSS background fallback colors.

Packages: `controller` handles MVC/REST and errors; `service` contains weather and saved-location behavior; `repository` owns persistence access; `entity` contains JPA records; `model` holds the external weather response shape. Templates and static resources are under `src/main/resources`.

## Prerequisites

- Java 21 (verify with `java -version`)
- Maven 3.9+ (or use the included Maven wrapper if added by your IDE)
- MySQL 8 running locally
- An OpenWeather API key

## Windows PowerShell setup

Start MySQL using the service name installed on your machine, for example:

```powershell
Get-Service *mysql*
Start-Service MySQL80
```

Create the database (Hibernate creates/updates tables at startup):

```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS summitsky_db CHARACTER SET utf8mb4;"
```

Obtain an API key from [OpenWeather](https://openweathermap.org/api), then set variables in the same terminal where you run MSetu:

```powershell
$env:OPENWEATHER_API_KEY = "your_api_key"
$env:DB_URL = "jdbc:mysql://localhost:3306/summitsky_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password"
```

The app reads `OPENWEATHER_API_KEY` at startup through `weather.api.key=${OPENWEATHER_API_KEY:}`. It logs only whether the key is present (`true` or `false`), never its value. Stop any older SummitSky/MSetu process before restarting so requests use the latest code and current environment.

The default DB URL and username are local-development defaults; set `DB_PASSWORD` for your MySQL installation. Never commit secrets. The schema helper is `database/schema.sql`.

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

Run `mvn test`. Tests use an isolated H2 database and a mocked `WeatherService`, so they do not need a real API key or live weather service. Use MySQL for running the application.

## Troubleshooting

- Configuration banner on weather search: set `OPENWEATHER_API_KEY` in the current PowerShell session and restart.
- MySQL connection refused: start the service, verify `DB_URL`, user and password, and confirm the database exists.
- API 401: the server log reports the HTTP status and sanitized OpenWeather `message`. It distinguishes a key the provider calls invalid from one it reports as not activated. Verify the key/account state in OpenWeather, then set the key in the same terminal used to launch the app and restart SummitSky. Do not paste the key into logs or support messages.
- API 404: OpenWeather could not resolve the location; check spelling or try a country-qualified location.
- API 429: OpenWeather rate-limited the request; wait briefly before retrying.
- If the page still shows an old generic key error, stop the old process and run `mvn clean package` followed by `mvn spring-boot:run` from this project directory. The server log begins with `OpenWeather API key configured: true|false`.

Weather lookups resolve names through OpenWeather Geocoding, then request current and forecast data by latitude/longitude with `units=metric`. `dt` is retained as the provider observation timestamp and displayed using the location's UTC offset. Current-condition `temp_min` and `temp_max` are deliberately not used as daily forecast extremes; forecast ranges come from forecast-list entries only. MSetu does not cache these responses.

To verify a live lookup after startup, run `Invoke-RestMethod "http://localhost:8080/api/weather?city=Manali"` in another PowerShell window. A successful response contains the provider's current observation; this project does not supply demo weather data.

## Screenshots

Add genuine screenshots captured from a running local instance to a `screenshots/` folder and link them here. No screenshots are included because this project has not been launched against a configured database/API key in this workspace.
