package com.apptolast.menus.recipe.controller

import com.apptolast.menus.recipe.dto.request.CreateRecipeRequest
import com.apptolast.menus.recipe.dto.request.UpdateRecipeRequest
import com.apptolast.menus.recipe.dto.response.AllergenBreakdownResponse
import com.apptolast.menus.recipe.dto.response.ComponentAllergenDetail
import com.apptolast.menus.recipe.dto.response.RecipeResponse
import com.apptolast.menus.recipe.dto.response.RecipeSummaryResponse
import com.apptolast.menus.recipe.mapper.toEntity
import com.apptolast.menus.recipe.mapper.toRecipeIngredientInput
import com.apptolast.menus.recipe.mapper.toResponse
import com.apptolast.menus.recipe.mapper.toSummaryResponse
import com.apptolast.menus.recipe.service.RecipeAllergenCalculator
import com.apptolast.menus.recipe.service.RecipeService
import com.apptolast.menus.restaurant.service.RestaurantService
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/recipes")
@Tag(name = "Admin - Recipes", description = "Recipe management with sub-elaboration and allergen computation")
@SecurityRequirement(name = "Bearer Authentication")
class AdminRecipeController(
    private val recipeService: RecipeService,
    private val recipeAllergenCalculator: RecipeAllergenCalculator,
    private val restaurantService: RestaurantService
) {

    @GetMapping
    @Operation(summary = "List all recipes for own restaurant", description = "Returns recipe summaries with computed allergen counts")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of recipes"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    )
    fun listRecipes(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<List<RecipeSummaryResponse>> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        val recipes = recipeService.findAllByRestaurant(restaurant.id)
        val summaries = recipes.map { recipe ->
            val allergenCount = recipeAllergenCalculator.computeAllergens(recipe.id).size
            recipe.toSummaryResponse(allergenCount)
        }
        return ResponseEntity.ok(summaries)
    }

    @PostMapping
    @Operation(summary = "Create a recipe with components", description = "Creates a recipe with ingredient and/or sub-recipe components. Requires role RESTAURANT_OWNER.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Recipe created"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(responseCode = "404", description = "Referenced ingredient or sub-recipe not found")
    )
    fun createRecipe(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateRecipeRequest
    ): ResponseEntity<RecipeResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        val recipe = request.toEntity(
            tenantId = restaurant.id,
            restaurantId = restaurant.id,
            createdBy = principal.profileUuid
        )
        val ingredientInputs = request.components.map { it.toRecipeIngredientInput() }
        val savedRecipe = recipeService.create(recipe, ingredientInputs)
        val allergens = recipeAllergenCalculator.computeAllergens(savedRecipe.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRecipe.toResponse(allergens))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get recipe detail", description = "Returns full recipe detail with computed allergens and component breakdown")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Recipe detail"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(responseCode = "404", description = "Recipe not found")
    )
    fun getRecipe(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<RecipeResponse> {
        restaurantService.findByOwnerId(principal.userId)
        val recipe = recipeService.findById(id)
        val allergens = recipeAllergenCalculator.computeAllergens(recipe.id)
        return ResponseEntity.ok(recipe.toResponse(allergens))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a recipe", description = "Updates recipe fields and optionally replaces components")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Recipe updated"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(responseCode = "404", description = "Recipe or referenced component not found")
    )
    fun updateRecipe(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateRecipeRequest
    ): ResponseEntity<RecipeResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        val existing = recipeService.findById(id)
        val updatedEntity = buildUpdatedRecipe(existing, request)
        val ingredientInputs = request.components?.map { it.toRecipeIngredientInput() }
        val savedRecipe = recipeService.update(id, updatedEntity, ingredientInputs)
        val allergens = recipeAllergenCalculator.computeAllergens(savedRecipe.id)
        return ResponseEntity.ok(savedRecipe.toResponse(allergens))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete or deactivate a recipe", description = "Hard deletes if not referenced by menus, otherwise soft-deletes (deactivates)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Recipe deleted or deactivated"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(responseCode = "404", description = "Recipe not found")
    )
    fun deleteRecipe(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        restaurantService.findByOwnerId(principal.userId)
        recipeService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/allergen-breakdown")
    @Operation(
        summary = "Get detailed allergen breakdown",
        description = "Shows which component (ingredient or sub-recipe) contributes which allergen to the recipe"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Allergen breakdown detail"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(responseCode = "404", description = "Recipe not found")
    )
    fun getAllergenBreakdown(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<AllergenBreakdownResponse> {
        restaurantService.findByOwnerId(principal.userId)
        val recipe = recipeService.findById(id)
        val allergens = recipeAllergenCalculator.computeAllergens(recipe.id)

        val componentDetails = recipe.ingredients.map { component ->
            val componentName = component.ingredient?.name
                ?: component.subRecipe?.name
                ?: "Unknown"
            val isSubRecipe = component.subRecipe != null

            val componentAllergens = if (isSubRecipe) {
                component.subRecipe?.let { sub ->
                    recipeAllergenCalculator.computeAllergens(sub.id)
                } ?: emptyList()
            } else if (component.ingredient != null) {
                recipeAllergenCalculator.computeAllergens(recipe.id)
                    .filter { allergen -> allergen.sources.any { it.contains(componentName) } }
            } else {
                emptyList()
            }

            ComponentAllergenDetail(
                componentName = componentName,
                isSubRecipe = isSubRecipe,
                allergens = componentAllergens.map { it.toResponse() }
            )
        }

        val response = AllergenBreakdownResponse(
            recipeId = recipe.id,
            recipeName = recipe.name,
            allergens = allergens.map { it.toResponse() },
            components = componentDetails
        )

        return ResponseEntity.ok(response)
    }

    private fun buildUpdatedRecipe(
        existing: com.apptolast.menus.recipe.model.entity.Recipe,
        request: UpdateRecipeRequest
    ): com.apptolast.menus.recipe.model.entity.Recipe {
        return com.apptolast.menus.recipe.model.entity.Recipe(
            id = existing.id,
            tenantId = existing.tenantId,
            restaurantId = existing.restaurantId,
            name = request.name ?: existing.name,
            description = request.description ?: existing.description,
            category = request.category ?: existing.category,
            isSubElaboration = request.isSubElaboration ?: existing.isSubElaboration,
            price = request.price ?: existing.price,
            imageUrl = existing.imageUrl,
            createdBy = existing.createdBy,
            isActive = request.isActive ?: existing.isActive,
            createdAt = existing.createdAt,
            updatedAt = existing.updatedAt
        )
    }
}
