package com.summitsky.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public record WeatherData(String location, String state, String country, double latitude, double longitude,
        double temperatureC, double feelsLikeC, int humidity, double windSpeed, Integer windDirection,
        int pressure, Integer visibility, Integer cloudCoverage, String description, String icon,
        Instant observedAt, Long sunrise, Long sunset, int timezoneOffset, List<ForecastDay> forecast,
        List<ForecastHour> hourlyForecast, int conditionCode, Double precipitationMmPerHour) {
    public WeatherData(String location, String state, String country, double latitude, double longitude,
            double temperatureC, double feelsLikeC, int humidity, double windSpeed, Integer windDirection,
            int pressure, Integer visibility, Integer cloudCoverage, String description, String icon,
            Instant observedAt, Long sunrise, Long sunset, int timezoneOffset, List<ForecastDay> forecast,
            List<ForecastHour> hourlyForecast, int conditionCode) {
        this(location, state, country, latitude, longitude, temperatureC, feelsLikeC, humidity, windSpeed,
                windDirection, pressure, visibility, cloudCoverage, description, icon, observedAt, sunrise,
                sunset, timezoneOffset, forecast, hourlyForecast, conditionCode, null);
    }
    public WeatherData(String location, String state, String country, double latitude, double longitude,
            double temperatureC, double feelsLikeC, int humidity, double windSpeed, Integer windDirection,
            int pressure, Integer visibility, Integer cloudCoverage, String description, String icon,
            Instant observedAt, Long sunrise, Long sunset, int timezoneOffset, List<ForecastDay> forecast,
            List<ForecastHour> hourlyForecast) {
        this(location, state, country, latitude, longitude, temperatureC, feelsLikeC, humidity, windSpeed,
                windDirection, pressure, visibility, cloudCoverage, description, icon, observedAt, sunrise,
                sunset, timezoneOffset, forecast, hourlyForecast, conditionFromIcon(icon), null);
    }
    public WeatherData(String location, String state, String country, double latitude, double longitude,
            double temperatureC, double feelsLikeC, int humidity, double windSpeed, Integer windDirection,
            int pressure, Integer visibility, Integer cloudCoverage, String description, String icon,
            Instant observedAt, Long sunrise, Long sunset, int timezoneOffset, List<ForecastDay> forecast) {
        this(location, state, country, latitude, longitude, temperatureC, feelsLikeC, humidity, windSpeed,
                windDirection, pressure, visibility, cloudCoverage, description, icon, observedAt, sunrise,
                sunset, timezoneOffset, forecast, List.of());
    }

    public OffsetDateTime localObservedAt() { return observedAt.atOffset(ZoneOffset.ofTotalSeconds(timezoneOffset)); }
    public WeatherConditionIcon weatherConditionIcon() {
        return WeatherConditionIcon.from(conditionCode, description, icon, daylightAtObservation());
    }
    public String weatherScene() {
        int code = conditionCode > 0 ? conditionCode : conditionFromIcon(icon);
        if (code >= 200 && code <= 232) return "storm";
        if ((code >= 300 && code <= 321) || (code >= 500 && code <= 531)) return "rain";
        if (code >= 600 && code <= 622) return "snow";
        if (code >= 701 && code <= 781) return "mist";
        if (code == 800) return "clear";
        if (code == 801 || code == 802) return "partly-cloudy";
        if (code == 803 || code == 804) return "overcast";
        return "cloudy";
    }
    public boolean thunderstormHasRain() {
        return conditionCode == 200 || conditionCode == 201 || conditionCode == 202
                || conditionCode == 230 || conditionCode == 231 || conditionCode == 232
                || precipitationMmPerHour != null && precipitationMmPerHour > 0;
    }
    public String precipitationIntensity() {
        if (precipitationMmPerHour == null || precipitationMmPerHour < 2.5) return "light";
        return precipitationMmPerHour < 7.5 ? "moderate" : "heavy";
    }
    public String timezoneLabel() {
        int minutes = Math.abs(timezoneOffset) / 60;
        return "UTC" + (timezoneOffset < 0 ? "−" : "+") + "%02d:%02d".formatted(minutes / 60, minutes % 60);
    }
    public String countryDisplayName() {
        if (country == null || country.isBlank()) return "";
        return new Locale.Builder().setRegion(country).build().getDisplayCountry(Locale.ENGLISH);
    }
    public String windDirectionLabel() {
        if (windDirection == null) return null;
        String[] points={"N","NNE","NE","ENE","E","ESE","SE","SSE","S","SSW","SW","WSW","W","WNW","NW","NNW"};
        return points[(int)Math.round((windDirection%360)/22.5)%16];
    }
    public ForecastDay todayForecast() {
        var localDate = localObservedAt().toLocalDate();
        return forecast.stream().filter(day -> day.date().equals(localDate)).findFirst().orElse(null);
    }
    public String sunriseLabel() { return eventLabel(sunrise); }
    public String sunsetLabel() { return eventLabel(sunset); }
    /** SVG points derived only from the provider's upcoming forecast temperatures. */
    public String hourlyLinePoints() {
        if (hourlyForecast.isEmpty()) return "";
        double min = hourlyForecast.stream().mapToDouble(ForecastHour::temperatureC).min().orElse(0);
        double max = hourlyForecast.stream().mapToDouble(ForecastHour::temperatureC).max().orElse(0);
        double span = Math.max(1, max - min);
        int count = hourlyForecast.size();
        StringJoiner points = new StringJoiner(" ");
        for (int i = 0; i < count; i++) {
            double x = count == 1 ? 300 : 28 + 544.0 * i / (count - 1);
            double y = 76 - 48 * (hourlyForecast.get(i).temperatureC() - min) / span;
            points.add(String.format(Locale.ROOT, "%.1f,%.1f", x, y));
        }
        return points.toString();
    }
    public double hourlyPointX(int index) {
        return hourlyForecast.size() == 1 ? 300 : 28 + 544.0 * index / (hourlyForecast.size() - 1);
    }
    public double hourlyPointY(int index) {
        double min = hourlyForecast.stream().mapToDouble(ForecastHour::temperatureC).min().orElse(0);
        double max = hourlyForecast.stream().mapToDouble(ForecastHour::temperatureC).max().orElse(0);
        return 76 - 48 * (hourlyForecast.get(index).temperatureC() - min) / Math.max(1, max - min);
    }
    /** Smooth SVG path through provider supplied daily highs or lows. */
    public String dailyForecastPath(boolean high) {
        if (forecast.isEmpty()) return "";
        List<Double> values = forecast.stream().map(day -> high ? day.highC() : day.lowC()).toList();
        double min = forecast.stream().mapToDouble(ForecastDay::lowC).min().orElse(0);
        double max = forecast.stream().mapToDouble(ForecastDay::highC).max().orElse(1);
        double span = Math.max(1, max - min);
        List<double[]> points = new java.util.ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            double x = values.size() == 1 ? 500 : 46 + 908.0 * i / (values.size() - 1);
            double y = 130 - 100 * (values.get(i) - min) / span;
            points.add(new double[]{x, y});
        }
        StringBuilder path = new StringBuilder("M ").append(point(points.get(0)));
        for (int i = 0; i < points.size() - 1; i++) {
            double[] a = points.get(i), b = points.get(i + 1);
            double dx = (b[0] - a[0]) / 2.7;
            path.append(" C ").append(format(a[0] + dx)).append(' ').append(format(a[1]))
                    .append(' ').append(format(b[0] - dx)).append(' ').append(format(b[1]))
                    .append(' ').append(point(b));
        }
        return path.toString();
    }
    public double dailyForecastX(int index) {
        return forecast.size() == 1 ? 500 : 46 + 908.0 * index / (forecast.size() - 1);
    }
    public double dailyForecastY(int index, boolean high) {
        double min = forecast.stream().mapToDouble(ForecastDay::lowC).min().orElse(0);
        double max = forecast.stream().mapToDouble(ForecastDay::highC).max().orElse(1);
        double value = high ? forecast.get(index).highC() : forecast.get(index).lowC();
        return 130 - 100 * (value - min) / Math.max(1, max - min);
    }
    public String dailyForecastRangePath() {
        if (forecast.isEmpty()) return "";
        StringBuilder path = new StringBuilder(dailyForecastPath(true));
        for (int i = forecast.size() - 1; i >= 0; i--) {
            path.append(i == forecast.size() - 1 ? " L " : " L ")
                    .append(format(dailyForecastX(i))).append(' ').append(format(dailyForecastY(i, false)));
        }
        return path.append(" Z").toString();
    }
    public double windScalePercent() { return Math.min(100, Math.max(0, windSpeed / 20.0 * 100)); }
    public String windScaleLabel() {
        if (windSpeed < 0.5) return "Calm";
        if (windSpeed < 3.4) return "Light air";
        if (windSpeed < 8.0) return "Gentle breeze";
        if (windSpeed < 13.9) return "Moderate breeze";
        if (windSpeed < 20.8) return "Fresh breeze";
        return "Strong wind";
    }
    public double daylightProgressPercent() {
        if (sunrise == null || sunset == null || sunset <= sunrise) return 0;
        return Math.max(0, Math.min(100, (observedAt.getEpochSecond() - sunrise) * 100.0 / (sunset - sunrise)));
    }
    public double daylightSunX() {
        double angle = Math.PI - Math.PI * daylightProgressPercent() / 100.0;
        return 110 + 92 * Math.cos(angle);
    }
    public double daylightSunY() {
        double angle = Math.PI - Math.PI * daylightProgressPercent() / 100.0;
        return 78 - 68 * Math.sin(angle);
    }
    public boolean daylightAtObservation() {
        return sunrise != null && sunset != null && observedAt.getEpochSecond() >= sunrise && observedAt.getEpochSecond() <= sunset;
    }
    private static String point(double[] point) { return format(point[0]) + " " + format(point[1]); }
    private static String format(double value) { return String.format(Locale.ROOT, "%.1f", value); }
    private String eventLabel(Long epochSeconds) {
        if (epochSeconds == null) return null;
        return DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochSecond(epochSeconds).atOffset(ZoneOffset.ofTotalSeconds(timezoneOffset)));
    }
    private static int conditionFromIcon(String icon) {
        if (icon == null || icon.length() < 2) return 0;
        return switch (icon.substring(0, 2)) {
            case "01" -> 800;
            case "02" -> 801;
            case "03" -> 802;
            case "04" -> 804;
            case "09" -> 520;
            case "10" -> 500;
            case "11" -> 200;
            case "13" -> 600;
            case "50" -> 701;
            default -> 0;
        };
    }
}
