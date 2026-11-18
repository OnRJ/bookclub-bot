
package com.bookclub.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig(
    @Value("\${app.frontend.url:http://localhost:5173}")
    private val frontendUrl: String
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins(frontendUrl)
            .allowedMethods("POST", "OPTIONS")
            .allowedHeaders("Content-Type")
            .allowCredentials(false)
            .maxAge(3600)
    }
}
