package io.github.solas.mcp.weather.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for wttr.in weather API (format=j1).
 */
class WttrInResponse {

    @JsonProperty("current_condition")
    private List<CurrentCondition> currentCondition;

    @JsonProperty("nearest_area")
    private List<NearestArea> nearestArea;

    @JsonProperty("weather")
    private List<Weather> weather;

    @JsonProperty("request")
    private List<Request> request;

    public WttrInResponse() {
    }

    public List<CurrentCondition> getCurrentCondition() {
        return currentCondition;
    }

    public void setCurrentCondition(List<CurrentCondition> currentCondition) {
        this.currentCondition = currentCondition;
    }

    public List<NearestArea> getNearestArea() {
        return nearestArea;
    }

    public void setNearestArea(List<NearestArea> nearestArea) {
        this.nearestArea = nearestArea;
    }

    public List<Weather> getWeather() {
        return weather;
    }

    public void setWeather(List<Weather> weather) {
        this.weather = weather;
    }

    public List<Request> getRequest() {
        return request;
    }

    public void setRequest(List<Request> request) {
        this.request = request;
    }

    /**
     * Current weather condition.
     */
    static class CurrentCondition {
        @JsonProperty("FeelsLikeC")
        private double feelsLikeC;

        @JsonProperty("FeelsLikeF")
        private double feelsLikeF;

        @JsonProperty("cloudcover")
        private int cloudCover;

        @JsonProperty("humidity")
        private int humidity;

        @JsonProperty("observation_time")
        private String observationTime;

        @JsonProperty("precipInches")
        private double precipInches;

        @JsonProperty("precipMM")
        private double precipMm;

        @JsonProperty("pressure")
        private int pressure;

        @JsonProperty("pressureInches")
        private int pressureInches;

        @JsonProperty("temp_C")
        private double tempC;

        @JsonProperty("temp_F")
        private double tempF;

        @JsonProperty("uvIndex")
        private int uvIndex;

        @JsonProperty("visibility")
        private int visibility;

        @JsonProperty("visibilityMiles")
        private int visibilityMiles;

        @JsonProperty("weatherCode")
        private int weatherCode;

        @JsonProperty("weatherDesc")
        private List<WeatherDesc> weatherDesc;

        @JsonProperty("weatherIconUrl")
        private List<WeatherIconUrl> weatherIconUrl;

        @JsonProperty("winddir16Point")
        private String windDir16Point;

        @JsonProperty("winddirDegree")
        private int windDirDegree;

        @JsonProperty("windspeedKmph")
        private int windSpeedKmph;

        @JsonProperty("windspeedMiles")
        private int windSpeedMiles;

        public CurrentCondition() {
        }

        public double getFeelsLikeC() {
            return feelsLikeC;
        }

        public void setFeelsLikeC(double feelsLikeC) {
            this.feelsLikeC = feelsLikeC;
        }

        public double getFeelsLikeF() {
            return feelsLikeF;
        }

        public void setFeelsLikeF(double feelsLikeF) {
            this.feelsLikeF = feelsLikeF;
        }

        public int getCloudCover() {
            return cloudCover;
        }

        public void setCloudCover(int cloudCover) {
            this.cloudCover = cloudCover;
        }

        public int getHumidity() {
            return humidity;
        }

        public void setHumidity(int humidity) {
            this.humidity = humidity;
        }

        public String getObservationTime() {
            return observationTime;
        }

        public void setObservationTime(String observationTime) {
            this.observationTime = observationTime;
        }

        public double getPrecipInches() {
            return precipInches;
        }

        public void setPrecipInches(double precipInches) {
            this.precipInches = precipInches;
        }

        public double getPrecipMm() {
            return precipMm;
        }

        public void setPrecipMm(double precipMm) {
            this.precipMm = precipMm;
        }

        public int getPressure() {
            return pressure;
        }

        public void setPressure(int pressure) {
            this.pressure = pressure;
        }

        public int getPressureInches() {
            return pressureInches;
        }

        public void setPressureInches(int pressureInches) {
            this.pressureInches = pressureInches;
        }

        public double getTempC() {
            return tempC;
        }

        public void setTempC(double tempC) {
            this.tempC = tempC;
        }

        public double getTempF() {
            return tempF;
        }

        public void setTempF(double tempF) {
            this.tempF = tempF;
        }

        public int getUvIndex() {
            return uvIndex;
        }

        public void setUvIndex(int uvIndex) {
            this.uvIndex = uvIndex;
        }

        public int getVisibility() {
            return visibility;
        }

        public void setVisibility(int visibility) {
            this.visibility = visibility;
        }

        public int getVisibilityMiles() {
            return visibilityMiles;
        }

        public void setVisibilityMiles(int visibilityMiles) {
            this.visibilityMiles = visibilityMiles;
        }

        public int getWeatherCode() {
            return weatherCode;
        }

        public void setWeatherCode(int weatherCode) {
            this.weatherCode = weatherCode;
        }

        public List<WeatherDesc> getWeatherDesc() {
            return weatherDesc;
        }

        public void setWeatherDesc(List<WeatherDesc> weatherDesc) {
            this.weatherDesc = weatherDesc;
        }

        public List<WeatherIconUrl> getWeatherIconUrl() {
            return weatherIconUrl;
        }

        public void setWeatherIconUrl(List<WeatherIconUrl> weatherIconUrl) {
            this.weatherIconUrl = weatherIconUrl;
        }

        public String getWindDir16Point() {
            return windDir16Point;
        }

        public void setWindDir16Point(String windDir16Point) {
            this.windDir16Point = windDir16Point;
        }

        public int getWindDirDegree() {
            return windDirDegree;
        }

        public void setWindDirDegree(int windDirDegree) {
            this.windDirDegree = windDirDegree;
        }

        public int getWindSpeedKmph() {
            return windSpeedKmph;
        }

        public void setWindSpeedKmph(int windSpeedKmph) {
            this.windSpeedKmph = windSpeedKmph;
        }

        public int getWindSpeedMiles() {
            return windSpeedMiles;
        }

        public void setWindSpeedMiles(int windSpeedMiles) {
            this.windSpeedMiles = windSpeedMiles;
        }
    }

    /**
     * Nearest area/location information.
     */
    static class NearestArea {
        @JsonProperty("areaName")
        private List<AreaName> areaName;

        @JsonProperty("country")
        private List<Country> country;

        @JsonProperty("latitude")
        private String latitude;

        @JsonProperty("longitude")
        private String longitude;

        @JsonProperty("population")
        private String population;

        @JsonProperty("region")
        private List<Region> region;

        @JsonProperty("weatherUrl")
        private List<WeatherUrl> weatherUrl;

        public NearestArea() {
        }

        public List<AreaName> getAreaName() {
            return areaName;
        }

        public void setAreaName(List<AreaName> areaName) {
            this.areaName = areaName;
        }

        public List<Country> getCountry() {
            return country;
        }

        public void setCountry(List<Country> country) {
            this.country = country;
        }

        public String getLatitude() {
            return latitude;
        }

        public void setLatitude(String latitude) {
            this.latitude = latitude;
        }

        public String getLongitude() {
            return longitude;
        }

        public void setLongitude(String longitude) {
            this.longitude = longitude;
        }

        public String getPopulation() {
            return population;
        }

        public void setPopulation(String population) {
            this.population = population;
        }

        public List<Region> getRegion() {
            return region;
        }

        public void setRegion(List<Region> region) {
            this.region = region;
        }

        public List<WeatherUrl> getWeatherUrl() {
            return weatherUrl;
        }

        public void setWeatherUrl(List<WeatherUrl> weatherUrl) {
            this.weatherUrl = weatherUrl;
        }
    }

    /**
     * Daily weather forecast.
     */
    static class Weather {
        @JsonProperty("astronomy")
        private List<Astronomy> astronomy;

        @JsonProperty("avgtempC")
        private double avgTempC;

        @JsonProperty("avgtempF")
        private double avgTempF;

        @JsonProperty("date")
        private String date;

        @JsonProperty("hourly")
        private List<Hourly> hourly;

        @JsonProperty("maxtempC")
        private double maxTempC;

        @JsonProperty("maxtempF")
        private double maxTempF;

        @JsonProperty("mintempC")
        private double minTempC;

        @JsonProperty("mintempF")
        private double minTempF;

        @JsonProperty("sunHour")
        private double sunHour;

        @JsonProperty("totalSnow_cm")
        private double totalSnowCm;

        @JsonProperty("uvIndex")
        private int uvIndex;

        public Weather() {
        }

        public List<Astronomy> getAstronomy() {
            return astronomy;
        }

        public void setAstronomy(List<Astronomy> astronomy) {
            this.astronomy = astronomy;
        }

        public double getAvgTempC() {
            return avgTempC;
        }

        public void setAvgTempC(double avgTempC) {
            this.avgTempC = avgTempC;
        }

        public double getAvgTempF() {
            return avgTempF;
        }

        public void setAvgTempF(double avgTempF) {
            this.avgTempF = avgTempF;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public List<Hourly> getHourly() {
            return hourly;
        }

        public void setHourly(List<Hourly> hourly) {
            this.hourly = hourly;
        }

        public double getMaxTempC() {
            return maxTempC;
        }

        public void setMaxTempC(double maxTempC) {
            this.maxTempC = maxTempC;
        }

        public double getMaxTempF() {
            return maxTempF;
        }

        public void setMaxTempF(double maxTempF) {
            this.maxTempF = maxTempF;
        }

        public double getMinTempC() {
            return minTempC;
        }

        public void setMinTempC(double minTempC) {
            this.minTempC = minTempC;
        }

        public double getMinTempF() {
            return minTempF;
        }

        public void setMinTempF(double minTempF) {
            this.minTempF = minTempF;
        }

        public double getSunHour() {
            return sunHour;
        }

        public void setSunHour(double sunHour) {
            this.sunHour = sunHour;
        }

        public double getTotalSnowCm() {
            return totalSnowCm;
        }

        public void setTotalSnowCm(double totalSnowCm) {
            this.totalSnowCm = totalSnowCm;
        }

        public int getUvIndex() {
            return uvIndex;
        }

        public void setUvIndex(int uvIndex) {
            this.uvIndex = uvIndex;
        }
    }

    /**
     * Request information.
     */
    static class Request {
        @JsonProperty("query")
        private String query;

        @JsonProperty("type")
        private String type;

        public Request() {
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    /**
     * Hourly forecast data.
     */
    static class Hourly {
        @JsonProperty("DewPointC")
        private int dewPointC;

        @JsonProperty("DewPointF")
        private int dewPointF;

        @JsonProperty("FeelsLikeC")
        private int feelsLikeC;

        @JsonProperty("FeelsLikeF")
        private int feelsLikeF;

        @JsonProperty("HeatIndexC")
        private int heatIndexC;

        @JsonProperty("HeatIndexF")
        private int heatIndexF;

        @JsonProperty("WindChillC")
        private int windChillC;

        @JsonProperty("WindChillF")
        private int windChillF;

        @JsonProperty("WindGustKmph")
        private int windGustKmph;

        @JsonProperty("WindGustMiles")
        private int windGustMiles;

        @JsonProperty("chanceoffog")
        private int chanceOfFog;

        @JsonProperty("chanceoffrost")
        private int chanceOfFrost;

        @JsonProperty("chanceofhightemp")
        private int chanceOfHighTemp;

        @JsonProperty("chanceofovercast")
        private int chanceOfOvercast;

        @JsonProperty("chanceofrain")
        private int chanceOfRain;

        @JsonProperty("chanceofremdry")
        private int chanceOfRemDry;

        @JsonProperty("chanceofsnow")
        private int chanceOfSnow;

        @JsonProperty("chanceofsunshine")
        private int chanceOfSunshine;

        @JsonProperty("chanceofthunder")
        private int chanceOfThunder;

        @JsonProperty("chanceofwindy")
        private int chanceOfWindy;

        @JsonProperty("cloudcover")
        private int cloudCover;

        @JsonProperty("diffRad")
        private double diffRad;

        @JsonProperty("humidity")
        private int humidity;

        @JsonProperty("precipInches")
        private double precipInches;

        @JsonProperty("precipMM")
        private double precipMm;

        @JsonProperty("pressure")
        private int pressure;

        @JsonProperty("pressureInches")
        private int pressureInches;

        @JsonProperty("shortRad")
        private double shortRad;

        @JsonProperty("tempC")
        private int tempC;

        @JsonProperty("tempF")
        private int tempF;

        @JsonProperty("time")
        private String time;

        @JsonProperty("uvIndex")
        private int uvIndex;

        @JsonProperty("visibility")
        private int visibility;

        @JsonProperty("visibilityMiles")
        private int visibilityMiles;

        @JsonProperty("weatherCode")
        private int weatherCode;

        @JsonProperty("weatherDesc")
        private List<WeatherDesc> weatherDesc;

        @JsonProperty("weatherIconUrl")
        private List<WeatherIconUrl> weatherIconUrl;

        @JsonProperty("winddir16Point")
        private String windDir16Point;

        @JsonProperty("winddirDegree")
        private int windDirDegree;

        @JsonProperty("windspeedKmph")
        private int windSpeedKmph;

        @JsonProperty("windspeedMiles")
        private int windSpeedMiles;

        public Hourly() {
        }

        public int getDewPointC() {
            return dewPointC;
        }

        public void setDewPointC(int dewPointC) {
            this.dewPointC = dewPointC;
        }

        public int getDewPointF() {
            return dewPointF;
        }

        public void setDewPointF(int dewPointF) {
            this.dewPointF = dewPointF;
        }

        public int getFeelsLikeC() {
            return feelsLikeC;
        }

        public void setFeelsLikeC(int feelsLikeC) {
            this.feelsLikeC = feelsLikeC;
        }

        public int getFeelsLikeF() {
            return feelsLikeF;
        }

        public void setFeelsLikeF(int feelsLikeF) {
            this.feelsLikeF = feelsLikeF;
        }

        public int getHeatIndexC() {
            return heatIndexC;
        }

        public void setHeatIndexC(int heatIndexC) {
            this.heatIndexC = heatIndexC;
        }

        public int getHeatIndexF() {
            return heatIndexF;
        }

        public void setHeatIndexF(int heatIndexF) {
            this.heatIndexF = heatIndexF;
        }

        public int getWindChillC() {
            return windChillC;
        }

        public void setWindChillC(int windChillC) {
            this.windChillC = windChillC;
        }

        public int getWindChillF() {
            return windChillF;
        }

        public void setWindChillF(int windChillF) {
            this.windChillF = windChillF;
        }

        public int getWindGustKmph() {
            return windGustKmph;
        }

        public void setWindGustKmph(int windGustKmph) {
            this.windGustKmph = windGustKmph;
        }

        public int getWindGustMiles() {
            return windGustMiles;
        }

        public void setWindGustMiles(int windGustMiles) {
            this.windGustMiles = windGustMiles;
        }

        public int getChanceOfFog() {
            return chanceOfFog;
        }

        public void setChanceOfFog(int chanceOfFog) {
            this.chanceOfFog = chanceOfFog;
        }

        public int getChanceOfFrost() {
            return chanceOfFrost;
        }

        public void setChanceOfFrost(int chanceOfFrost) {
            this.chanceOfFrost = chanceOfFrost;
        }

        public int getChanceOfHighTemp() {
            return chanceOfHighTemp;
        }

        public void setChanceOfHighTemp(int chanceOfHighTemp) {
            this.chanceOfHighTemp = chanceOfHighTemp;
        }

        public int getChanceOfOvercast() {
            return chanceOfOvercast;
        }

        public void setChanceOfOvercast(int chanceOfOvercast) {
            this.chanceOfOvercast = chanceOfOvercast;
        }

        public int getChanceOfRain() {
            return chanceOfRain;
        }

        public void setChanceOfRain(int chanceOfRain) {
            this.chanceOfRain = chanceOfRain;
        }

        public int getChanceOfRemDry() {
            return chanceOfRemDry;
        }

        public void setChanceOfRemDry(int chanceOfRemDry) {
            this.chanceOfRemDry = chanceOfRemDry;
        }

        public int getChanceOfSnow() {
            return chanceOfSnow;
        }

        public void setChanceOfSnow(int chanceOfSnow) {
            this.chanceOfSnow = chanceOfSnow;
        }

        public int getChanceOfSunshine() {
            return chanceOfSunshine;
        }

        public void setChanceOfSunshine(int chanceOfSunshine) {
            this.chanceOfSunshine = chanceOfSunshine;
        }

        public int getChanceOfThunder() {
            return chanceOfThunder;
        }

        public void setChanceOfThunder(int chanceOfThunder) {
            this.chanceOfThunder = chanceOfThunder;
        }

        public int getChanceOfWindy() {
            return chanceOfWindy;
        }

        public void setChanceOfWindy(int chanceOfWindy) {
            this.chanceOfWindy = chanceOfWindy;
        }

        public int getCloudCover() {
            return cloudCover;
        }

        public void setCloudCover(int cloudCover) {
            this.cloudCover = cloudCover;
        }

        public double getDiffRad() {
            return diffRad;
        }

        public void setDiffRad(double diffRad) {
            this.diffRad = diffRad;
        }

        public int getHumidity() {
            return humidity;
        }

        public void setHumidity(int humidity) {
            this.humidity = humidity;
        }

        public double getPrecipInches() {
            return precipInches;
        }

        public void setPrecipInches(double precipInches) {
            this.precipInches = precipInches;
        }

        public double getPrecipMm() {
            return precipMm;
        }

        public void setPrecipMm(double precipMm) {
            this.precipMm = precipMm;
        }

        public int getPressure() {
            return pressure;
        }

        public void setPressure(int pressure) {
            this.pressure = pressure;
        }

        public int getPressureInches() {
            return pressureInches;
        }

        public void setPressureInches(int pressureInches) {
            this.pressureInches = pressureInches;
        }

        public double getShortRad() {
            return shortRad;
        }

        public void setShortRad(double shortRad) {
            this.shortRad = shortRad;
        }

        public int getTempC() {
            return tempC;
        }

        public void setTempC(int tempC) {
            this.tempC = tempC;
        }

        public int getTempF() {
            return tempF;
        }

        public void setTempF(int tempF) {
            this.tempF = tempF;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public int getUvIndex() {
            return uvIndex;
        }

        public void setUvIndex(int uvIndex) {
            this.uvIndex = uvIndex;
        }

        public int getVisibility() {
            return visibility;
        }

        public void setVisibility(int visibility) {
            this.visibility = visibility;
        }

        public int getVisibilityMiles() {
            return visibilityMiles;
        }

        public void setVisibilityMiles(int visibilityMiles) {
            this.visibilityMiles = visibilityMiles;
        }

        public int getWeatherCode() {
            return weatherCode;
        }

        public void setWeatherCode(int weatherCode) {
            this.weatherCode = weatherCode;
        }

        public List<WeatherDesc> getWeatherDesc() {
            return weatherDesc;
        }

        public void setWeatherDesc(List<WeatherDesc> weatherDesc) {
            this.weatherDesc = weatherDesc;
        }

        public List<WeatherIconUrl> getWeatherIconUrl() {
            return weatherIconUrl;
        }

        public void setWeatherIconUrl(List<WeatherIconUrl> weatherIconUrl) {
            this.weatherIconUrl = weatherIconUrl;
        }

        public String getWindDir16Point() {
            return windDir16Point;
        }

        public void setWindDir16Point(String windDir16Point) {
            this.windDir16Point = windDir16Point;
        }

        public int getWindDirDegree() {
            return windDirDegree;
        }

        public void setWindDirDegree(int windDirDegree) {
            this.windDirDegree = windDirDegree;
        }

        public int getWindSpeedKmph() {
            return windSpeedKmph;
        }

        public void setWindSpeedKmph(int windSpeedKmph) {
            this.windSpeedKmph = windSpeedKmph;
        }

        public int getWindSpeedMiles() {
            return windSpeedMiles;
        }

        public void setWindSpeedMiles(int windSpeedMiles) {
            this.windSpeedMiles = windSpeedMiles;
        }
    }

    /**
     * Astronomy data (sunrise, sunset, moon phase).
     */
    static class Astronomy {
        @JsonProperty("moon_illumination")
        private String moonIllumination;

        @JsonProperty("moon_phase")
        private String moonPhase;

        @JsonProperty("moonrise")
        private String moonrise;

        @JsonProperty("moonset")
        private String moonset;

        @JsonProperty("sunrise")
        private String sunrise;

        @JsonProperty("sunset")
        private String sunset;

        public Astronomy() {
        }

        public String getMoonIllumination() {
            return moonIllumination;
        }

        public void setMoonIllumination(String moonIllumination) {
            this.moonIllumination = moonIllumination;
        }

        public String getMoonPhase() {
            return moonPhase;
        }

        public void setMoonPhase(String moonPhase) {
            this.moonPhase = moonPhase;
        }

        public String getMoonrise() {
            return moonrise;
        }

        public void setMoonrise(String moonrise) {
            this.moonrise = moonrise;
        }

        public String getMoonset() {
            return moonset;
        }

        public void setMoonset(String moonset) {
            this.moonset = moonset;
        }

        public String getSunrise() {
            return sunrise;
        }

        public void setSunrise(String sunrise) {
            this.sunrise = sunrise;
        }

        public String getSunset() {
            return sunset;
        }

        public void setSunset(String sunset) {
            this.sunset = sunset;
        }
    }

    /**
     * Weather description wrapper.
     */
    static class WeatherDesc {
        @JsonProperty("value")
        private String value;

        public WeatherDesc() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * Weather icon URL wrapper.
     */
    static class WeatherIconUrl {
        @JsonProperty("value")
        private String value;

        public WeatherIconUrl() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * Area name wrapper.
     */
    static class AreaName {
        @JsonProperty("value")
        private String value;

        public AreaName() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * Country wrapper.
     */
    static class Country {
        @JsonProperty("value")
        private String value;

        public Country() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * Region wrapper.
     */
    static class Region {
        @JsonProperty("value")
        private String value;

        public Region() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * Weather URL wrapper.
     */
    static class WeatherUrl {
        @JsonProperty("value")
        private String value;

        public WeatherUrl() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
