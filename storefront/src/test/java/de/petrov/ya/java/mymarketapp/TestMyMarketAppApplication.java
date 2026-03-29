package de.petrov.ya.java.mymarketapp;

import org.springframework.boot.SpringApplication;

public class TestMyMarketAppApplication {

    public static void main(String[] args) {
        SpringApplication.from(MyMarketAppApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
