# AGENTS.md — AI Agent Development Guide

## Project Context

### What This Is

Weather MCP Server is a Model Context Protocol (MCP) server that exposes weather data tools to AI assistants. It connects to the OpenMeteo API and provides three tools for querying current weather, forecasts, and city geocoding.

### Purpose

- Enable AI assistants (Claude, Cursor, etc.) to fetch real-time weather data
- Provide structured responses with dual format: human-readable text + JSON
- Support multilingual responses (automatic Russian detection via Cyrillic characters)

### Core Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Runtime language |
| Spring Boot | 3.3.13 | Application framework |
| MCP SDK | 2.0.0 | Protocol implementation |
| OpenMeteo API | — | Weather data source |
| Gradle | 8.7 | Build system |
| Jackson | 2.17.0 | JSON serialization |

---

## Development Guidelines

### Code Style

Follow standard Java conventions:

- **Classes**: PascalCase (`WeatherMcpTools`, `OpenMeteoClient`)
- **Methods**: camelCase (`getCurrentWeather`, `handleGetForecast`)
- **Constants**: UPPER_SNAKE_CASE (`CODE_MAP`)
- **Packages**: `io.github.solas.mcp.weather.<module>`

**Record usage for DTOs:**
```java
public record CurrentWeather(
    double temperature,
    double feelsLike,
    int humidity,
    int pressure,
    double windSpeed,
    String windDirection,
    int weatherCode,
    String description
) {}
```

**Exception handling:**
- Catch specific exceptions first (`WeatherApiException`)
- Wrap checked exceptions in runtime exceptions
- Return MCP error results via `createErrorResult()`

### Module Boundaries

**api/** — Shared contracts, no external dependencies
- DTOs: `CurrentWeather`, `DailyForecast`, `Location`, `WeatherData`
- Interfaces: `WeatherClient`
- Utilities: `WmoCode`, `WeatherApiException`
- **NO** Spring, **NO** HTTP client code

**client/** — OpenMeteo integration
- HTTP client: `OpenMeteoClient` (uses `RestClient`)
- Retry logic: `RetryUtils` (3 attempts, 1s delay)
- Response mappers: `GeocodingResponse`, `ForecastResponse`
- **DEPENDS ON**: `api`

**server/** — MCP server and Spring Boot app
- MCP tools: `WeatherMcpTools` (tool registration + handlers)
- Configuration: `McpServerConfig`, `ClientConfig`
- Utilities: `LanguageUtils` (Cyrillic detection)
- **DEPENDS ON**: `api`, `client`

### Testing Requirements

**Unit tests** (JUnit 5 + AssertJ):
```java
@Test
void shouldReturnDescriptionForKnownCode() {
    assertThat(WmoCode.getDescription(0)).isEqualTo("Clear sky");
}
```

**Mocking pattern** (Mockito):
```java
@Mock
private WeatherClient weatherClient;

@InjectMocks
private WeatherMcpTools tools;

@Test
void shouldReturnWeatherData() {
    when(weatherClient.getCurrentWeather("Moscow"))
        .thenReturn(new CurrentWeather(...));
    
    McpSchema.CallToolResult result = tools.handleGetCurrentWeather(request);
    
    assertThat(result.isError()).isFalse();
}
```

**Integration tests** (Spring Boot Test):
```java
@SpringBootTest
@AutoConfigureMockMvc
class WeatherMcpToolsIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldRespondOnMcpEndpoint() throws Exception {
        mockMvc.perform(post("/mcp")
                .contentType("application/json")
                .content("{\"jsonrpc\":\"2.0\", ...}"))
            .andExpect(status().isOk());
    }
}
```

---

## Build System

### Gradle Structure

**Root build.gradle** — Plugin management, Java toolchain
```gradle
plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.dependency.management) apply false
}

subprojects {
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }
}
```

**Version catalog** (gradle/libs.versions.toml):
```toml
[versions]
spring-boot = "3.3.13"
mcp = "2.0.0"
jackson = "2.17.0"
junit = "5.10.2"

[libraries]
mcp-core = { group = "io.modelcontextprotocol.sdk", name = "mcp-core", version.ref = "mcp" }
mcp-json-jackson2 = { group = "io.modelcontextprotocol.sdk", name = "mcp-json-jackson2", version.ref = "mcp" }
```

**Module dependencies:**
```gradle
// server/build.gradle
dependencies {
    implementation project(':api')
    implementation project(':client')
    implementation libs.mcp.core
    implementation libs.mcp.json.jackson2
}
```

### Key Dependencies

| Dependency | Module | Purpose |
|------------|--------|---------|
| `mcp-core` | server | MCP protocol types, sync server |
| `mcp-json-jackson2` | server | Jackson 2.x JSON serialization |
| `spring-boot-starter-web` | client, server | REST client, MVC |
| `jackson-databind` | api | JSON record serialization |
| `jakarta.servlet-api` | server | Streamable HTTP transport |

---

## MCP SDK Integration

### Streamable HTTP Transport

The server uses `HttpServletStreamableServerTransportProvider` on `/mcp` endpoint:

```java
@Bean
public HttpServletStreamableServerTransportProvider transportProvider() {
    return HttpServletStreamableServerTransportProvider.builder()
            .mcpEndpoint("/mcp")
            .build();
}
```

**Request flow:**
1. Client sends JSON-RPC 2.0 to `/mcp`
2. Transport provider routes to `McpSyncServer`
3. Tool handler executes (`handleGetCurrentWeather`, etc.)
4. Response serialized to JSON with `TextContent` array

### Tool Specification Pattern

Each tool follows this pattern:

```java
Map<String, Object> schema = Map.of(
    "type", "object",
    "properties", Map.of("city", Map.of("type", "string")),
    "required", List.of("city")
);

McpServerFeatures.SyncToolSpecification tool = 
    McpServerFeatures.SyncToolSpecification.builder()
        .tool(McpSchema.Tool.builder("tool_name", schema)
            .description("Tool description")
            .build())
        .callHandler((exchange, request) -> handleTool(request))
        .build();

server.addTool(tool);
```

### Dual TextContent Response

All tools return two `TextContent` objects:

```java
List<McpSchema.Content> content = new ArrayList<>();
content.add(new McpSchema.TextContent(humanReadableSummary));  // Localized
content.add(new McpSchema.TextContent(jsonData));              // Structured
return new McpSchema.CallToolResult(content, false, null, null);
```

**Why dual format:**
- First text: AI assistant reads aloud to user
- Second text: AI parses for follow-up reasoning

---

## Common Tasks

### How to Add New MCP Tool

1. **Add DTO in api/** (if needed):
```java
public record HourlyForecast(
    String datetime,
    double temperature,
    double precipitation
) {}
```

2. **Add client method in client/**:
```java
public List<HourlyForecast> getHourlyForecast(String city, int hours) {
    // Call OpenMeteo API
}
```

3. **Register tool in WeatherMcpTools.java**:
```java
Map<String, Object> schema = Map.of(
    "type", "object",
    "properties", Map.of(
        "city", Map.of("type", "string"),
        "hours", Map.of("type", "integer")
    ),
    "required", List.of("city")
);

McpServerFeatures.SyncToolSpecification hourlyTool = 
    McpServerFeatures.SyncToolSpecification.builder()
        .tool(McpSchema.Tool.builder("get_hourly_forecast", schema)
            .description("Get hourly weather forecast")
            .build())
        .callHandler((exchange, request) -> handleGetHourlyForecast(request))
        .build();

server.addTool(hourlyTool);
```

4. **Implement handler**:
```java
private McpSchema.CallToolResult handleGetHourlyForecast(McpSchema.CallToolRequest request) {
    String city = (String) request.arguments().get("city");
    int hours = ((Number) request.arguments().get("hours")).intValue();
    
    List<HourlyForecast> forecast = weatherClient.getHourlyForecast(city, hours);
    
    // Build dual response...
}
```

5. **Add unit test** in `WeatherMcpToolsTest.java`

### How to Modify DTOs

**Adding a field:**
```java
// Before
public record CurrentWeather(double temperature, int humidity) {}

// After
public record CurrentWeather(
    double temperature,
    int humidity,
    double uvIndex  // New field
) {}
```

**Update JSON mapping in client:**
```java
// ForecastResponse.java
@JsonProperty("uv_index") double uvIndex,

// Mapper
uvIndex: response.uvIndex()
```

**Update tool response:**
```java
String jsonData = objectMapper.writeValueAsString(Map.of(
    "temperature", weather.temperature(),
    "humidity", weather.humidity(),
    "uvIndex", weather.uvIndex()  // New field
));
```

### How to Update Dependencies

1. **Edit gradle/libs.versions.toml**:
```toml
[versions]
spring-boot = "3.3.14"  # Update version
```

2. **Refresh Gradle**:
```bash
./gradlew --refresh-dependencies
```

3. **Run tests**:
```bash
./gradlew test
```

4. **Check for conflicts**:
```bash
./gradlew dependencies
```

---

## Testing Strategy

### Test Locations

| Module | Test Path | Coverage |
|--------|-----------|----------|
| api | `api/src/test/java/` | WMO codes, exceptions |
| client | `client/src/test/java/` | HTTP client, retry logic |
| server | `server/src/test/java/` | MCP tools, integration |

### Unit Tests Pattern

**Test class structure:**
```java
@ExtendWith(MockitoExtension.class)
class WeatherMcpToolsTest {
    
    @Mock
    private WeatherClient weatherClient;
    
    @InjectMocks
    private WeatherMcpTools tools;
    
    @Test
    void shouldHandleMissingCityParameter() {
        // Given
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "get_current_weather", Map.of(), null
        );
        
        // When
        McpSchema.CallToolResult result = tools.handleGetCurrentWeather(request);
        
        // Then
        assertThat(result.isError()).isTrue();
        assertThat(result.content().get(0).text())
            .contains("City parameter is required");
    }
}
```

### Integration Tests Pattern

**Mock external API:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class WeatherMcpToolsIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private OpenMeteoClient openMeteoClient;
    
    @Test
    void shouldReturnWeatherOnMcpEndpoint() throws Exception {
        // Given
        when(openMeteoClient.getCurrentWeather("Moscow"))
            .thenReturn(new CurrentWeather(...));
        
        // When & Then
        mockMvc.perform(post("/mcp")
                .contentType("application/json")
                .content(mcpRequestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content").isArray());
    }
}
```

### Mocking OpenMeteo API

**Use MockWebServer for HTTP tests:**
```java
@ExtendWith(MockitoExtension.class)
class OpenMeteoClientTest {
    
    private MockWebServer mockWebServer;
    private OpenMeteoClient client;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        client = new OpenMeteoClient(
            "http://localhost:" + mockWebServer.getPort()
        );
    }
    
    @Test
    void shouldParseWeatherResponse() throws IOException {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"current\": {\"temperature_2m\": 20.5}}"));
        
        // When
        CurrentWeather weather = client.getCurrentWeather("Moscow");
        
        // Then
        assertThat(weather.temperature()).isEqualTo(20.5);
    }
}
```

---

## Deployment

### Docker Build Process

**Multi-stage Dockerfile:**
```dockerfile
# Stage 1: Build with JDK
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY gradle/wrapper gradle/wrapper
COPY gradle/libs.versions.toml gradle/libs.versions.toml
COPY gradlew .
COPY build.gradle . settings.gradle .
COPY api api client client server server
RUN ./gradlew :server:bootJar --no-daemon

# Stage 2: Runtime with JRE only
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=build /app/server/build/libs/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Build commands:**
```bash
# Build image
docker build -t weather-mcp-server:latest .

# Run container
docker run -p 8080:8080 weather-mcp-server:latest

# Or with docker-compose
docker-compose up -d
```

### Environment Configuration

**Docker Compose:**
```yaml
version: '3.8'

services:
  weather-mcp-server:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SERVER_PORT=8080
      - JAVA_OPTS=-Xmx512m
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 3s
      retries: 3
      start_period: 10s
    restart: unless-stopped
```

**Override configuration:**
```bash
# Custom port
docker run -e SERVER_PORT=8081 -p 8081:8081 weather-mcp-server

# Memory limit
docker run -e JAVA_OPTS="-Xmx256m -Xms128m" weather-mcp-server
```

### Health Checks

**Endpoints:**
- `/actuator/health` — Liveness probe (returns `{"status":"UP"}`)
- `/actuator/info` — Application metadata

**Docker healthcheck:**
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 3s
  retries: 3
  start_period: 10s  # Allow JVM warmup
```

**Load balancer config:**
```nginx
location /health {
    proxy_pass http://weather-mcp-server:8080/actuator/health;
    proxy_read_timeout 3s;
}
```

---

## Known Issues & Solutions

### Jackson 2.x/3.x Compatibility

**Problem:**
```
java.lang.ClassNotFoundException: com.fasterxml.jackson.databind.ObjectMapper
```

**Cause:** MCP SDK 2.0.0 requires Jackson 2.x, but Spring Boot 3.x uses Jackson 2.x by default. The `mcp` artifact bundles Jackson 3.x which is incompatible.

**Solution:** Use separate `mcp-core` + `mcp-json-jackson2` artifacts:

```toml
# gradle/libs.versions.toml
[libraries]
mcp-core = { group = "io.modelcontextprotocol.sdk", name = "mcp-core", version.ref = "mcp" }
mcp-json-jackson2 = { group = "io.modelcontextprotocol.sdk", name = "mcp-json-jackson2", version.ref = "mcp" }
```

```gradle
// server/build.gradle
dependencies {
    implementation libs.mcp.core
    implementation libs.mcp.json.jackson2
    // NOT: implementation libs.mcp
}
```

### Port Configuration (8080)

**Problem:**
```
Web server failed to start. Port 8080 was already in use.
```

**Solutions:**

1. **application.yml:**
```yaml
server:
  port: 8081
```

2. **Environment variable:**
```bash
SERVER_PORT=8081 ./gradlew :server:bootRun
```

3. **Docker Compose:**
```yaml
ports:
  - "8081:8080"  # Host 8081 → Container 8080
```

4. **Find and kill process:**
```bash
# Linux/macOS
lsof -ti:8080 | xargs kill -9

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### OpenMeteo Rate Limiting

**Limits:**
- 600 requests/minute (free tier)
- No API key required
- No authentication

**Current mitigation:**
- Retry logic: 3 attempts, 1s delay
- No caching (by design)

**If rate limited:**
```java
// Add exponential backoff in RetryUtils.java
private static final long INITIAL_DELAY_MS = 1000;
private static final double BACKOFF_MULTIPLIER = 2.0;

long delay = (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attempt));
Thread.sleep(delay);
```

**Alternative solutions:**
- Add Redis cache with 10-minute TTL
- Implement request queue with rate limiter
- Use OpenMeteo commercial plan (higher limits)

### Docker Build Issues

**Problem: Gradle wrapper not found**
```
COPY gradlew .
Error: file not found
```

**Solution:** Ensure `gradlew` script is in root:
```bash
ls -la gradlew
chmod +x gradlew
```

**Problem: JAR not found in build stage**
```
COPY --from=build /app/server/build/libs/*.jar app.jar
Error: no source files
```

**Solution:** Check bootJar task:
```bash
./gradlew :server:bootJar --info
```

Ensure `bootJar { enabled = true }` in `server/build.gradle`.

**Problem: Container exits immediately**
```bash
docker-compose logs
# No output
```

**Solution:** Increase JVM memory and health check timeout:
```yaml
environment:
  - JAVA_OPTS=-Xmx256m -Xms128m
healthcheck:
  start_period: 30s  # Increased from 10s
```

---

## Quick Reference

### Build Commands

```bash
# Full build
./gradlew build

# Build specific module
./gradlew :api:build
./gradlew :client:build
./gradlew :server:build

# Run tests
./gradlew test

# Run server
./gradlew :server:bootRun

# Generate JAR
./gradlew :server:bootJar
```

### Test Commands

```bash
# All tests
./gradlew test

# Module tests
./gradlew :api:test
./gradlew :client:test
./gradlew :server:test

# Coverage report
./gradlew jacocoTestReport
```

### Docker Commands

```bash
# Build image
docker build -t weather-mcp-server:latest .

# Run container
docker run -p 8080:8080 weather-mcp-server:latest

# Docker Compose
docker-compose up -d
docker-compose logs -f
docker-compose down
```

### MCP Inspector

```bash
# Connect to server
mcp-inspector --endpoint http://localhost:8080/mcp

# List tools
list-tools

# Call tool
call-tool get_current_weather '{"city": "Moscow"}'
```

---

## File Structure Reference

```
weather-mcp-server/
├── api/
│   ├── src/main/java/io/github/solas/mcp/weather/api/
│   │   ├── CurrentWeather.java      # Current weather DTO
│   │   ├── DailyForecast.java       # Forecast DTO
│   │   ├── Location.java            # Geocoding DTO
│   │   ├── WeatherData.java         # Combined DTO
│   │   ├── WeatherClient.java       # Client interface
│   │   ├── WeatherApiException.java # Custom exception
│   │   └── WmoCode.java             # WMO code mapping
│   └── build.gradle
│
├── client/
│   ├── src/main/java/io/github/solas/mcp/weather/client/
│   │   ├── OpenMeteoClient.java     # HTTP client implementation
│   │   ├── RetryUtils.java          # Retry logic (3 attempts)
│   │   ├── GeocodingResponse.java   # API response mapper
│   │   └── ForecastResponse.java    # API response mapper
│   └── build.gradle
│
├── server/
│   ├── src/main/java/io/github/solas/mcp/weather/server/
│   │   ├── WeatherMcpServerApplication.java  # Main class
│   │   ├── McpServerConfig.java              # MCP configuration
│   │   ├── WeatherMcpTools.java              # Tool implementations
│   │   ├── ClientConfig.java                 # RestClient config
│   │   └── LanguageUtils.java                # Russian detection
│   ├── src/main/resources/
│   │   └── application.yml                   # Spring config
│   └── build.gradle
│
├── gradle/
│   └── libs.versions.toml          # Version catalog
│
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Container orchestration
├── build.gradle                     # Root build config
├── settings.gradle                  # Module inclusion
└── README.md                        # User documentation
```

---

## Decision Log

### Why Multi-Module?

**Decision:** Split into `api`, `client`, `server` modules.

**Rationale:**
- Clear separation of concerns
- `api` can be reused by other projects
- `client` can be tested independently
- Faster builds (only changed modules rebuild)

### Why Dual TextContent?

**Decision:** Return two `TextContent` objects per tool call.

**Rationale:**
- First text: Human-readable summary for AI to read aloud
- Second text: Structured JSON for AI reasoning
- Avoids parsing overhead for simple queries
- Enables multilingual responses

### Why Cyrillic Detection?

**Decision:** Detect Russian by Unicode range `\u0400-\u04FF`.

**Rationale:**
- No external locale libraries needed
- Works for all Cyrillic-based languages
- Simple regex: `.*[\u0400-\u04FF].*`
- Zero runtime overhead

### Why Retry Logic?

**Decision:** 3 attempts, 1s delay on failures.

**Rationale:**
- OpenMeteo occasionally returns 502/503
- No API key = no quota tracking
- Transient failures common in free APIs
- Simple implementation, effective results

### Why Jackson 2.x Separation?

**Decision:** Use `mcp-core` + `mcp-json-jackson2` separately.

**Rationale:**
- MCP SDK 2.0.0 bundles Jackson 3.x in `mcp` artifact
- Spring Boot 3.3.13 uses Jackson 2.17.0
- Class incompatibility causes `ClassNotFoundException`
- Separate artifacts ensure Jackson 2.x usage
