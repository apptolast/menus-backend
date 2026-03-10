package com.apptolast.menus.allergen.controller

import com.apptolast.menus.allergen.dto.response.AllergenResponse
import com.apptolast.menus.allergen.service.AllergenService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
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
    fun getAllergens(): ResponseEntity<List<AllergenResponse>> =
        ResponseEntity.ok(allergenService.findAll())
}
