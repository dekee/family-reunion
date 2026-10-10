package com.familyreunion.rsvp.config

import com.familyreunion.rsvp.service.GalleryService
import com.google.api.services.drive.Drive
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** One [GalleryService] per photo album, each reading its own Drive folder. */
@Configuration
@ConditionalOnProperty("google.drive.credentials-file")
class GalleryConfig {

    @Bean
    fun galleryService(
        drive: Drive,
        @Value("\${google.drive.folder-id}") folderId: String
    ) = GalleryService(drive, folderId, "/api/gallery")

    // Memorial album is optional: off until its folder id is configured.
    @Bean
    @ConditionalOnExpression("!'\${google.drive.memorial-folder-id:}'.isBlank()")
    fun memorialGalleryService(
        drive: Drive,
        @Value("\${google.drive.memorial-folder-id}") folderId: String
    ) = GalleryService(drive, folderId, MEMORIAL_API_BASE)

    companion object {
        const val MEMORIAL_API_BASE = "/api/gallery/memorial"
    }
}
