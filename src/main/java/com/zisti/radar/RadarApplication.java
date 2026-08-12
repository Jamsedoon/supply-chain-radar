package com.zisti.radar;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class RadarApplication {

	private static final Logger log = LoggerFactory.getLogger(RadarApplication.class);

	@Value("${radar.role}")
	private String role;

	public static void main(String[] args) {
		SpringApplication.run(RadarApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void announceRole() {
		log.info("=== supply-chain-radar started in role: {} ===", role);
	}
}