package com.summitsky.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="weather_history", indexes=@Index(name="idx_weather_observed",columnList="observed_at"))
public class WeatherHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String location; private String country;
    private double temperatureC; private double feelsLikeC; private int humidity; private double windSpeed; private int pressure;
    private String description;
    @Column(name="observed_at",nullable=false) private Instant observedAt;
    protected WeatherHistory() {}
    public WeatherHistory(String location,String country,double temperatureC,double feelsLikeC,int humidity,double windSpeed,int pressure,String description,Instant observedAt){this.location=location;this.country=country;this.temperatureC=temperatureC;this.feelsLikeC=feelsLikeC;this.humidity=humidity;this.windSpeed=windSpeed;this.pressure=pressure;this.description=description;this.observedAt=observedAt;}
    public Long getId(){return id;} public String getLocation(){return location;} public String getCountry(){return country;} public double getTemperatureC(){return temperatureC;} public double getFeelsLikeC(){return feelsLikeC;} public int getHumidity(){return humidity;} public double getWindSpeed(){return windSpeed;} public int getPressure(){return pressure;} public String getDescription(){return description;} public Instant getObservedAt(){return observedAt;}
}
