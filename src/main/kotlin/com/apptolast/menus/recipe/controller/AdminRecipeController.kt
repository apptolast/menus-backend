package com.apptolast.menus.recipe.controller

import com.apptolast.menus.recipe.dto.request.CreateRecipeRequest
import com.apptolast.menus.recipe.dto.request.UpdateRecipeRequest
import com.apptolast.menus.recipe.dto.response.ComputedAllergenResponse
import com.apptolast.menus.recipe.dto.response.RecipeResponse
import com.apptolast.menus.recipe.dto.response.RecipeSummaryResponse
import com.apptolast.menus.recipe.mapper.toResponse
import com.apptolast.menus.recipe.service.RecipeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@Tag(name = "Admin - Recipes", description = "Recipe management with allergen computation")
@SecurityRequirement(name = "Bearer Authentication")
class AdminRecipeController(
    private val recipeService: RecipeService
) {

    @GetMapping("/api/v1/admin/restaurants/{restaurantId}/recipes")
    @Operation(summary = "List recipes for a restaurant")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of recipes"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    )
    fun listRecipes(
        @PathVariable restaurantId: UUID
    ): ResponseEntity<List<RecipeSummaryResponse>> =
        ResponseEntity.ok(recipeService.findAllByRestaurant(restaurantId))

    @GetMapping("/api/v1/admin/recipes/{id}")
    @Operation(summary = "Get recipe with computed allergens")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Recipe detail"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "404", description = "Recipe not found")
    )
    fun getRecipe(@PathVariable id: UUID): ResponseEntity<RecipeResponse> =
        ResponseEntity.ok(recipeService.findById(id))

    @PostMapping("/api/v1/admin/restaurants/{restaurantId}/recipes")
    @Operation(summary = "Create a recipe with ingredients")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Recipe created"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "404", description = "Ingredient not found")
    )
    fun createRecipe(
        @Valid @RequestBody request: CreateRecipeRequest
    ): ResponseEntity<RecipeResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(recipeService.create(request))

    @PutMapping("/api/v1/admin/recipes/{id}")
    @Operation(summary = "Update a recipe")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Recipe updated"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "404", description = "Recipe not found")
    )
    fun updateRecipe(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateRecipeRequest
    ): ResponseEntity<RecipeResponse> =
        ResponseEntity.ok(recipeService.update(id, request))

    @DeleteMapping("/api/v1/admin/recipes/{id}")
    @Operation(summary = "Delete a recipe")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Recipe deleted"),
        ApiResponse(responseCode = "404", description = "Recipe not found")
    )
    fun deleteRecipe(@PathVariable id: UUID): ResponseEntity<Void> {
        recipeService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/api/v1/admin/recipes/{id}/allergens")
    @Operation(summary = "Get computed allergens for a recipe (flat UNION from all ingredients)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Computed allergens list"),
        ApiResponse(responseCode = "404", description = "Recipe not found")
    )
    fun getRecipeAllergens(@PathVariable id: UUID): ResponseEntity<List<ComputedAllergenResponse>> =
        ResponseEntity.ok(recipeService.computeAllergens(id).map { it.toResponse() })
}
