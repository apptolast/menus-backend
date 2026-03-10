package com.apptolast.menus.auth.service

import com.apptolast.menus.config.AppConfig
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import org.springframework.stereotype.Component

data class GoogleUserInfo(
    val googleId: String,
    val email: String,
    val name: String?
)

@Component
class GoogleTokenVerifier(private val appConfig: AppConfig) {

    fun verify(idTokenString: String): GoogleUserInfo {
        val transport = NetHttpTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        val idToken: GoogleIdToken = GoogleIdToken.parse(jsonFactory, idTokenString)
            ?: throw IllegalArgumentException("Invalid Google ID token format")

        val payload = idToken.payload
        return GoogleUserInfo(
            googleId = payload.subject,
            email = payload.email,
            name = payload["name"] as? String
        )
    }
}
