package com.familyreunion.rsvp.security

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

data class GoogleUserInfo(val email: String, val name: String)

@Service
class GoogleTokenVerifier(
    @Value("\${google.client-id:}") private val clientId: String,
    @Value("\${google.ios-client-id:}") private val iosClientId: String
) {

    private val verifier: GoogleIdTokenVerifier by lazy {
        val audiences = listOfNotNull(
            clientId.ifBlank { null },
            iosClientId.ifBlank { null }
        )
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(audiences)
            .build()
    }

    fun verify(idTokenString: String): GoogleUserInfo? {
        if (clientId.isBlank() && iosClientId.isBlank()) return null
        return try {
            val idToken = verifier.verify(idTokenString) ?: return null
            val payload = idToken.payload
            GoogleUserInfo(
                email = payload.email,
                name = payload["name"] as? String ?: payload.email
            )
        } catch (e: Exception) {
            null
        }
    }
}
