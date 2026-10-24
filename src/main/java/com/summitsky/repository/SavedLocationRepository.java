package com.summitsky.repository;
import com.summitsky.entity.SavedLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SavedLocationRepository extends JpaRepository<SavedLocation,Long> { Optional<SavedLocation> findByNormalizedName(String name); }
