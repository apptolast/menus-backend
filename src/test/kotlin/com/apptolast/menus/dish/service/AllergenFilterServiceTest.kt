package com.apptolast.menus.dish.service

import com.apptolast.menus.dish.dto.DishAllergenData
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import com.apptolast.menus.dish.model.enum.SafetyLevel
import com.apptolast.menus.dish.service.impl.AllergenFilterServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AllergenFilterService — Semaforo Algorithm")
class AllergenFilterServiceTest {

    private lateinit var service: AllergenFilterService

    @BeforeEach
    fun setUp() {
        service = AllergenFilterServiceImpl()
    }

    private fun makeDishAllergen(allergenCode: String, level: ContainmentLevel) =
        DishAllergenData(allergenCode = allergenCode, containmentLevel = level)

    @Nested
    @DisplayName("computeSafetyLevel")
    inner class ComputeSafetyLevelTests {

        @Test
        @DisplayName("SAFE when user has no allergens (empty profile)")
        fun safeWhenEmptyProfile() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.CONTAINS)
            )
            val userCodes = emptyList<String>()
            assertThat(service.computeSafetyLevel(dishAllergens, userCodes)).isEqualTo(SafetyLevel.SAFE)
        }

        @Test
        @DisplayName("SAFE when dish has no allergens")
        fun safeWhenDishHasNoAllergens() {
            val userCodes = listOf("GLUTEN", "MILK")
            assertThat(service.computeSafetyLevel(emptyList(), userCodes)).isEqualTo(SafetyLevel.SAFE)
        }

        @Test
        @DisplayName("SAFE when no user allergens match dish allergens")
        fun safeWhenNoMatch() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.CONTAINS)
            )
            val userCodes = listOf("EGGS", "MILK")
            assertThat(service.computeSafetyLevel(dishAllergens, userCodes)).isEqualTo(SafetyLevel.SAFE)
        }

        @Test
        @DisplayName("RISK when user allergen is MAY_CONTAIN in dish")
        fun riskWhenMayContain() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.MAY_CONTAIN)
            )
            val userCodes = listOf("GLUTEN")
            assertThat(service.computeSafetyLevel(dishAllergens, userCodes)).isEqualTo(SafetyLevel.RISK)
        }

        @Test
        @DisplayName("DANGER when user allergen is CONTAINS in dish")
        fun dangerWhenContains() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.CONTAINS)
            )
            val userCodes = listOf("GLUTEN")
            assertThat(service.computeSafetyLevel(dishAllergens, userCodes)).isEqualTo(SafetyLevel.DANGER)
        }

        @Test
        @DisplayName("DANGER takes priority over RISK when both allergens present")
        fun dangerPriorityOverRisk() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.CONTAINS),
                makeDishAllergen("MILK", ContainmentLevel.MAY_CONTAIN)
            )
            val userCodes = listOf("GLUTEN", "MILK")
            assertThat(service.computeSafetyLevel(dishAllergens, userCodes)).isEqualTo(SafetyLevel.DANGER)
        }

        @Test
        @DisplayName("SAFE when dish has allergen that user does not have")
        fun safeWhenDishAllergenNotInUserProfile() {
            val dishAllergens = listOf(
                makeDishAllergen("CRUSTACEANS", ContainmentLevel.CONTAINS),
                makeDishAllergen("FISH", ContainmentLevel.CONTAINS)
            )
            val userCodes = listOf("MILK", "EGGS")
            assertThat(service.computeSafetyLevel(dishAllergens, userCodes)).isEqualTo(SafetyLevel.SAFE)
        }

        @Test
        @DisplayName("RISK when only one of multiple user allergens is MAY_CONTAIN")
        fun riskWhenOneOfMultipleIsMayContain() {
            val dishAllergens = listOf(
                makeDishAllergen("MILK", ContainmentLevel.MAY_CONTAIN)
            )
            val userCodes = listOf("GLUTEN", "MILK", "EGGS")
            assertThat(service.computeSafetyLevel(dishAllergens, userCodes)).isEqualTo(SafetyLevel.RISK)
        }
    }

    @Nested
    @DisplayName("getMatchedAllergens")
    inner class GetMatchedAllergensTests {

        @Test
        @DisplayName("returns codes of allergens that match user profile at CONTAINS level")
        fun returnsMatchedContains() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.CONTAINS),
                makeDishAllergen("EGGS", ContainmentLevel.CONTAINS)
            )
            val userCodes = listOf("GLUTEN")
            val matched = service.getMatchedAllergens(dishAllergens, userCodes)
            assertThat(matched).containsExactly("GLUTEN")
        }

        @Test
        @DisplayName("returns codes of allergens that match at MAY_CONTAIN level")
        fun returnsMatchedMayContain() {
            val dishAllergens = listOf(
                makeDishAllergen("MILK", ContainmentLevel.MAY_CONTAIN)
            )
            val userCodes = listOf("MILK")
            val matched = service.getMatchedAllergens(dishAllergens, userCodes)
            assertThat(matched).containsExactly("MILK")
        }

        @Test
        @DisplayName("returns empty list when no allergens match")
        fun emptyWhenNoMatch() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.CONTAINS)
            )
            val userCodes = listOf("EGGS")
            assertThat(service.getMatchedAllergens(dishAllergens, userCodes)).isEmpty()
        }

        @Test
        @DisplayName("returns empty list when user has no allergen profile")
        fun emptyWhenEmptyUserProfile() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.CONTAINS)
            )
            assertThat(service.getMatchedAllergens(dishAllergens, emptyList())).isEmpty()
        }

        @Test
        @DisplayName("returns multiple matched allergen codes")
        fun returnsMultipleMatched() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.CONTAINS),
                makeDishAllergen("MILK", ContainmentLevel.MAY_CONTAIN),
                makeDishAllergen("EGGS", ContainmentLevel.CONTAINS)
            )
            val userCodes = listOf("GLUTEN", "MILK")
            val matched = service.getMatchedAllergens(dishAllergens, userCodes)
            assertThat(matched).containsExactlyInAnyOrder("GLUTEN", "MILK")
        }
    }
}
