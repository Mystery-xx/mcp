package io.github.solas.mcp.weather.server;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public HttpServletStatelessServerTransport statelessTransport() {
        return HttpServletStatelessServerTransport.builder()
                .messageEndpoint("/mcp")
                .build();
    }

    @Bean
    public ServletRegistrationBean<?> mcpServletRegistration(HttpServletStatelessServerTransport transport) {
        return new ServletRegistrationBean<>(transport, "/mcp");
    }

    @Bean
    public McpStatelessSyncServer mcpStatelessSyncServer(
            HttpServletStatelessServerTransport transport,
            WeatherMcpTools weatherMcpTools) {
        
        McpStatelessSyncServer server = McpServer.sync(transport)
                .serverInfo("weather-mcp-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .build();
        
        weatherMcpTools.registerTools(server);
        
        return server;
    }
}
