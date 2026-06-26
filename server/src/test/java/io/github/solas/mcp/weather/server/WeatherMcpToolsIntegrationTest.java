package io.github.solas.mcp.weather.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.solas.mcp.weather.api.CurrentWeather;
import io.github.solas.mcp.weather.api.DailyForecast;
import io.github.solas.mcp.weather.api.Location;
import io.github.solas.mcp.weather.api.WeatherApiException;
import io.github.solas.mcp.weather.api.WeatherClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class WeatherMcpToolsIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WeatherClient weatherClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testEndToEndCurrentWeather() throws Exception {
        when(weatherClient.getCurrentWeather(any()))
            .thenReturn(new CurrentWeather(22.5, 20.0, 65, 1013, 5.2, "NW", 1, "Mainly clear"));

        String request = createToolCallRequest("get_current_weather", Map.of("city", "Moscow"));
        String response = mockMvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        McpSchema.CallToolResult result = objectMapper.readValue(response, McpSchema.CallToolResult.class);
        assertThat(result.content()).hasSize(2);
        assertThat(result.isError()).isFalse();
        
        McpSchema.TextContent humanContent = (McpSchema.TextContent) result.content().get(0);
        assertThat(humanContent.text()).contains("Moscow").contains("22.5").contains("Mainly clear");
        
        McpSchema.TextContent jsonContent = (McpSchema.TextContent) result.content().get(1);
        assertThat(jsonContent.text()).contains("\"temperature\"").contains("22.5");
    }

    @Test
    void testEndToEndForecast() throws Exception {
        when(weatherClient.getForecast(any(), eq(3)))
            .thenReturn(List.of(
                new DailyForecast("2025-01-01", 22.5, 15.0, 0.0, 1),
                new DailyForecast("2025-01-02", 21.0, 14.5, 2.5, 61),
                new DailyForecast("2025-01-03", 19.5, 13.0, 5.0, 63)
            ));

        String request = createToolCallRequest("get_forecast", Map.of("city", "Moscow", "days", 3));
        String response = mockMvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        McpSchema.CallToolResult result = objectMapper.readValue(response, McpSchema.CallToolResult.class);
        assertThat(result.content()).hasSize(2);
        assertThat(result.isError()).isFalse();
        
        McpSchema.TextContent humanContent = (McpSchema.TextContent) result.content().get(0);
        assertThat(humanContent.text()).contains("Moscow").contains("3-day");
        
        McpSchema.TextContent jsonContent = (McpSchema.TextContent) result.content().get(1);
        assertThat(jsonContent.text()).contains("[{\"date\":\"2025-01-01\"");
    }

    @Test
    void testEndToEndSearchCity() throws Exception {
        when(weatherClient.searchCities(any()))
            .thenReturn(List.of(new Location("Moscow", "Russia", 55.7558, 37.6173, "Europe/Moscow", 156.0)));

        String request = createToolCallRequest("search_city", Map.of("query", "Moscow"));
        String response = mockMvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        McpSchema.CallToolResult result = objectMapper.readValue(response, McpSchema.CallToolResult.class);
        assertThat(result.content()).hasSize(2);
        assertThat(result.isError()).isFalse();
        
        McpSchema.TextContent humanContent = (McpSchema.TextContent) result.content().get(0);
        assertThat(humanContent.text()).contains("Moscow").contains("Russia");
        
        McpSchema.TextContent jsonContent = (McpSchema.TextContent) result.content().get(1);
        assertThat(jsonContent.text()).contains("\"name\":\"Moscow\"");
    }

    @Test
    void testRussianLanguageDetection() throws Exception {
        when(weatherClient.getCurrentWeather(any()))
            .thenReturn(new CurrentWeather(18.3, 16.5, 78, 1008, 7.8, "W", 3, "Overcast"));

        String request = createToolCallRequest("get_current_weather", Map.of("city", "Санкт-Петербург"));
        String response = mockMvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        McpSchema.CallToolResult result = objectMapper.readValue(response, McpSchema.CallToolResult.class);
        assertThat(result.content()).hasSize(2);
        
        McpSchema.TextContent humanContent = (McpSchema.TextContent) result.content().get(0);
        assertThat(humanContent.text()).contains("В Санкт-Петербурге").contains("18.3");
    }

    @Test
    void testToolErrorHandling() throws Exception {
        when(weatherClient.getCurrentWeather(any()))
            .thenThrow(new WeatherApiException("City not found", null, "NOT_FOUND"));

        String request = createToolCallRequest("get_current_weather", Map.of("city", "UnknownCity"));
        String response = mockMvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        McpSchema.CallToolResult result = objectMapper.readValue(response, McpSchema.CallToolResult.class);
        assertThat(result.isError()).isTrue();
        assertThat(result.content()).hasSize(1);
        
        McpSchema.TextContent errorContent = (McpSchema.TextContent) result.content().get(0);
        assertThat(errorContent.text()).contains("Error").contains("City not found");
    }

    private String createToolCallRequest(String toolName, Map<String, Object> arguments) throws Exception {
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, arguments);
        return objectMapper.writeValueAsString(request);
    }
}
