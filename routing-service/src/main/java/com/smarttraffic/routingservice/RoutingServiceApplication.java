package com.smarttraffic.routingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class RoutingServiceApplication {

    public static void main(String[] args) {
        // Same workaround as the other services - postgres:16's Docker image
        // doesn't recognize the deprecated "Asia/Calcutta" alias this JVM
        // reports by default.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        SpringApplication.run(RoutingServiceApplication.class, args);
    }

}