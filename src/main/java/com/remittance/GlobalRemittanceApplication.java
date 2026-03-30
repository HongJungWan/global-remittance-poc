package com.remittance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GlobalRemittanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlobalRemittanceApplication.class, args);
    }
}
