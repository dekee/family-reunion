package com.familyreunion.rsvp.config

import com.familyreunion.rsvp.model.AgeGroup
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.fees")
class FeeConfig {
    var adult: Long = 10000
    var spouse: Long = 10000
    var child: Long = 5000
    var infant: Long = 1500

    fun feeForAgeGroup(ageGroup: AgeGroup): Long = when (ageGroup) {
        AgeGroup.ADULT -> adult
        AgeGroup.SPOUSE -> spouse
        AgeGroup.CHILD -> child
        AgeGroup.INFANT -> infant
    }
}
