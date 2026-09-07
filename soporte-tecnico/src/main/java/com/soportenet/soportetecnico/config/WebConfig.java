package com.soportenet.soportetecnico.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sirve la carpeta frontend/ directamente desde el backend, en
 * http://localhost:8080/frontend/..., para que los links de correo
 * (activacion, recuperacion) funcionen siempre igual sin depender de
 * Live Server, IntelliJ u otro servidor aparte.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.frontend.dir}")
    private String frontendDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/frontend/**")
                .addResourceLocations("file:" + frontendDir + "/");
    }
}
