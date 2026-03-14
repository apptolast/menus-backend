package com.apptolast.menus.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppConfig(
    val jwt: JwtProperties = JwtProperties(),
    val cors: CorsProperties = CorsProperties(),
    val google: GoogleProperties = GoogleProperties(),
    val upload: UploadProperties = UploadProperties()
) {
    data class JwtProperties(
        val secret: String = "",
        val accessExpiration: Long = 900,
        val refreshExpiration: Long = 604800
    )

    data class CorsProperties(
        val allowedOrigins: String = "http://localhost:3000"
    )

    data class GoogleProperties(
        val clientId: String = ""
    )

    data class UploadProperties(
        val dir: String = "/data/uploads",
        val maxSizeMb: Long = 5,
        val allowedTypes: List<String> = listOf("image/png", "image/jpeg", "image/webp", "image/svg+xml")
    )
}
