package com.basilico.backend.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.basilico.backend.common.RestaurantSchedule;

@Configuration
public class ClockConfig {

	@Bean
	public Clock clock() {
		return Clock.system(RestaurantSchedule.RESTAURANT_ZONE);
	}
}
