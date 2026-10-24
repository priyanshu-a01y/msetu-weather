package com.summitsky;

import com.summitsky.model.WeatherConditionIcon;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherConditionIconTest {
    @Test void everyOpenWeatherConditionCodeHasAnAppropriateSvgSymbol() {
        assertCodes(new int[]{200,201,202,210,211,212,221,230,231,232}, "thunderstorm");
        assertCodes(new int[]{300,301,302,310,311,312,313,314,321}, "drizzle");
        assertCodes(new int[]{500,501,502,503,504,511,520,521,522,531}, "rain");
        assertCodes(new int[]{600,601,602,611,612,613,615,616,620,621,622}, "snow");
        assertCodes(new int[]{701,741}, "mist");
        assertCodes(new int[]{711}, "smoke");
        assertCodes(new int[]{721,762}, "haze");
        assertCodes(new int[]{731,751,761}, "dust");
        assertCodes(new int[]{771}, "wind");
        assertCodes(new int[]{781}, "tornado");
        assertCodes(new int[]{803}, "broken-clouds");
        assertCodes(new int[]{804}, "overcast");
        assertThat(WeatherConditionIcon.from(800, "clear sky", "01d", true).symbol()).isEqualTo("clear-day");
        assertThat(WeatherConditionIcon.from(801, "few clouds", "02d", true).symbol()).isEqualTo("few-clouds-day");
        assertThat(WeatherConditionIcon.from(802, "scattered clouds", "03d", true).symbol()).isEqualTo("scattered-clouds-day");
    }

    @Test void clearAndFewCloudConditionsUseProviderDayNightIcon() {
        assertThat(WeatherConditionIcon.from(800, "clear sky", "01n", true).symbol()).isEqualTo("clear-night");
        assertThat(WeatherConditionIcon.from(0, "clear sky", "", false).symbol()).isEqualTo("clear-night");
        assertThat(WeatherConditionIcon.from(801, "few clouds", "02n", true).symbol()).isEqualTo("few-clouds-night");
        assertThat(WeatherConditionIcon.from(802, "scattered clouds", "03n", false).symbol()).isEqualTo("scattered-clouds-night");
    }

    @Test void descriptionsRecoverAValidIconAndUnknownConditionsUseANeutralCloud() {
        assertThat(WeatherConditionIcon.from(0, "light rain", "", true).symbol()).isEqualTo("rain");
        assertThat(WeatherConditionIcon.from(999, "haze", "", true).symbol()).isEqualTo("haze");
        var unknown = WeatherConditionIcon.from(999, "unclassified conditions", "", true);
        assertThat(unknown.symbol()).isEqualTo("neutral-cloud");
        assertThat(unknown.label()).isEqualTo("Unclassified conditions");
        assertThat(unknown.label()).isNotBlank();
    }

    @Test void everyMappedSymbolExistsInTheLocalSvgSprite() throws Exception {
        String sprite;
        try (var input = getClass().getResourceAsStream("/static/images/weather-icons.svg")) {
            assertThat(input).as("local icon sprite").isNotNull();
            sprite = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        for (String symbol : new String[]{"clear-day","clear-night","few-clouds-day","few-clouds-night",
                "scattered-clouds-day","scattered-clouds-night","broken-clouds","overcast","drizzle","rain",
                "thunderstorm","snow","mist","smoke","haze","dust","wind","tornado","neutral-cloud"}) {
            assertThat(sprite).as("SVG sprite symbol %s", symbol).contains("id=\"" + symbol + "\"");
        }
    }

    private void assertCodes(int[] codes, String symbol) {
        for (int code : codes) {
            var icon = WeatherConditionIcon.from(code, "", "", true);
            assertThat(icon.symbol()).as("condition code %s", code).isEqualTo(symbol);
            assertThat(icon.label()).as("accessible label for %s", code).isNotBlank();
            assertThat(icon.symbol()).as("valid code %s must not produce a placeholder", code).isNotEqualTo("neutral-cloud");
        }
    }
}
