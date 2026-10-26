package com.planner;

import com.planner.config.PlannerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PlannerProperties.class)
public class PlannerApplication {
    public static void main(String[] args) throws Exception {
        // 사용자 관리: java -jar ebs-planner.jar add-user <ID> / remove-user <ID> (서버는 띄우지 않음)
        if (args.length > 0 && ("add-user".equals(args[0]) || "remove-user".equals(args[0]))) {
            com.planner.auth.UserFile.cli(args);
            return;
        }
        SpringApplication.run(PlannerApplication.class, args);
    }
}
