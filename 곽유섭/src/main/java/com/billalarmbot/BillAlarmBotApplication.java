package com.billalarmbot;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BillAlarmBotApplication {

    public static void main(String[] args) {
        Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load()
                .entries()
                .forEach(e -> System.setProperty(e.getKey(), e.getValue()));

        SpringApplication.run(BillAlarmBotApplication.class, args);
    }

}
