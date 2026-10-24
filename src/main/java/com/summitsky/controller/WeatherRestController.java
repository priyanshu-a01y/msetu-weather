package com.summitsky.controller;
import com.summitsky.service.*; import com.summitsky.repository.*; import com.summitsky.entity.SavedLocation; import com.summitsky.model.WeatherData; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import org.springframework.web.bind.annotation.*; import org.springframework.http.HttpStatus; import java.util.*;
@RestController @RequestMapping("/api") public class WeatherRestController {
 private final WeatherService weather; private final SavedLocationService saved; private final SavedLocationRepository locations; private final WeatherHistoryRepository history;
 public WeatherRestController(WeatherService w,SavedLocationService s,SavedLocationRepository l,WeatherHistoryRepository h){weather=w;saved=s;locations=l;history=h;}
 @GetMapping("/weather") public WeatherData weather(@RequestParam @NotBlank @Size(max=80) String city){return weather.current(city);}
 public record SavedLocationDto(Long id,String name,String country,Double latitude,Double longitude,java.time.Instant createdAt) { static SavedLocationDto from(SavedLocation l){return new SavedLocationDto(l.getId(),l.getName(),l.getCountry(),l.getLatitude(),l.getLongitude(),l.getCreatedAt());} }
 public record HistoryDto(Long id,String location,String country,double temperatureC,double feelsLikeC,int humidity,double windSpeed,int pressure,String description,java.time.Instant observedAt) { static HistoryDto from(com.summitsky.entity.WeatherHistory h){return new HistoryDto(h.getId(),h.getLocation(),h.getCountry(),h.getTemperatureC(),h.getFeelsLikeC(),h.getHumidity(),h.getWindSpeed(),h.getPressure(),h.getDescription(),h.getObservedAt());} }
 @GetMapping("/locations") public List<SavedLocationDto> locations(){return saved.all().stream().map(SavedLocationDto::from).toList();}
 public record SaveRequest(@NotBlank @Size(max=80) String city){}
 @PostMapping("/locations") @ResponseStatus(HttpStatus.CREATED) public SavedLocationDto save(@Valid @RequestBody SaveRequest r){return SavedLocationDto.from(saved.save(weather.current(r.city())));}
 @DeleteMapping("/locations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable @Positive Long id){saved.delete(id);}
 @GetMapping("/history") public List<HistoryDto> history(@RequestParam(required=false) String city){return (city==null||city.isBlank()?history.findTop100ByOrderByObservedAtDesc():history.findTop100ByLocationContainingIgnoreCaseOrderByObservedAtDesc(city)).stream().map(HistoryDto::from).toList();}
}
