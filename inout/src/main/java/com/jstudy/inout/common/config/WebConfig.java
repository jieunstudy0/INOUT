package com.jstudy.inout.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    	
        String location = uploadPath.endsWith("/") ? uploadPath : uploadPath + "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + location);

        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations("file:" + location);
    }
}