package com.smarttraffic.trafficservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class TrafficServiceApplication {

    public static void main(String[] args) {
        // Same workaround as the other services - postgres:16's Docker image
        // doesn't recognize the deprecated "Asia/Calcutta" alias this JVM
        // reports by default.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        SpringApplication.run(TrafficServiceApplication.class, args);
    }

}