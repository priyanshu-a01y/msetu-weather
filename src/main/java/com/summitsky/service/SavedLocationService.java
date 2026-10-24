package com.summitsky.service;
import com.summitsky.entity.SavedLocation;
import com.summitsky.model.WeatherData;
import com.summitsky.repository.SavedLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service public class SavedLocationService {
 private final SavedLocationRepository repo; public SavedLocationService(SavedLocationRepository repo){this.repo=repo;}
 public List<SavedLocation> all(){return repo.findAll();}
 @Transactional public SavedLocation save(WeatherData w){return repo.findByNormalizedName(w.location().trim().toLowerCase(java.util.Locale.ROOT)).orElseGet(()->repo.save(new SavedLocation(w.location(),w.country(),w.latitude(),w.longitude())));}
 public void delete(Long id){if(!repo.existsById(id))throw new IllegalArgumentException("Saved location not found.");repo.deleteById(id);}
}
