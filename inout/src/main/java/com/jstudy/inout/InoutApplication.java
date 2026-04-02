package com.jstudy.inout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class InoutApplication {

	public static void main(String[] args) {
		SpringApplication.run(InoutApplication.class, args);
	}

}
