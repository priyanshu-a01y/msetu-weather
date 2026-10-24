package com.summitsky.repository;
import com.summitsky.entity.WeatherHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface WeatherHistoryRepository extends JpaRepository<WeatherHistory,Long> { List<WeatherHistory> findTop100ByOrderByObservedAtDesc(); List<WeatherHistory> findTop100ByLocationContainingIgnoreCaseOrderByObservedAtDesc(String city); }
