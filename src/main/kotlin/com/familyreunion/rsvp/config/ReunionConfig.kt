package com.familyreunion.rsvp.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.reunion")
class ReunionConfig {
    var familyName: String = "Tumblin"
    var fullTitle: String = "Tumblin Family Reunion"
    var corsOrigins: List<String> = listOf("http://localhost:5173")
}
