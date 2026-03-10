package com.apptolast.menus.allergen

import com.apptolast.menus.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

@DisplayName("Allergen API — Integration Tests")
class AllergenIntegrationTest : AbstractIntegrationTest() {

    @Test
    @DisplayName("GET /api/v1/allergens returns 200 with 14 allergens")
    fun getAllergensReturns14() {
        val response = restClient()
            .get()
            .uri("/api/v1/allergens")
            .retrieve()
            .toEntity(List::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.size).isEqualTo(14)
    }
}
