package com.example;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableCaching

public class RadiswithpaginationApplication {
	public static void main(String[] args) throws Exception{
		SpringApplication.run(RadiswithpaginationApplication.class, args);
	}
}