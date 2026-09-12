package com.nudge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Nudge REST API.
 *
 * <p>Run it with: {@code mvn spring-boot:run}</p>
 */
@SpringBootApplication
public class NudgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NudgeApplication.class, args);
    }
}
