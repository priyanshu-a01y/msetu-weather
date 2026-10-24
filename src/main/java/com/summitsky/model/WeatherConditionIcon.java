package com.summitsky.model;

import java.util.Locale;

/** Accessible local SVG symbol selected from OpenWeather's condition ID and description. */
public record WeatherConditionIcon(String symbol, String label) {
    public static WeatherConditionIcon from(int conditionCode, String description, String providerIcon,
            boolean daylight) {
        int code = conditionCode > 0 ? conditionCode : codeFromIcon(providerIcon);
        String name = normalized(description);
        boolean isDay = providerIcon != null && providerIcon.endsWith("d") ? true
                : providerIcon != null && providerIcon.endsWith("n") ? false : daylight;

        String symbol = symbolFor(code, name, isDay);
        String label = description == null || description.isBlank() ? defaultLabel(code, name) : sentenceCase(description);
        return new WeatherConditionIcon(symbol, label);
    }

    private static String symbolFor(int code, String description, boolean daylight) {
        if (code >= 200 && code <= 232 || description.contains("thunderstorm")) return "thunderstorm";
        if (code >= 300 && code <= 321 || description.contains("drizzle")) return "drizzle";
        if (code >= 500 && code <= 531 || description.contains("rain")) return "rain";
        if (code >= 600 && code <= 622 || description.contains("snow") || description.contains("sleet")) return "snow";
        if (code == 800 || description.contains("clear sky") || description.equals("clear")) return daylight ? "clear-day" : "clear-night";
        if (code == 801 || description.contains("few clouds")) return daylight ? "few-clouds-day" : "few-clouds-night";
        if (code == 802 || description.contains("scattered clouds")) return daylight ? "scattered-clouds-day" : "scattered-clouds-night";
        if (code == 803 || description.contains("broken clouds")) return "broken-clouds";
        if (code == 804 || description.contains("overcast")) return "overcast";
        if (code == 701 || code == 741 || description.contains("mist") || description.contains("fog")) return "mist";
        if (code == 711 || description.contains("smoke")) return "smoke";
        if (code == 721 || description.contains("haze") || code == 762 || description.contains("ash")) return "haze";
        if (code == 731 || code == 761 || description.contains("dust")) return "dust";
        if (code == 751 || description.contains("sand")) return "dust";
        if (code == 771 || description.contains("squall")) return "wind";
        if (code == 781 || description.contains("tornado")) return "tornado";
        if (description.contains("cloud")) return "neutral-cloud";
        return "neutral-cloud";
    }

    private static int codeFromIcon(String icon) {
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

    private static String normalized(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private static String sentenceCase(String value) {
        String clean = value.trim();
        return clean.isEmpty() ? "Weather condition" : clean.substring(0, 1).toUpperCase(Locale.ROOT) + clean.substring(1);
    }

    private static String defaultLabel(int code, String description) {
        if (!description.isBlank()) return sentenceCase(description);
        if (code >= 200 && code <= 232) return "Thunderstorm";
        if (code >= 300 && code <= 321) return "Drizzle";
        if (code >= 500 && code <= 531) return "Rain";
        if (code >= 600 && code <= 622) return "Snow";
        if (code == 800) return "Clear sky";
        if (code == 801) return "Few clouds";
        if (code == 802) return "Scattered clouds";
        if (code == 803) return "Broken clouds";
        if (code == 804) return "Overcast clouds";
        if (code == 701) return "Mist";
        if (code == 711) return "Smoke";
        if (code == 721) return "Haze";
        if (code == 731 || code == 761) return "Dust";
        if (code == 741) return "Fog";
        if (code == 751) return "Sand";
        if (code == 762) return "Volcanic ash";
        if (code == 771) return "Squalls";
        if (code == 781) return "Tornado";
        return "Cloudy conditions";
    }
}
