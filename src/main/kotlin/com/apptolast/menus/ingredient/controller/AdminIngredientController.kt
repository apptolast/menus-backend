package com.apptolast.menus.ingredient.controller

import com.apptolast.menus.ingredient.dto.request.CreateIngredientRequest
import com.apptolast.menus.ingredient.dto.request.IngredientAllergenRequest
import com.apptolast.menus.ingredient.dto.request.UpdateIngredientRequest
import com.apptolast.menus.ingredient.dto.response.IngredientAllergenResponse
import com.apptolast.menus.ingredient.dto.response.IngredientResponse
import com.apptolast.menus.ingredient.mapper.applyTo
import com.apptolast.menus.ingredient.mapper.toEntity
import com.apptolast.menus.ingredient.mapper.toResponse
import com.apptolast.menus.ingredient.service.IngredientService
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
@RequestMapping("/api/v1/admin/ingredients")
@Tag(name = "Admin - Ingredients", description = "Global ingredient catalog management")
@SecurityRequirement(name = "Bearer Authentication")
class AdminIngredientController(
    private val ingredientService: IngredientService
) {

    @GetMapping
    @Operation(summary = "List all ingredients")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of ingredients"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    )
    fun listIngredients(): ResponseEntity<List<IngredientResponse>> =
        ResponseEntity.ok(ingredientService.findAll().map { it.toResponse() })

    @GetMapping("/{id}")
    @Operation(summary = "Get ingredient by ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Ingredient found"),
        ApiResponse(responseCode = "404", description = "Ingredient not found")
    )
    fun getIngredient(@PathVariable id: UUID): ResponseEntity<IngredientResponse> =
        ResponseEntity.ok(ingredientService.findById(id).toResponse())

    @PostMapping
    @Operation(summary = "Create an ingredient")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Ingredient created"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "409", description = "Ingredient with same name already exists")
    )
    fun createIngredient(
        @Valid @RequestBody request: CreateIngredientRequest
    ): ResponseEntity<IngredientResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(ingredientService.create(request.toEntity()).toResponse())

    @PutMapping("/{id}")
    @Operation(summary = "Update an ingredient")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Ingredient updated"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "404", description = "Ingredient not found")
    )
    fun updateIngredient(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateIngredientRequest
    ): ResponseEntity<IngredientResponse> {
        val existing = ingredientService.findById(id)
        val updated = request.applyTo(existing)
        return ResponseEntity.ok(ingredientService.update(id, updated).toResponse())
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an ingredient")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Ingredient deleted"),
        ApiResponse(responseCode = "404", description = "Ingredient not found")
    )
    fun deleteIngredient(@PathVariable id: UUID): ResponseEntity<Void> {
        ingredientService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/search")
    @Operation(summary = "Search ingredients by name")
    @ApiResponse(responseCode = "200", description = "Search results returned")
    fun searchIngredients(@RequestParam name: String): ResponseEntity<List<IngredientResponse>> =
        ResponseEntity.ok(ingredientService.searchByName(name).map { it.toResponse() })

    @GetMapping("/{id}/allergens")
    @Operation(summary = "Get allergens for an ingredient")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Allergens returned"),
        ApiResponse(responseCode = "404", description = "Ingredient not found")
    )
    fun getAllergens(@PathVariable id: UUID): ResponseEntity<List<IngredientAllergenResponse>> =
        ResponseEntity.ok(ingredientService.getAllergens(id))

    @PutMapping("/{id}/allergens")
    @Operation(summary = "Set allergens for an ingredient (replaces existing)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Allergens updated"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "404", description = "Ingredient or allergen not found")
    )
    fun setAllergens(
        @PathVariable id: UUID,
        @Valid @RequestBody allergens: List<IngredientAllergenRequest>
    ): ResponseEntity<IngredientResponse> =
        ResponseEntity.ok(ingredientService.setAllergens(id, allergens).toResponse())
}
