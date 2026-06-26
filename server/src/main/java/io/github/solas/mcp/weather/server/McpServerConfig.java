package io.github.solas.mcp.weather.server;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public HttpServletStreamableServerTransportProvider streamableHttpTransport() {
        return HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint("/mcp")
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletStreamableServerTransportProvider> streamableHttpServlet(
            HttpServletStreamableServerTransportProvider transportProvider) {
        return new ServletRegistrationBean<>(transportProvider, "/mcp/*");
    }

    @Bean
    public HttpServletSseServerTransportProvider sseTransportProvider() {
        return HttpServletSseServerTransportProvider.builder()
                .messageEndpoint("/message")
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletSseServerTransportProvider> sseServlet(
            HttpServletSseServerTransportProvider sseTransportProvider) {
        return new ServletRegistrationBean<>(sseTransportProvider, "/sse/*");
    }

    @Bean
    public McpSyncServer mcpSyncServer(
            HttpServletStreamableServerTransportProvider streamableHttpTransport,
            WeatherMcpTools weatherMcpTools) {
        
        McpSyncServer server = McpServer.sync(streamableHttpTransport)
                .serverInfo("weather-mcp-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .build();
        
        weatherMcpTools.registerTools(server);
        
        return server;
    }

    @Bean
    public McpSyncServer mcpSseSyncServer(
            HttpServletSseServerTransportProvider sseTransportProvider,
            WeatherMcpTools weatherMcpTools) {
        
        McpSyncServer server = McpServer.sync(sseTransportProvider)
                .serverInfo("weather-mcp-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .build();
        
        weatherMcpTools.registerTools(server);
        
        return server;
    }
}
