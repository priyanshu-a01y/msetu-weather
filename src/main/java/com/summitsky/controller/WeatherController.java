package com.summitsky.controller;
import com.summitsky.model.LocationVisualCatalog;
import com.summitsky.service.SavedLocationService;
import com.summitsky.service.WeatherService;
import com.summitsky.repository.WeatherHistoryRepository;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Controller
@Validated
public class WeatherController {
    private final WeatherService weather;
    private final SavedLocationService saved;
    private final WeatherHistoryRepository history;

    public WeatherController(WeatherService weather, SavedLocationService saved, WeatherHistoryRepository history) {
        this.weather = weather;
        this.saved = saved;
        this.history = history;
    }

    @GetMapping("/")
    public String home(@RequestParam(required = false) @Size(min = 2, max = 80) String city, Model model) {
        model.addAttribute("destinations", LocationVisualCatalog.featured());
        model.addAttribute("savedLocations", saved.all());
        if (city != null && !city.isBlank()) {
            var data = weather.current(city);
            model.addAttribute("weather", data);
            model.addAttribute("visual", LocationVisualCatalog.forLocation(data.location(), city));
            model.addAttribute("selectedCity", data.location());
        }
        return "index";
    }

    @GetMapping("/weather")
    public String dashboard(@RequestParam @Size(min = 2, max = 80) String city, Model model) {
        var data = weather.current(city);
        model.addAttribute("weather", data);
        model.addAttribute("visual", LocationVisualCatalog.forLocation(data.location(), city));
        var match = saved.all().stream().filter(s -> s.getNormalizedName()
                .equals(data.location().trim().toLowerCase(java.util.Locale.ROOT))).findFirst();
        model.addAttribute("savedLocation", match.orElse(null));
        model.addAttribute("savedLocations", saved.all());
        return "dashboard";
    }

    @PostMapping("/locations/save")
    public String save(@RequestParam String city) { saved.save(weather.current(city)); return "redirect:/locations"; }

    @GetMapping("/locations")
    public String locations(Model model) { model.addAttribute("locations", saved.all()); return "saved-locations"; }

    @PostMapping("/locations/{id}/delete")
    public String delete(@PathVariable Long id) { saved.delete(id); return "redirect:/locations"; }

    @GetMapping("/history")
    public String history(@RequestParam(required = false) String city, Model model) {
        model.addAttribute("records", city == null || city.isBlank()
                ? history.findTop100ByOrderByObservedAtDesc()
                : history.findTop100ByLocationContainingIgnoreCaseOrderByObservedAtDesc(city));
        model.addAttribute("city", city);
        return "weather-history";
    }

    @GetMapping("/about")
    public String about() { return "about"; }
}
