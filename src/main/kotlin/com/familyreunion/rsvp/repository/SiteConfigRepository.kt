package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.SiteConfig
import org.springframework.data.jpa.repository.JpaRepository

interface SiteConfigRepository : JpaRepository<SiteConfig, String>
