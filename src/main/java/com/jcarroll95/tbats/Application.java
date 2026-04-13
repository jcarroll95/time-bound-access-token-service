package com.jcarroll95.tbats;

import com.jcarroll95.tbats.security.JwtProperties;
import com.jcarroll95.tbats.security.JwtUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(JwtProperties.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);



	}

}
