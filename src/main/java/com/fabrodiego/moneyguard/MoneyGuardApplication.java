package com.fabrodiego.moneyguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MoneyGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoneyGuardApplication.class, args);
    }
}
