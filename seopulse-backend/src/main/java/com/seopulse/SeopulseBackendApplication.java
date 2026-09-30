package com.seopulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableScheduling
@SpringBootApplication
public class SeopulseBackendApplication {

    public static void main(String[] args) {
        // The Postgres driver sends the JVM zone on connect, and Postgres
        // rejects legacy zone IDs such as Asia/Calcutta.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(SeopulseBackendApplication.class, args);
    }

}
