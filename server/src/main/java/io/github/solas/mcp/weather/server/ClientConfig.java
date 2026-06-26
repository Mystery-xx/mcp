package io.github.solas.mcp.weather.server;

import io.github.solas.mcp.weather.api.WeatherClient;
import io.github.solas.mcp.weather.client.NominatimClient;
import io.github.solas.mcp.weather.client.RateLimiter;
import io.github.solas.mcp.weather.client.WttrInClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // wttr.in returns JSON with text/plain content type, so we need to configure the converter
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        List<MediaType> supportedMediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
        supportedMediaTypes.add(MediaType.TEXT_PLAIN);
        converter.setSupportedMediaTypes(supportedMediaTypes);
        restTemplate.getMessageConverters().add(converter);
        return restTemplate;
    }

    @Bean
    public RateLimiter rateLimiter() {
        return new RateLimiter();
    }

    @Bean
    public NominatimClient nominatimClient(RestTemplate restTemplate) {
        return new NominatimClient(restTemplate);
    }

    @Bean
    public WeatherClient weatherClient(RestTemplate restTemplate, NominatimClient nominatimClient) {
        return new WttrInClient(restTemplate, nominatimClient);
    }
}
