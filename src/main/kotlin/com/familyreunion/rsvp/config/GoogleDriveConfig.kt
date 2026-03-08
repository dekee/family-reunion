package com.familyreunion.rsvp.config

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

@Configuration
@ConditionalOnProperty("google.drive.credentials-file")
class GoogleDriveConfig(
    @Value("\${google.drive.credentials-file}") private val credentialsFile: String
) {

    @Bean
    fun driveService(): Drive {
        val credentials = GoogleCredentials
            .fromStream(FileInputStream(credentialsFile))
            .createScoped(listOf(DriveScopes.DRIVE_READONLY))

        return Drive.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials)
        )
            .setApplicationName("Family Reunion")
            .build()
    }
}
