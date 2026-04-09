package com.jcarroll95.tbats;

import com.jcarroll95.tbats.security.JwtUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public CommandLineRunner jwtSmokeTest(JwtUtil jwtUtil) {
		return args -> {
			String token = jwtUtil.generateToken("alice", "ADMIN");
			System.out.println("TOKEN: " + token);
			System.out.println("VALID: " + jwtUtil.validateToken(token));
			System.out.println("USER:  " + jwtUtil.extractUsername(token));
			System.out.println("ROLE:  " + jwtUtil.extractRole(token));
		};
	}

}
