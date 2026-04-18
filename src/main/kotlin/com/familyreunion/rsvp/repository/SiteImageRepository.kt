package com.familyreunion.rsvp.repository

import com.familyreunion.rsvp.model.SiteImage
import org.springframework.data.jpa.repository.JpaRepository

interface SiteImageRepository : JpaRepository<SiteImage, String>
