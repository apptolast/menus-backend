package com.apptolast.menus.auth.service

import com.apptolast.menus.config.AppConfig
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
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

    private val verifier: GoogleIdTokenVerifier by lazy {
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(listOf(appConfig.google.clientId))
            .build()
    }

    fun verify(idTokenString: String): GoogleUserInfo {
        val idToken = verifier.verify(idTokenString)
            ?: throw IllegalArgumentException("Invalid Google ID token: signature, issuer, or audience check failed")

        val payload = idToken.payload
        return GoogleUserInfo(
            googleId = payload.subject,
            email = payload.email,
            name = payload["name"] as? String
        )
    }
}
