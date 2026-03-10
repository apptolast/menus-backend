package com.apptolast.menus.auth.service

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

data class GoogleUserInfo(
    val googleId: String,
    val email: String,
    val name: String?
)

@Component
class GoogleTokenVerifier(
    @Value("\${spring.security.oauth2.client.registration.google.client-id:placeholder}")
    private val googleClientId: String
) {

    private val log = org.slf4j.LoggerFactory.getLogger(GoogleTokenVerifier::class.java)

    private val verifier: GoogleIdTokenVerifier by lazy {
        val builder = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
        if (googleClientId.isNotBlank() && googleClientId != "placeholder") {
            builder.setAudience(listOf(googleClientId))
        } else {
            log.warn("Google OAuth client ID is not configured — audience validation is DISABLED. Set GOOGLE_CLIENT_ID in production.")
        }
        builder.build()
    }

    fun verify(idTokenString: String): GoogleUserInfo {
        val idToken = verifier.verify(idTokenString)
            ?: throw IllegalArgumentException("Invalid or expired Google ID token")

        val payload = idToken.payload
        return GoogleUserInfo(
            googleId = payload.subject,
            email = payload.email,
            name = payload["name"] as? String
        )
    }
}
