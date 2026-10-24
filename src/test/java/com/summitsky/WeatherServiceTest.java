package com.summitsky;

import com.summitsky.entity.WeatherHistory;
import com.summitsky.repository.WeatherHistoryRepository;
import com.summitsky.service.WeatherException;
import com.summitsky.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class WeatherServiceTest {
    private WeatherHistoryRepository history;
    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private WeatherService weather;

    @BeforeEach void setup() {
        history=mock(WeatherHistoryRepository.class);
        builder=RestClient.builder();
        server=MockRestServiceServer.bindTo(builder).build();
        weather=new WeatherService(builder.build(),"test-key","https://weather.test/data/2.5","https://weather.test/geo/1.0",history);
    }
    private void manaliGeocode(){
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/geo/1.0/direct")))
          .andExpect(queryParam("q","Manali,Himachal%20Pradesh,IN")).andExpect(queryParam("appid","test-key"))
          .andRespond(withSuccess("[{\"name\":\"Manali\",\"state\":\"Himachal Pradesh\",\"country\":\"IN\",\"lat\":32.2396,\"lon\":77.1887}]",MediaType.APPLICATION_JSON));
    }
    private void current(){
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/data/2.5/weather")))
          .andExpect(queryParam("lat","32.2396")).andExpect(queryParam("lon","77.1887")).andExpect(queryParam("units","metric"))
          .andRespond(withSuccess("""
          {"coord":{"lat":32.24,"lon":77.19},"name":"Manali","sys":{"country":"IN"},"main":{"temp":29.0,"feels_like":32.54,"temp_min":28.75,"temp_max":30.1,"humidity":72,"pressure":812},"weather":[{"id":804,"description":"overcast clouds","icon":"04d"}],"wind":{"speed":3.84,"deg":250},"clouds":{"all":88},"visibility":8000,"dt":1792838400,"timezone":19800}
          """,MediaType.APPLICATION_JSON));
    }
    private void forecast(){
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/data/2.5/forecast"))).andExpect(queryParam("units","metric"))
          .andRespond(withSuccess("""
          {"city":{"timezone":19800},"list":[{"dt":1792840000,"main":{"temp":14,"temp_min":10,"temp_max":16},"weather":[{"id":802,"description":"scattered clouds","icon":"03d"}],"pop":0.45},{"dt":1792860000,"main":{"temp":15,"temp_min":12,"temp_max":18},"weather":[{"id":500,"description":"light rain","icon":"10d"}]}]}
          """,MediaType.APPLICATION_JSON));
    }
    @Test void resolvesManaliAndMapsMetricObservationLocalTimeAndForecast(){
        manaliGeocode();current();forecast();var result=weather.current("Manali");
        assertThat(result.location()).isEqualTo("Manali");assertThat(result.state()).isEqualTo("Himachal Pradesh");assertThat(result.country()).isEqualTo("IN");
        assertThat(result.latitude()).isEqualTo(32.2396);assertThat(result.temperatureC()).isEqualTo(29.0);assertThat(result.feelsLikeC()).isEqualTo(32.54);
        assertThat(result.humidity()).isEqualTo(72);assertThat(result.windSpeed()).isEqualTo(3.84);assertThat(result.windDirectionLabel()).isEqualTo("WSW");
        assertThat(result.cloudCoverage()).isEqualTo(88);assertThat(result.observedAt()).isEqualTo(Instant.ofEpochSecond(1792838400));
        assertThat(result.conditionCode()).isEqualTo(804);assertThat(result.weatherScene()).isEqualTo("overcast");
        assertThat(result.localObservedAt().getOffset().getTotalSeconds()).isEqualTo(19800);assertThat(result.timezoneLabel()).isEqualTo("UTC+05:30");assertThat(result.countryDisplayName()).isEqualTo("India");assertThat(result.forecast()).hasSize(1);
        assertThat(result.forecast().getFirst().conditionCode()).isEqualTo(802);assertThat(result.hourlyForecast().getFirst().conditionCode()).isEqualTo(802);assertThat(result.hourlyForecast().get(1).conditionCode()).isEqualTo(500);
        assertThat(result.forecast().getFirst().lowC()).isEqualTo(10);assertThat(result.forecast().getFirst().highC()).isEqualTo(18);assertThat(result.todayForecast().highC()).isEqualTo(18);assertThat(result.hourlyForecast()).hasSize(2);assertThat(result.hourlyForecast().getFirst().temperatureC()).isEqualTo(14);assertThat(result.hourlyForecast().getFirst().precipitationProbability()).isEqualTo(45);assertThat(result.hourlyForecast().get(1).precipitationProbability()).isNull();
        assertThat(result.hourlyLinePoints()).isEqualTo("28.0,76.0 572.0,28.0");
        verify(history).save(any(WeatherHistory.class));server.verify();
    }
    @Test void locationNotFoundDoesNotCallCurrentWeather(){
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/geo/1.0/direct"))).andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->weather.current("Unknown Place")).isInstanceOf(WeatherException.class).hasMessageContaining("couldn't find");server.verify();verifyNoInteractions(history);
    }
    @Test void rejectsManaliWhenGeocoderReturnsDifferentRegion(){
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/geo/1.0/direct"))).andRespond(withSuccess("[{\"name\":\"Manali\",\"state\":\"Somewhere Else\",\"country\":\"US\",\"lat\":30,\"lon\":70}]",MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->weather.current("Manali")).isInstanceOf(WeatherException.class).hasMessageContaining("Himachal Pradesh");server.verify();
    }
    @Test void distinguishesInvalidAndInactiveKeysAndRateLimits(){
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/geo/1.0/direct"))).andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED).body("{\"message\":\"Invalid API key: test-key\"}").contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->weather.current("Manali")).isInstanceOf(WeatherException.class).hasMessageContaining("invalid").hasMessageNotContaining("test-key");server.verify();server.reset();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/geo/1.0/direct"))).andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED).body("{\"message\":\"API key not activated yet\"}").contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->weather.current("Manali")).isInstanceOf(WeatherException.class).hasMessageContaining("not active yet");server.verify();server.reset();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/geo/1.0/direct"))).andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS).body("{\"message\":\"too many requests\"}").contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->weather.current("Manali")).isInstanceOf(WeatherException.class).hasMessageContaining("HTTP 429");server.verify();
    }
    @Test void malformedCurrentResponseIsRejected(){manaliGeocode();server.expect(requestTo(org.hamcrest.Matchers.containsString("/data/2.5/weather"))).andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));assertThatThrownBy(()->weather.current("Manali")).isInstanceOf(WeatherException.class).hasMessageContaining("incomplete current conditions");server.verify();}
    @Test void mapsReportedOneHourRainfallIntensity(){
        manaliGeocode();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/data/2.5/weather"))).andRespond(withSuccess("""
          {"name":"Manali","sys":{"country":"IN","sunrise":1792820000,"sunset":1792860000},"main":{"temp":9,"feels_like":8,"humidity":71,"pressure":812},"weather":[{"id":500,"description":"light rain","icon":"10d"}],"wind":{"speed":2.4,"deg":250},"dt":1792838400,"timezone":19800,"rain":{"1h":4.5}}
          """,MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/data/2.5/forecast")))
                .andRespond(withSuccess("{\"city\":{\"timezone\":19800},\"list\":[]}",MediaType.APPLICATION_JSON));
        var result=weather.current("Manali");
        assertThat(result.conditionCode()).isEqualTo(500);
        assertThat(result.weatherScene()).isEqualTo("rain");
        assertThat(result.precipitationMmPerHour()).isEqualTo(4.5);
        assertThat(result.precipitationIntensity()).isEqualTo("moderate");
        server.verify();
    }
}





