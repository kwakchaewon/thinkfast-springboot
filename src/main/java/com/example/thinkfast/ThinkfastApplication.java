package com.example.thinkfast;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ThinkfastApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThinkfastApplication.class, args);
		System.setProperty("file.encoding","UTF-8");
	}

}
