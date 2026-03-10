package com.apptolast.menus.dish.service

import com.apptolast.menus.allergen.model.entity.Allergen
import com.apptolast.menus.dish.model.entity.DishAllergen
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import com.apptolast.menus.dish.model.enum.SafetyLevel
import com.apptolast.menus.dish.service.impl.AllergenFilterServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AllergenFilterService — Semáforo Algorithm")
class AllergenFilterServiceTest {

    private lateinit var service: AllergenFilterService

    @BeforeEach
    fun setUp() {
        service = AllergenFilterServiceImpl()
    }

    private fun makeAllergen(code: String, id: Int = 1) = Allergen(id = id, code = code)

    private fun makeDishAllergen(allergenCode: String, level: ContainmentLevel, allergenId: Int = 1): DishAllergen {
        val allergen = makeAllergen(allergenCode, allergenId)
        return DishAllergen(
            allergen = allergen,
            containmentLevel = level,
            tenantId = java.util.UUID.randomUUID()
        )
    }

    @Nested
    @DisplayName("computeSafetyLevel")
    inner class ComputeSafetyLevelTests {

        @Test
        @DisplayName("SAFE when no user allergens match dish allergens")
        fun safeWhenNoMatch() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.CONTAINS, 1)
            )
            val userCodes = listOf("EGGS", "MILK")
            assertThat(service.computeSafetyLevel(dishAllergens, userCodes)).isEqualTo(SafetyLevel.SAFE)
        }

        @Test
        @DisplayName("SAFE when dish only has FREE_OF for user allergens")
        fun safeWhenFreeOf() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.FREE_OF, 1)
            )
            val userCodes = listOf("GLUTEN")
            assertThat(service.computeSafetyLevel(dishAllergens, userCodes)).isEqualTo(SafetyLevel.SAFE)
        }

        @Test
        @DisplayName("RISK when user allergen is MAY_CONTAIN")
        fun riskWhenMayContain() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.MAY_CONTAIN, 1)
            )
            val userCodes = listOf("GLUTEN")
            assertThat(service.computeSafetyLevel(dishAllergens, userCodes)).isEqualTo(SafetyLevel.RISK)
        }

        @Test
        @DisplayName("DANGER when user allergen is CONTAINS")
        fun dangerWhenContains() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.CONTAINS, 1)
            )
            val userCodes = listOf("GLUTEN")
            assertThat(service.computeSafetyLevel(dishAllergens, userCodes)).isEqualTo(SafetyLevel.DANGER)
        }

        @Test
        @DisplayName("DANGER takes priority over RISK when both present")
        fun dangerPriorityOverRisk() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.CONTAINS, 1),
                makeDishAllergen("MILK", ContainmentLevel.MAY_CONTAIN, 2)
            )
            val userCodes = listOf("GLUTEN", "MILK")
            assertThat(service.computeSafetyLevel(dishAllergens, userCodes)).isEqualTo(SafetyLevel.DANGER)
        }

        @Test
        @DisplayName("SAFE when user has no allergens (empty profile)")
        fun safeWhenEmptyProfile() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.CONTAINS, 1)
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
    }

    @Nested
    @DisplayName("getMatchedAllergens")
    inner class GetMatchedAllergensTests {

        @Test
        @DisplayName("Returns codes of allergens that match user profile at CONTAINS level")
        fun returnsMatchedContains() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.CONTAINS, 1),
                makeDishAllergen("EGGS", ContainmentLevel.CONTAINS, 3)
            )
            val userCodes = listOf("GLUTEN")
            val matched = service.getMatchedAllergens(dishAllergens, userCodes)
            assertThat(matched).containsExactly("GLUTEN")
        }

        @Test
        @DisplayName("Returns codes of allergens that match at MAY_CONTAIN level")
        fun returnsMatchedMayContain() {
            val dishAllergens = listOf(
                makeDishAllergen("MILK", ContainmentLevel.MAY_CONTAIN, 7)
            )
            val userCodes = listOf("MILK")
            val matched = service.getMatchedAllergens(dishAllergens, userCodes)
            assertThat(matched).containsExactly("MILK")
        }

        @Test
        @DisplayName("Excludes FREE_OF allergens from matched list")
        fun excludesFreeOf() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.FREE_OF, 1)
            )
            val userCodes = listOf("GLUTEN")
            val matched = service.getMatchedAllergens(dishAllergens, userCodes)
            assertThat(matched).isEmpty()
        }

        @Test
        @DisplayName("Returns empty list when no allergens match")
        fun emptyWhenNoMatch() {
            val dishAllergens = listOf(
                makeDishAllergen("GLUTEN", ContainmentLevel.CONTAINS, 1)
            )
            val userCodes = listOf("EGGS")
            assertThat(service.getMatchedAllergens(dishAllergens, userCodes)).isEmpty()
        }
    }
}
