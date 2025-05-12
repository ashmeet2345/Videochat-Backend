package com.videochat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration
public class VideochatApplication {

	public static void main(String[] args) {
		SpringApplication.run(VideochatApplication.class, args);
	}

}
