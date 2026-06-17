package com.megasena.sync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MegaSenaSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(MegaSenaSyncApplication.class, args);
    }
}
