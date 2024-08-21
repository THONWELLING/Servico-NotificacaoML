package com.ambarx.notificacoesML;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.sql.SQLException;

@EnableScheduling
@EnableJpaRepositories(basePackages = "com/ambarx/notificacoesML/repositories")
@SpringBootApplication
public class StartUp {

	public static void main(String[] args) throws SQLException {
		System.setProperty("jakarta.net.debug", "all");
		SpringApplication.run(StartUp.class, args);
		System.out.println("Server Is Running At The Port: 80");
		System.out.println("Happy Hacking !!!");
	}

}