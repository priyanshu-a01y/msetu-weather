package com.summitsky.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ForecastHour(Instant time, double temperatureC, String description, String icon,
        Integer precipitationProbability, int conditionCode) {
    public ForecastHour(Instant time, double temperatureC, String description, String icon,
            Integer precipitationProbability) {
        this(time, temperatureC, description, icon, precipitationProbability, 0);
    }
    public OffsetDateTime localTime(int timezoneOffset) {
        return time.atOffset(ZoneOffset.ofTotalSeconds(timezoneOffset));
    }
    public WeatherConditionIcon weatherConditionIcon(int timezoneOffset) {
        int hour = localTime(timezoneOffset).getHour();
        return WeatherConditionIcon.from(conditionCode, description, icon, hour >= 6 && hour < 18);
    }
}
