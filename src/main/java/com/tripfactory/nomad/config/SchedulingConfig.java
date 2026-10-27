package com.tripfactory.nomad.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled jobs (currently the trip reminders). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
