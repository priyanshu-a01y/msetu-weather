
package com.summitsky.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.summitsky.entity.WeatherHistory;
import com.summitsky.model.ForecastDay;
import com.summitsky.model.ForecastHour;
import com.summitsky.model.WeatherData;
import com.summitsky.repository.WeatherHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class WeatherService {

    private static final Logger log =
            LoggerFactory.getLogger(WeatherService.class);

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private final RestClient client;
    private final String key;
    private final String base;
    private final String geoBase;
    private final WeatherHistoryRepository history;

    @Autowired
    public WeatherService(
            RestClient.Builder builder,
            @Value("${weather.api.key:}") String key,
            @Value("${weather.api.base-url}") String base,
            @Value("${weather.api.geocoding-url}") String geoBase,
            WeatherHistoryRepository history) {

        var factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(java.time.Duration.ofSeconds(5));
        factory.setReadTimeout(java.time.Duration.ofSeconds(8));

        this.client = builder.requestFactory(factory).build();
        this.key = key == null ? "" : key.trim();
        this.base = base;
        this.geoBase = geoBase;
        this.history = history;

        log.info("OpenWeather API key configured: {}", !this.key.isBlank());
    }

    public WeatherService(
            RestClient client,
            String key,
            String base,
            String geoBase,
            WeatherHistoryRepository history) {

        this.client = client;
        this.key = key == null ? "" : key.trim();
        this.base = base;
        this.geoBase = geoBase;
        this.history = history;
    }

    public WeatherData current(String query) {

        if (query == null || query.isBlank() || query.length() > 80) {
            throw new IllegalArgumentException(
                    "Enter a location name up to 80 characters.");
        }

        if (key.isBlank()) {
            throw new WeatherException(
                    "Weather service is not configured. Set OPENWEATHER_API_KEY and restart MSetu.",
                    503);
        }

        Location location = resolve(query.trim());

        // FIX: Correct OpenWeather current-weather endpoint.
        java.net.URI url = uri(base + "/data/2.5/weather")
                .queryParam("lat", location.lat)
                .queryParam("lon", location.lon)
                .queryParam("appid", key)
                .queryParam("units", "metric")
                .build()
                .encode()
                .toUri();

        try {
            JsonNode n = client.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (req, res) -> providerError(res, "current weather"))
                    .body(JsonNode.class);

            if (n == null) {
                throw new WeatherException(
                        "Weather provider returned an empty current-conditions response.",
                        502);
            }

            JsonNode main = n.path("main");
            JsonNode weather = n.path("weather").path(0);
            JsonNode wind = n.path("wind");

            if (!main.path("temp").isNumber()
                    || !main.path("feels_like").isNumber()
                    || !main.path("humidity").isNumber()
                    || !main.path("pressure").isNumber()
                    || !wind.path("speed").isNumber()
                    || !weather.path("description").isTextual()
                    || !n.path("dt").isNumber()
                    || !n.path("timezone").isNumber()) {

                throw new WeatherException(
                        "Weather provider returned incomplete current conditions. Please retry.",
                        502);
            }

            Instant observed = Instant.ofEpochSecond(n.path("dt").asLong());

            ForecastBundle forecast = forecast(location, observed);

            WeatherData data = new WeatherData(
                    location.name,
                    location.state,
                    location.country,
                    location.lat,
                    location.lon,
                    main.path("temp").asDouble(),
                    main.path("feels_like").asDouble(),
                    main.path("humidity").asInt(),
                    wind.path("speed").asDouble(),
                    wind.path("deg").isNumber()
                            ? wind.path("deg").asInt()
                            : null,
                    main.path("pressure").asInt(),
                    n.path("visibility").isNumber()
                            ? n.path("visibility").asInt()
                            : null,
                    n.path("clouds").path("all").isNumber()
                            ? n.path("clouds").path("all").asInt()
                            : null,
                    weather.path("description").asText(),
                    weather.path("icon").asText(""),
                    observed,
                    n.path("sys").path("sunrise").isNumber()
                            ? n.path("sys").path("sunrise").asLong()
                            : null,
                    n.path("sys").path("sunset").isNumber()
                            ? n.path("sys").path("sunset").asLong()
                            : null,
                    n.path("timezone").asInt(),
                    forecast.days(),
                    forecast.hours(),
                    weather.path("id").asInt(0),
                    precipitationMmPerHour(n.path("rain"))
            );

            log.info(
                    "Weather resolved: location={}, state={}, country={}, lat={}, lon={}, tempC={}, feelsLikeC={}, condition={}, observedAtUtc={}",
                    location.name,
                    location.state,
                    location.country,
                    location.lat,
                    location.lon,
                    main.path("temp").asDouble(),
                    main.path("feels_like").asDouble(),
                    weather.path("description").asText(),
                    observed
            );

            history.save(new WeatherHistory(
                    data.location(),
                    data.country(),
                    data.temperatureC(),
                    data.feelsLikeC(),
                    data.humidity(),
                    data.windSpeed(),
                    data.pressure(),
                    data.description(),
                    data.observedAt()
            ));

            return data;

        } catch (WeatherException | IllegalArgumentException e) {
            throw e;

        } catch (ResourceAccessException e) {
            log.warn(
                    "OpenWeather current-weather request could not connect: {}",
                    e.getClass().getSimpleName());

            throw new WeatherException(
                    "Couldn't reach the weather service. Check your connection and retry.",
                    503);

        } catch (RestClientException e) {
            log.warn(
                    "OpenWeather current-weather response could not be parsed: {}",
                    e.getClass().getSimpleName());

            throw new WeatherException(
                    "Weather service returned an unreadable response. Please retry.",
                    502);
        }
    }

    private Location resolve(String query) {

        boolean manali = query.equalsIgnoreCase("Manali")
                || query.toLowerCase(Locale.ROOT).startsWith("manali,");

        String search = manali
                ? "Manali,Himachal Pradesh,IN"
                : query;

        java.net.URI url = uri(geoBase + "/direct")
                .queryParam("q", search)
                .queryParam("limit", 5)
                .queryParam("appid", key)
                .build()
                .encode()
                .toUri();

        try {
            JsonNode results = client.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (req, res) -> providerError(res, "location search"))
                    .body(JsonNode.class);

            if (results == null || !results.isArray() || results.isEmpty()) {
                throw new WeatherException(
                        "We couldn't find that location. Check the name and try again.",
                        404);
            }

            JsonNode selected = null;

            for (JsonNode candidate : results) {

                if (!candidate.path("lat").isNumber()
                        || !candidate.path("lon").isNumber()) {
                    continue;
                }

                if (!manali
                        || (candidate.path("country").asText()
                                .equalsIgnoreCase("IN")
                        && candidate.path("state").asText()
                                .toLowerCase(Locale.ROOT)
                                .contains("himachal"))) {

                    selected = candidate;
                    break;
                }
            }

            if (selected == null) {
                throw new WeatherException(
                        "The location search did not return Manali in Himachal Pradesh, India. Please retry or use another search.",
                        502);
            }

            Location result = new Location(
                    selected.path("name").asText(query),
                    selected.path("state").asText(""),
                    selected.path("country").asText(""),
                    selected.path("lat").asDouble(),
                    selected.path("lon").asDouble()
            );

            log.info(
                    "Location resolved: name={}, state={}, country={}, lat={}, lon={}",
                    result.name,
                    result.state,
                    result.country,
                    result.lat,
                    result.lon
            );

            return result;

        } catch (WeatherException e) {
            throw e;

        } catch (ResourceAccessException e) {
            log.warn(
                    "OpenWeather geocoding request could not connect: {}",
                    e.getClass().getSimpleName());

            throw new WeatherException(
                    "Couldn't reach location search. Check your connection and retry.",
                    503);

        } catch (RestClientException e) {
            log.warn(
                    "OpenWeather geocoding response could not be parsed: {}",
                    e.getClass().getSimpleName());

            throw new WeatherException(
                    "Location search returned an unreadable response. Please retry.",
                    502);
        }
    }

    private ForecastBundle forecast(Location location, Instant observedAt) {

        // FIX: Correct OpenWeather 5-day forecast endpoint.
        java.net.URI url = uri(base + "/data/2.5/forecast")
                .queryParam("lat", location.lat)
                .queryParam("lon", location.lon)
                .queryParam("appid", key)
                .queryParam("units", "metric")
                .build()
                .encode()
                .toUri();

        try {
            JsonNode response = client.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (req, res) -> providerError(res, "forecast"))
                    .body(JsonNode.class);

            if (response == null || !response.path("list").isArray()) {
                return ForecastBundle.EMPTY;
            }

            int offset = response.path("city").path("timezone").asInt(0);

            Map<LocalDate, Daily> days = new TreeMap<>();
            List<ForecastHour> hours = new ArrayList<>();

            for (JsonNode item : response.path("list")) {

                JsonNode main = item.path("main");
                JsonNode condition = item.path("weather").path(0);

                if (!item.path("dt").isNumber()) {
                    continue;
                }

                Instant time = Instant.ofEpochSecond(item.path("dt").asLong());

                if (main.path("temp").isNumber()
                        && time.isAfter(observedAt)
                        && hours.size() < 6) {

                    Integer pop = item.path("pop").isNumber()
                            ? (int) Math.round(item.path("pop").asDouble() * 100)
                            : null;

                    hours.add(new ForecastHour(
                            time,
                            main.path("temp").asDouble(),
                            condition.path("description")
                                    .asText("Forecast condition unavailable"),
                            condition.path("icon").asText(""),
                            pop,
                            condition.path("id").asInt(0)
                    ));
                }

                if (!main.path("temp_min").isNumber()
                        || !main.path("temp_max").isNumber()) {
                    continue;
                }

                LocalDate date = time
                        .atOffset(java.time.ZoneOffset.ofTotalSeconds(offset))
                        .toLocalDate();

                Daily daily = days.computeIfAbsent(date, d -> new Daily());

                daily.low = Math.min(
                        daily.low,
                        main.path("temp_min").asDouble());

                daily.high = Math.max(
                        daily.high,
                        main.path("temp_max").asDouble());

                if (daily.description == null) {
                    daily.description = condition.path("description")
                            .asText("Forecast condition unavailable");

                    daily.icon = condition.path("icon").asText("");
                    daily.conditionCode = condition.path("id").asInt(0);
                }
            }

            List<ForecastDay> daily = days.entrySet()
                    .stream()
                    .map(e -> new ForecastDay(
                            e.getKey(),
                            e.getValue().low,
                            e.getValue().high,
                            e.getValue().description,
                            e.getValue().icon,
                            e.getValue().conditionCode))
                    .toList();

            return new ForecastBundle(daily, List.copyOf(hours));

        } catch (Exception e) {
            log.warn(
                    "OpenWeather forecast unavailable: {}",
                    e instanceof WeatherException we
                            ? we.getMessage()
                            : e.getClass().getSimpleName());

            return ForecastBundle.EMPTY;
        }
    }

    private void providerError(
            ClientHttpResponse response,
            String operation) throws IOException {

        int status = response.getStatusCode().value();
        String message = providerMessage(response);

        log.warn(
                "OpenWeather {} failed: status={}, providerMessage={}",
                operation,
                status,
                message.isBlank() ? "(empty)" : message
        );

        if (status == 401) {

            String lower = message.toLowerCase(Locale.ROOT);

            if (lower.contains("not activated")
                    || lower.contains("not active")
                    || lower.contains("activation")) {

                throw new WeatherException(
                        "OpenWeather says this API key is not active yet. Check its status in your OpenWeather account.",
                        502);
            }

            if (lower.contains("invalid api key")
                    || lower.contains("invalid appid")
                    || lower.contains("invalid key")) {

                throw new WeatherException(
                        "OpenWeather says the API key is invalid. Verify OPENWEATHER_API_KEY and restart MSetu.",
                        502);
            }

            throw new WeatherException(
                    "OpenWeather rejected the request with HTTP 401"
                            + (message.isBlank() ? "." : ": " + message),
                    502);
        }

        if (status == 404) {
            throw new WeatherException(
                    "OpenWeather endpoint or resource returned HTTP 404."
                            + (message.isBlank() ? "" : " Provider: " + message),
                    404);
        }

        if (status == 429) {
            throw new WeatherException(
                    "OpenWeather rate-limited the request (HTTP 429). Wait briefly and retry."
                            + (message.isBlank() ? "" : " Provider: " + message),
                    429);
        }

        throw new WeatherException(
                "OpenWeather returned HTTP " + status
                        + (message.isBlank() ? "." : ": " + message),
                502);
    }

    private String providerMessage(ClientHttpResponse response) throws IOException {

        byte[] bytes = response.getBody().readAllBytes();

        if (bytes.length == 0) {
            return "";
        }

        String raw = new String(bytes, StandardCharsets.UTF_8);
        String message;

        try {
            JsonNode node = JSON.readTree(raw);

            message = node != null && node.hasNonNull("message")
                    ? node.path("message").asText()
                    : raw;

        } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
            message = raw;
        }

        message = message
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim();

        if (!key.isBlank()) {
            message = message.replace(key, "[redacted]");
        }

        message = message.replaceAll(
                "(?i)(appid=)[^&\\s]+",
                "$1[redacted]");

        return message.length() > 200
                ? message.substring(0, 200) + "…"
                : message;
    }

    private UriComponentsBuilder uri(String url) {
        return UriComponentsBuilder.fromUriString(url);
    }

    private Double precipitationMmPerHour(JsonNode rain) {

        if (rain.path("1h").isNumber()) {
            return Math.max(0, rain.path("1h").asDouble());
        }

        if (rain.path("3h").isNumber()) {
            return Math.max(0, rain.path("3h").asDouble() / 3.0);
        }

        return null;
    }

    private record ForecastBundle(
            List<ForecastDay> days,
            List<ForecastHour> hours) {

        static final ForecastBundle EMPTY =
                new ForecastBundle(List.of(), List.of());
    }

    private static final class Location {

        final String name;
        final String state;
        final String country;
        final double lat;
        final double lon;

        Location(
                String name,
                String state,
                String country,
                double lat,
                double lon) {

            this.name = name;
            this.state = state;
            this.country = country;
            this.lat = lat;
            this.lon = lon;
        }
    }

    private static final class Daily {

        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        String description;
        String icon;
        int conditionCode;
    }
}

