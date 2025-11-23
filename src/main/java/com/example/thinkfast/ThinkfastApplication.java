package com.example.thinkfast;

import com.example.thinkfast.common.config.WebClientConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
@EntityScan("com.example.thinkfast.domain")
@ComponentScan(basePackages = "com.example.thinkfast")
@Import(WebClientConfig.class)
public class ThinkfastApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThinkfastApplication.class, args);
		System.setProperty("file.encoding","UTF-8");
	}

}
