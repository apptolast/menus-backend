package com.apptolast.menus.allergen.controller

import com.apptolast.menus.allergen.dto.response.AllergenDetailResponse
import com.apptolast.menus.allergen.dto.response.AllergenResponse
import com.apptolast.menus.allergen.service.AllergenService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/allergens")
@Tag(name = "Allergens", description = "14 EU allergens (Regulation 1169/2011)")
class AllergenController(
    private val allergenService: AllergenService
) {

    @GetMapping
    @Operation(summary = "List all 14 EU allergens with translations")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of all allergens")
    )
    fun getAllergens(): ResponseEntity<List<AllergenResponse>> =
        ResponseEntity.ok(allergenService.findAll())

    @GetMapping("/{code}")
    @Operation(summary = "Get a single allergen by code (e.g., GLUTEN, EGGS, MILK)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Allergen detail with all translations"),
        ApiResponse(responseCode = "404", description = "Allergen not found")
    )
    fun getAllergenByCode(
        @PathVariable code: String
    ): ResponseEntity<AllergenDetailResponse> =
        ResponseEntity.ok(allergenService.findByCode(code))
}
