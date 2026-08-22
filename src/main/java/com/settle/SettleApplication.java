package com.settle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class SettleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SettleApplication.class, args);
	}

}
