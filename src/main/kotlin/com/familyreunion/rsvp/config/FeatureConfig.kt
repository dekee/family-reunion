package com.familyreunion.rsvp.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Runtime feature switches. Each is an env var so a feature can be turned off or back on with a
 * pod restart rather than a rebuild (see application.properties for the variable names).
 */
@Configuration
@ConfigurationProperties(prefix = "app.features")
class FeatureConfig {
    /**
     * Whether the Give page offers T-shirts as an add-on. Off hides the option in the UI and makes
     * the server refuse a contribution that asks for one, so a stale tab cannot buy a shirt either.
     */
    var donationShirts: Boolean = false
}
