package com.summitsky.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="saved_locations", uniqueConstraints=@UniqueConstraint(columnNames="normalized_name"))
public class SavedLocation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String name;
    private String country;
    @Column(name="normalized_name",nullable=false) private String normalizedName;
    private Double latitude; private Double longitude;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected SavedLocation() {}
    public SavedLocation(String name,String country,Double latitude,Double longitude){this.name=name;this.country=country;this.latitude=latitude;this.longitude=longitude;this.normalizedName=name.trim().toLowerCase(java.util.Locale.ROOT);}
    public Long getId(){return id;} public String getName(){return name;} public String getCountry(){return country;} public String getNormalizedName(){return normalizedName;} public Double getLatitude(){return latitude;} public Double getLongitude(){return longitude;} public Instant getCreatedAt(){return createdAt;}
}
