package com.familyreunion.rsvp.config

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.UserCredentials
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

@Configuration
@ConditionalOnProperty("google.drive.credentials-file")
class GoogleDriveConfig(
    @Value("\${google.drive.credentials-file}") private val credentialsFile: String,
    @Value("\${google.drive.oauth.client-id:}") private val oauthClientId: String,
    @Value("\${google.drive.oauth.client-secret:}") private val oauthClientSecret: String,
    @Value("\${google.drive.oauth.refresh-token:}") private val oauthRefreshToken: String
) {

    @Bean
    fun driveService(): Drive {
        // Service accounts have no Drive storage quota, so they can list/read the gallery
        // folder but cannot upload into it. When an OAuth refresh token for the folder
        // owner's account is configured, use it instead — uploads are then owned by (and
        // count against the quota of) that account.
        val credentials = if (oauthRefreshToken.isNotBlank()) {
            UserCredentials.newBuilder()
                .setClientId(oauthClientId)
                .setClientSecret(oauthClientSecret)
                .setRefreshToken(oauthRefreshToken)
                .build()
        } else {
            GoogleCredentials
                .fromStream(FileInputStream(credentialsFile))
                .createScoped(listOf(DriveScopes.DRIVE))
        }

        return Drive.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials)
        )
            .setApplicationName("Family Reunion")
            .build()
    }
}
