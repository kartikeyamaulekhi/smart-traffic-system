    package com.smarttraffic.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        // This JVM/Windows locale reports "Asia/Calcutta" - a deprecated legacy
        // alias - which the postgres:16 Docker image's timezone data doesn't
        // recognize, causing every connection to fail at the handshake stage.
        // Force the modern IANA name before anything else initializes.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
