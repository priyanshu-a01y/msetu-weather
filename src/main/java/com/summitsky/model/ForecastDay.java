package com.summitsky.model;

import java.time.LocalDate;

public record ForecastDay(LocalDate date, double lowC, double highC, String description, String icon, int conditionCode) {
    public ForecastDay(LocalDate date, double lowC, double highC, String description, String icon) {
        this(date, lowC, highC, description, icon, 0);
    }
    public WeatherConditionIcon weatherConditionIcon() {
        return WeatherConditionIcon.from(conditionCode, description, icon, true);
    }
    public int rangePercent() { return (int)Math.max(1, Math.min(100, Math.round((highC-lowC)*4))); }
}
