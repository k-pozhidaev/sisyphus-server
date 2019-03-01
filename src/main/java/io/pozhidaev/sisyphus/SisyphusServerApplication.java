package io.pozhidaev.sisyphus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("io.pozhidaev.sisyphus.domain")
public class SisyphusServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SisyphusServerApplication.class, args);
	}

}

