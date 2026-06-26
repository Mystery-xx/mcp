package io.github.solas.mcp.weather.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WmoCodeTest {

    @Test
    void testSunnyClear() {
        assertThat(WmoCode.getDescription(113)).isEqualTo("Sunny/Clear");
    }

    @Test
    void testPartlyCloudy() {
        assertThat(WmoCode.getDescription(116)).isEqualTo("Partly Cloudy");
    }

    @Test
    void testCloudy() {
        assertThat(WmoCode.getDescription(119)).isEqualTo("Cloudy");
    }

    @Test
    void testOvercast() {
        assertThat(WmoCode.getDescription(122)).isEqualTo("Overcast");
    }

    @Test
    void testMist() {
        assertThat(WmoCode.getDescription(143)).isEqualTo("Mist");
    }

    @Test
    void testFog() {
        assertThat(WmoCode.getDescription(248)).isEqualTo("Fog");
    }

    @Test
    void testFreezingFog() {
        assertThat(WmoCode.getDescription(260)).isEqualTo("Freezing fog");
    }

    @Test
    void testLightDrizzle() {
        assertThat(WmoCode.getDescription(263)).isEqualTo("Light drizzle");
    }

    @Test
    void testFreezingDrizzle() {
        assertThat(WmoCode.getDescription(281)).isEqualTo("Freezing drizzle");
    }

    @Test
    void testPatchyRain() {
        assertThat(WmoCode.getDescription(176)).isEqualTo("Patchy rain nearby");
    }

    @Test
    void testLightRain() {
        assertThat(WmoCode.getDescription(296)).isEqualTo("Light rain");
    }

    @Test
    void testModerateRain() {
        assertThat(WmoCode.getDescription(302)).isEqualTo("Moderate rain");
    }

    @Test
    void testHeavyRain() {
        assertThat(WmoCode.getDescription(308)).isEqualTo("Heavy rain");
    }

    @Test
    void testRainShower() {
        assertThat(WmoCode.getDescription(353)).isEqualTo("Light rain shower");
    }

    @Test
    void testTorrentialRain() {
        assertThat(WmoCode.getDescription(359)).isEqualTo("Torrential rain shower");
    }

    @Test
    void testFreezingRain() {
        assertThat(WmoCode.getDescription(311)).isEqualTo("Light freezing rain");
    }

    @Test
    void testPatchySnow() {
        assertThat(WmoCode.getDescription(179)).isEqualTo("Patchy snow nearby");
    }

    @Test
    void testBlowingSnow() {
        assertThat(WmoCode.getDescription(227)).isEqualTo("Blowing snow");
    }

    @Test
    void testBlizzard() {
        assertThat(WmoCode.getDescription(230)).isEqualTo("Blizzard");
    }

    @Test
    void testLightSnow() {
        assertThat(WmoCode.getDescription(326)).isEqualTo("Light snow");
    }

    @Test
    void testHeavySnow() {
        assertThat(WmoCode.getDescription(338)).isEqualTo("Heavy snow");
    }

    @Test
    void testIcePellets() {
        assertThat(WmoCode.getDescription(350)).isEqualTo("Ice pellets");
    }

    @Test
    void testPatchySleet() {
        assertThat(WmoCode.getDescription(182)).isEqualTo("Patchy sleet nearby");
    }

    @Test
    void testLightSleet() {
        assertThat(WmoCode.getDescription(317)).isEqualTo("Light sleet");
    }

    @Test
    void testThunder() {
        assertThat(WmoCode.getDescription(200)).isEqualTo("Thundery outbreaks in nearby");
    }

    @Test
    void testThunderWithRain() {
        assertThat(WmoCode.getDescription(389)).isEqualTo("Moderate or heavy rain in area with thunder");
    }

    @Test
    void testThunderWithSnow() {
        assertThat(WmoCode.getDescription(395)).isEqualTo("Moderate or heavy snow in area with thunder");
    }

    @Test
    void testUnknownCode() {
        assertThat(WmoCode.getDescription(127)).isEqualTo("WMO code: 127");
    }

    @Test
    void testNegativeCode() {
        assertThat(WmoCode.getDescription(-1)).isEqualTo("WMO code: -1");
    }

    @Test
    void testVeryLargeCode() {
        assertThat(WmoCode.getDescription(999)).isEqualTo("WMO code: 999");
    }
}
