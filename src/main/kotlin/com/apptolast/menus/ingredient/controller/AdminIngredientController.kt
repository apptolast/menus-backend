package com.apptolast.menus.ingredient.controller

import com.apptolast.menus.ingredient.dto.request.AnalyzeTextRequest
import com.apptolast.menus.ingredient.dto.request.CreateIngredientRequest
import com.apptolast.menus.ingredient.dto.request.UpdateIngredientRequest
import com.apptolast.menus.ingredient.dto.response.AnalyzeTextResponse
import com.apptolast.menus.ingredient.dto.response.IngredientResponse
import com.apptolast.menus.ingredient.mapper.applyTo
import com.apptolast.menus.ingredient.mapper.toEntity
import com.apptolast.menus.ingredient.mapper.toResponse
import com.apptolast.menus.ingredient.service.IngredientService
import com.apptolast.menus.ingredient.service.TextAnalyzerService
import com.apptolast.menus.restaurant.service.RestaurantService
import com.apptolast.menus.shared.security.UserPrincipal
import tools.jackson.databind.ObjectMapper
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
@RequestMapping("/api/v1/admin/ingredients")
@Tag(name = "Admin - Ingredients", description = "Ingredient catalog management for restaurant owners")
@SecurityRequirement(name = "Bearer Authentication")
class AdminIngredientController(
    private val ingredientService: IngredientService,
    private val textAnalyzerService: TextAnalyzerService,
    private val restaurantService: RestaurantService,
    private val objectMapper: ObjectMapper
) {

    @GetMapping
    @Operation(
        summary = "List all ingredients",
        description = "Returns all ingredients for the authenticated user's restaurant (tenant-scoped)"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of ingredients returned successfully"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
        ApiResponse(responseCode = "403", description = "Not authorized")
    )
    fun listIngredients(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<List<IngredientResponse>> {
        val ingredients = ingredientService.findAll()
        return ResponseEntity.ok(ingredients.map { it.toResponse(objectMapper) })
    }

    @PostMapping
    @Operation(
        summary = "Create an ingredient",
        description = "Creates a new ingredient in the restaurant's catalog with allergen declarations"
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Ingredient created successfully"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
        ApiResponse(responseCode = "403", description = "Not authorized"),
        ApiResponse(responseCode = "409", description = "Ingredient with same name already exists")
    )
    fun createIngredient(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateIngredientRequest
    ): ResponseEntity<IngredientResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        val entity = request.toEntity(
            tenantId = restaurant.id,
            createdBy = principal.userId,
            objectMapper = objectMapper
        )
        val created = ingredientService.create(entity)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(created.toResponse(objectMapper))
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get ingredient by ID",
        description = "Returns a single ingredient by its UUID"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Ingredient found"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
        ApiResponse(responseCode = "403", description = "Not authorized"),
        ApiResponse(responseCode = "404", description = "Ingredient not found")
    )
    fun getIngredient(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<IngredientResponse> {
        val ingredient = ingredientService.findById(id)
        return ResponseEntity.ok(ingredient.toResponse(objectMapper))
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update an ingredient",
        description = "Updates an existing ingredient. Only provided fields are modified."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Ingredient updated successfully"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
        ApiResponse(responseCode = "403", description = "Not authorized"),
        ApiResponse(responseCode = "404", description = "Ingredient not found"),
        ApiResponse(responseCode = "409", description = "Ingredient with same name already exists")
    )
    fun updateIngredient(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateIngredientRequest
    ): ResponseEntity<IngredientResponse> {
        val existing = ingredientService.findById(id)
        val updated = request.applyTo(existing, objectMapper)
        val saved = ingredientService.update(id, updated)
        return ResponseEntity.ok(saved.toResponse(objectMapper))
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete an ingredient",
        description = "Permanently deletes an ingredient from the catalog"
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Ingredient deleted successfully"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
        ApiResponse(responseCode = "403", description = "Not authorized"),
        ApiResponse(responseCode = "404", description = "Ingredient not found")
    )
    fun deleteIngredient(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        ingredientService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/analyze-text")
    @Operation(
        summary = "Analyze text for allergens",
        description = "Analyzes free text (e.g. OCR from product labels) and detects potential allergens using keyword matching"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Text analyzed successfully"),
        ApiResponse(responseCode = "400", description = "Validation failed - text is required"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
        ApiResponse(responseCode = "403", description = "Not authorized")
    )
    fun analyzeText(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AnalyzeTextRequest
    ): ResponseEntity<AnalyzeTextResponse> {
        val result = textAnalyzerService.analyzeText(request.text)
        return ResponseEntity.ok(result.toResponse())
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search ingredients by name",
        description = "Searches for ingredients matching the given name (case-insensitive, partial match)"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Search results returned"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
        ApiResponse(responseCode = "403", description = "Not authorized")
    )
    fun searchIngredients(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam name: String
    ): ResponseEntity<List<IngredientResponse>> {
        val ingredients = ingredientService.searchByName(name)
        return ResponseEntity.ok(ingredients.map { it.toResponse(objectMapper) })
    }
}
