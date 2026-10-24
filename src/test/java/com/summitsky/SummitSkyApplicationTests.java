package com.summitsky;

import com.summitsky.model.ForecastDay;
import com.summitsky.model.ForecastHour;
import com.summitsky.model.LocationVisualCatalog;
import com.summitsky.model.WeatherData;
import com.summitsky.repository.SavedLocationRepository;
import com.summitsky.service.SavedLocationService;
import com.summitsky.service.WeatherException;
import com.summitsky.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class SummitSkyApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired SavedLocationService saved;
    @Autowired SavedLocationRepository locations;
    @MockBean WeatherService weather;

    private WeatherData sample(String city) {
        String state = city.equals("Manali") ? "Himachal Pradesh" : "Uttar Pradesh";
        return new WeatherData(city,state,"IN",32.2396,77.1887,9,8,71,2.4,250,812,9000,65,
                "overcast clouds","04d",Instant.parse("2026-10-24T07:00:00Z"),1792820000L,1792860000L,19800,
                List.of(new ForecastDay(LocalDate.parse("2026-10-24"),6,12,"scattered clouds","03d")),
                List.of(new ForecastHour(Instant.parse("2026-10-24T08:00:00Z"),11,"scattered clouds","03d",35),
                        new ForecastHour(Instant.parse("2026-10-24T11:00:00Z"),13,"clear sky","01d",null)));
    }

    @Test void homepageBuildsTheNewMSetuExperienceWithoutInventedWeather() throws Exception {
        String html=mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("The sky,").contains("atmospheric-sky").contains("atmospheric-clouds.png")
                .contains("id=\"destinations\"").contains("Weather has a sense of place.")
                .contains("favicon.svg?v=ms-monogram-20261024").contains("brand-monogram")
                .contains("Mausam Setu").contains("MSetu").doesNotContain("brand-mark")
                .doesNotContain("°C").doesNotContain("SummitSky").doesNotContain("destination-condition")
                .doesNotContain("10.5 20h14a5 5 0 0 0 .3-10");
        assertThat(html.split("class=\"brand-monogram\"", -1)).hasSize(3);
    }

    @Test void destinationScenesAreExplicitAndDoNotUseIndexBasedImages() throws Exception {
        String html=mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for(String city:List.of("Manali","Shimla","Leh","Srinagar","Gangtok","Mussoorie","Prayagraj")) assertThat(html).contains(city).contains("city="+city);
        assertThat(html.split("class=\"destination-card\"", -1)).hasSize(8);
        assertThat(html).contains("Manali_Mountains.jpg").contains("Deodar_forests_of_Shimla_in_monsoon.jpg")
                .contains("Ladakh_landscape_near_Leh.jpg").contains("Dal_Lake_Kashmir")
                .contains("Gangtok_View_Point").contains("Beautiful_Mussoorie.jpg");
        assertThat(html).contains("Arail-ghatt.jpg").contains("Delhi").contains("Mumbai");
    }

    @Test void selectedHomepageShowsOnlyProviderBackedConditionsAndForecasts() throws Exception {
        when(weather.current("Manali")).thenReturn(sample("Manali"));
        String html=mvc.perform(get("/").param("city","Manali")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("CURRENT CONDITIONS").contains("9").contains("Feels like")
                .contains("35% rain chance").contains("THE DAYS AHEAD").contains("Manali_Mountains.jpg")
                .contains("wind-inline").contains("daylight-inline").contains("forecast-high-line")
                .contains("UPCOMING HOURS").contains("weather-icons.svg#overcast")
                .contains("weather-icons.svg#scattered-clouds-day").contains("weather-icons.svg#clear-day")
                .contains("aria-label=\"Overcast clouds\"").contains("destination-condition")
                .doesNotContain("◌").doesNotContain("openweathermap.org/img/wn");
    }

    @Test void geographicSceneMapKeepsCitiesSeparateFromWeatherConditions() throws Exception {
        var gangtok=LocationVisualCatalog.forLocation("Gangtok", "Gangtok");
        var mussoorie=LocationVisualCatalog.forLocation("Mussoorie", "Mussoorie");
        var prayagraj=LocationVisualCatalog.forLocation("Prayagraj", "Prayagraj");
        var unknown=LocationVisualCatalog.forLocation("Auckland", "Auckland");
        assertThat(gangtok.imageUrl()).contains("Gangtok_View_Point");
        assertThat(gangtok.imageAlt()).containsIgnoringCase("green");
        assertThat(mussoorie.imageUrl()).contains("Beautiful_Mussoorie");
        assertThat(prayagraj.imageUrl()).contains("Arail-ghatt");
        assertThat(prayagraj.sceneClass()).isEqualTo("river-city");
        assertThat(unknown.hasImage()).isFalse();

        for (String city : List.of("Gangtok", "Mussoorie", "Prayagraj")) {
            when(weather.current(city)).thenReturn(sample(city));
            String html=mvc.perform(get("/weather").param("city",city)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).contains("data-weather-code=\"804\"").contains("data-weather-icon=\"04d\"")
                    .contains("data-is-daylight=\"true\"");
            if (city.equals("Gangtok")) assertThat(html).contains("Gangtok_View_Point").doesNotContain("images.unsplash.com/photo-1501785888041");
            if (city.equals("Mussoorie")) assertThat(html).contains("Beautiful_Mussoorie");
            if (city.equals("Prayagraj")) assertThat(html).contains("Arail-ghatt").doesNotContain("mountain");
        }
    }

    @Test void creatorPageRendersTheRequestedPersonalStoryAndProjects() throws Exception {
        mvc.perform(get("/about")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hi, I’m")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chandigarh University, Mohali")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("artificial intelligence, and building useful digital experiences")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MSetu")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Monica")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Smart Agriculture System")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Built with curiosity, shaped by code, and inspired by the world outside.")));
    }

    @Test void dashboardRendersLocationSceneMetricsAndForecastTimeline() throws Exception {
        when(weather.current("Manali")).thenReturn(sample("Manali"));
        saved.save(sample("Manali"));
        String html=mvc.perform(get("/weather").param("city","Manali")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("LOCAL CONDITIONS")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("HUMIDITY")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("THE DAYS AHEAD")))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Manali_Mountains.jpg").contains("data-temp-c=\"12.0\"")
                .contains("M 500.0 30.0").contains("SUNRISE")
                .contains("35% rain chance").contains("hourly-ribbon-chart").contains("Remove saved");
    }

    @Test void windDaylightAndDailyCurveAreDerivedFromProviderValues() {
        WeatherData data=sample("Manali");
        assertThat(data.dailyForecastPath(true)).isEqualTo("M 500.0 30.0");
        assertThat(data.dailyForecastPath(false)).isEqualTo("M 500.0 130.0");
        assertThat(data.dailyForecastRangePath()).isEqualTo("M 500.0 30.0 L 500.0 130.0 Z");
        assertThat(data.windDirectionLabel()).isEqualTo("WSW");
        assertThat(data.windScaleLabel()).isEqualTo("Light air");
        assertThat(data.daylightProgressPercent()).isBetween(0.0,100.0);
        assertThat(data.daylightSunX()).isBetween(18.0,202.0);
        assertThat(data.daylightSunY()).isBetween(10.0,78.0);
    }

    private WeatherData withCondition(int conditionCode) {
        WeatherData data = sample("Manali");
        return new WeatherData(data.location(), data.state(), data.country(), data.latitude(), data.longitude(),
                data.temperatureC(), data.feelsLikeC(), data.humidity(), data.windSpeed(), data.windDirection(),
                data.pressure(), data.visibility(), data.cloudCoverage(), data.description(), data.icon(),
                data.observedAt(), data.sunrise(), data.sunset(), data.timezoneOffset(), data.forecast(),
                data.hourlyForecast(), conditionCode);
    }

    @Test void weatherSceneUsesProviderConditionCodeAndLocalDaylight() {
        assertThat(withCondition(800).weatherScene()).isEqualTo("clear");
        assertThat(withCondition(801).weatherScene()).isEqualTo("partly-cloudy");
        assertThat(withCondition(804).weatherScene()).isEqualTo("overcast");
        assertThat(withCondition(501).weatherScene()).isEqualTo("rain");
        assertThat(withCondition(600).weatherScene()).isEqualTo("snow");
        assertThat(withCondition(202).weatherScene()).isEqualTo("storm");
        assertThat(withCondition(202).thunderstormHasRain()).isTrue();
        assertThat(withCondition(211).thunderstormHasRain()).isFalse();
        WeatherData clearNight = withCondition(800);
        WeatherData afterSunset = new WeatherData(clearNight.location(), clearNight.state(), clearNight.country(),
                clearNight.latitude(), clearNight.longitude(), clearNight.temperatureC(), clearNight.feelsLikeC(),
                clearNight.humidity(), clearNight.windSpeed(), clearNight.windDirection(), clearNight.pressure(),
                clearNight.visibility(), clearNight.cloudCoverage(), clearNight.description(), clearNight.icon(),
                Instant.ofEpochSecond(clearNight.sunset() + 600), clearNight.sunrise(), clearNight.sunset(),
                clearNight.timezoneOffset(), clearNight.forecast(), clearNight.hourlyForecast(), 800);
        assertThat(clearNight.daylightAtObservation()).isTrue();
        assertThat(afterSunset.daylightAtObservation()).isFalse();
    }

    @Test void errorsOfferUsefulRetryForm() throws Exception {
        when(weather.current("Manali")).thenThrow(new WeatherException("OpenWeather says the API key is invalid.",502));
        mvc.perform(get("/weather").param("city","Manali")).andExpect(status().isOk()).andExpect(view().name("error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("OpenWeather says the API key is invalid.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Try again")));
    }

    @Test void savedLocationDeduplicates(){WeatherData d=sample("Manali");saved.save(d);saved.save(d);assertThat(locations.findAll()).hasSize(1);}

    @Test void savedPlacesAndHistoryPagesRender() throws Exception {
        mvc.perform(get("/locations")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Saved locations")));
        mvc.perform(get("/history")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Weather history")));
    }

    @Test void apiWeatherValidationReturns400() throws Exception {mvc.perform(get("/api/weather").param("city"," ")).andExpect(status().isBadRequest());}
}
