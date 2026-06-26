# Weather MCP Server

A Model Context Protocol (MCP) server that provides weather data tools for AI assistants. Built with Spring Boot 3.3.13, Java 17, and the official MCP SDK 2.0.0 with Streamable HTTP transport.

## Overview

Weather MCP Server exposes three MCP tools that enable AI assistants to retrieve real-time weather data from the OpenMeteo API:

- **get_current_weather** — Current conditions for any city
- **get_forecast** — Multi-day weather forecast (1-16 days)
- **search_city** — City lookup with geocoding

The server responds with dual TextContent: a human-readable summary and structured JSON data. Language detection uses Cyrillic character recognition to automatically respond in Russian when appropriate.

## Architecture

The project follows a multi-module Gradle structure with clear separation of concerns:

```
weather-mcp-server/
├── api/      # Shared DTOs, interfaces, WMO mapping, exceptions
├── client/   # OpenMeteo HTTP client with retry logic
└── server/   # MCP server, Spring Boot application, tool implementations
```

**Module Dependencies:**
- `server` → `api` + `client`
- `client` → `api`
- `api` → no internal dependencies

**Key Components:**

| Component | Description |
|-----------|-------------|
| Streamable HTTP Transport | MCP SDK 2.0.0 on `/mcp` endpoint |
| OpenMeteo Integration | Geocoding + Forecast APIs, no API key required |
| Retry Logic | 3 attempts, 1-second delay on transient failures |
| WMO Code Mapping | Standard codes 0-99 with descriptions |
| Russian Detection | Cyrillic range `\u0400-\u04FF` in city name |
| Dual Response | Human-readable text + raw JSON |

## Prerequisites

- **Java 17** or later (Eclipse Temurin recommended)
- **Gradle 8.7** (wrapper included in project)
- **Docker** and **Docker Compose** (optional, for containerized deployment)

## Quick Start

### Build

```bash
# Full build with tests
./gradlew build

# Build server module only
./gradlew :server:build
```

### Run Locally

```bash
./gradlew :server:bootRun
```

Server starts on `http://localhost:8080` with MCP endpoint at `/mcp`.

### Docker Deployment

```bash
# Build image
docker build -t weather-mcp-server:latest .

# Run with docker-compose
docker-compose up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

Container exposes port 8080 with health check at `/actuator/health`.

## MCP Tools

### `get_current_weather(city: String)`

Retrieves current weather conditions for a specified city.

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| city | String | City name (e.g., "Moscow", "Москва", "Санкт-Петербург") |

**Response:**
```typescript
{
  text1: "Current weather in Moscow: 20°C, Clear sky",
  text2: {
    "temperature": 20.5,
    "feelsLike": 19.2,
    "humidity": 65,
    "pressure": 1013,
    "windSpeed": 5.2,
    "windDirection": "NW",
    "weatherCode": 0,
    "description": "Clear sky"
  }
}
```

**Example:**
```bash
# Via MCP Inspector or compatible client
get_current_weather({ city: "Moscow" })
```

### `get_forecast(city: String, days: Integer)`

Retrieves weather forecast for a specified city.

**Parameters:**
| Name | Type | Description | Constraints |
|------|------|-------------|-------------|
| city | String | City name | — |
| days | Integer | Number of days | 1-16 (default: 7) |

**Response:**
```typescript
{
  text1: "7-day forecast for Moscow",
  text2: [
    {
      "date": "2025-06-25",
      "temperatureMax": 22.5,
      "temperatureMin": 14.2,
      "precipitationSum": 0.0,
      "weatherCode": 1,
      "description": "Mainly clear"
    },
    // ... more days
  ]
}
```

**Validation:**
- `days < 1` or `days > 16` → throws `IllegalArgumentException`

**Example:**
```bash
get_forecast({ city: "Москва", days: 5 })
```

### `search_city(query: String)`

Searches for cities matching a query string using OpenMeteo geocoding.

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| query | String | City name or partial match |

**Response:**
```typescript
{
  text1: "Found 3 cities: Moscow (RU), Moscow (US), Moscow (ID)",
  text2: [
    {
      "name": "Moscow",
      "country": "RU",
      "latitude": 55.7522,
      "longitude": 37.6156,
      "timezone": "Europe/Moscow",
      "elevation": 156
    },
    // ... more results
  ]
}
```

**Example:**
```bash
search_city({ query: "Санкт" })
```

## Configuration

### application.yml

```yaml
server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| SERVER_PORT | 8080 | HTTP server port |
| JAVA_OPTS | — | JVM options (e.g., `-Xmx512m`) |

### MCP Endpoint

- **URL:** `http://localhost:8080/mcp`
- **Transport:** Streamable HTTP (MCP SDK 2.0.0)
- **Capabilities:** `tools: true`

## Docker Deployment

### Dockerfile

Multi-stage build for minimal runtime image:

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
# ... gradle build steps

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=build /app/server/build/libs/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

```yaml
version: '3.8'

services:
  weather-mcp-server:
    build: .
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 3s
      retries: 3
      start_period: 10s
    restart: unless-stopped
```

**Health Check:**
- Endpoint: `/actuator/health`
- Interval: 30 seconds
- Timeout: 3 seconds
- Retries: 3

## Testing

### Run All Tests

```bash
./gradlew test
```

### Run Module-Specific Tests

```bash
./gradlew :api:test
./gradlew :client:test
./gradlew :server:test
```

### Test Reports

HTML reports available at:
- `api/build/reports/tests/test/index.html`
- `client/build/reports/tests/test/index.html`
- `server/build/reports/tests/test/index.html`

### MCP Inspector

Verify tools with MCP Inspector CLI:

```bash
# Connect to server
mcp-inspector --endpoint http://localhost:8080/mcp

# List available tools
list-tools

# Invoke tool
call-tool get_current_weather '{"city": "Moscow"}'
```

## Troubleshooting

### Jackson Compatibility

**Problem:** `ClassNotFoundException: com.fasterxml.jackson.databind.ObjectMapper`

**Cause:** MCP SDK 2.0.0 requires Jackson 2.x modules explicitly.

**Solution:** Use `mcp-core` + `mcp-json-jackson2` instead of `mcp` artifact:

```toml
# gradle/libs.versions.toml
[versions]
mcp = "2.0.0"

[libraries]
mcp-core = { group = "io.modelcontextprotocol.sdk", name = "mcp-core", version.ref = "mcp" }
mcp-json-jackson2 = { group = "io.modelcontextprotocol.sdk", name = "mcp-json-jackson2", version.ref = "mcp" }
```

```gradle
// server/build.gradle
dependencies {
    implementation libs.mcp.core
    implementation libs.mcp.json.jackson2
}
```

### Port Conflicts

**Problem:** `Port 8080 is already in use`

**Solutions:**

1. Change port in `application.yml`:
   ```yaml
   server:
     port: 8081
   ```

2. Override via environment:
   ```bash
   SERVER_PORT=8081 ./gradlew :server:bootRun
   ```

3. Find and kill process:
   ```bash
   lsof -ti:8080 | xargs kill -9  # Linux/macOS
   netstat -ano | findstr :8080  # Windows
   ```

### OpenMeteo API Limits

**Problem:** HTTP 429 Too Many Requests

**Details:** OpenMeteo free tier allows 600 requests/minute.

**Mitigation:**
- Retry logic handles transient failures (3 attempts, 1s delay)
- No caching layer (by design)
- Consider rate limiting at client level if needed

### Unknown WMO Codes

**Behavior:** Unknown weather codes return `"WMO code: XX"` format.

**Example:** `WmoCode.getDescription(127)` → `"WMO code: 127"`

### Docker Container Fails to Start

**Check logs:**
```bash
docker-compose logs weather-mcp-server
```

**Common issues:**
- Port 8080 already bound on host
- Health check timeout too short (increase `start_period`)
- JVM memory limits (set `JAVA_OPTS: "-Xmx512m"` in compose)

## API Reference

### MCP Protocol Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/mcp` | POST | Streamable HTTP transport for MCP messages |
| `/actuator/health` | GET | Health check (Docker, load balancers) |
| `/actuator/info` | GET | Application info |

### Tool Input Schemas

**get_current_weather:**
```json
{
  "type": "object",
  "properties": {
    "city": { "type": "string" }
  },
  "required": ["city"]
}
```

**get_forecast:**
```json
{
  "type": "object",
  "properties": {
    "city": { "type": "string" },
    "days": { "type": "integer", "minimum": 1, "maximum": 16 }
  },
  "required": ["city"]
}
```

**search_city:**
```json
{
  "type": "object",
  "properties": {
    "query": { "type": "string" }
  },
  "required": ["query"]
}
```

### Response Format

All tools return `CallToolResult` with `content` array containing two `TextContent` objects:

1. **Human-readable summary** — Localized text (Russian if city contains Cyrillic)
2. **Structured JSON** — Raw data from OpenMeteo API

## License

MIT License

## Contributing

1. Fork the repository
2. Create a feature branch
3. Run `./gradlew build` to ensure tests pass
4. Submit a pull request
