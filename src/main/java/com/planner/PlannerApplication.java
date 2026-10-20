package com.planner;

import com.planner.config.PlannerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PlannerProperties.class)
public class PlannerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlannerApplication.class, args);
    }
}
