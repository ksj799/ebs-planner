package com.planner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 외부 안내 페이지(예: GitHub Pages)가 서버 기동 여부를 확인할 수 있도록
 * 상태 확인 API(/api/info, GET)만 지정한 출처에 열어 둔다. planner.cors-origins 가 비어 있으면 아무것도 열지 않는다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String[] origins;

    public WebConfig(@Value("${planner.cors-origins:}") String[] origins) {
        this.origins = origins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (origins.length == 0 || (origins.length == 1 && origins[0].trim().isEmpty())) return;
        registry.addMapping("/api/info").allowedOrigins(origins).allowedMethods("GET");
    }
}
