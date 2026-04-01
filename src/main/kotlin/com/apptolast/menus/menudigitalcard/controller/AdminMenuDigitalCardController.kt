package com.apptolast.menus.menudigitalcard.controller

import com.apptolast.menus.menudigitalcard.dto.request.CreateMenuDigitalCardRequest
import com.apptolast.menus.menudigitalcard.dto.request.UpdateMenuDigitalCardRequest
import com.apptolast.menus.menudigitalcard.dto.response.MenuDigitalCardResponse
import com.apptolast.menus.menudigitalcard.service.MenuDigitalCardService
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
@RequestMapping("/api/v1/admin/menu-digital-cards")
@Tag(name = "Admin - Menu Digital Cards", description = "Manage dish assignments to menu digital cards")
@SecurityRequirement(name = "Bearer Authentication")
class AdminMenuDigitalCardController(
    private val menuDigitalCardService: MenuDigitalCardService
) {

    @PostMapping
    @Operation(summary = "Assign a dish to a menu's digital card")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Assignment created"),
        ApiResponse(responseCode = "404", description = "Menu or dish not found"),
        ApiResponse(responseCode = "409", description = "Duplicate assignment")
    )
    fun create(
        @Valid @RequestBody request: CreateMenuDigitalCardRequest
    ): ResponseEntity<MenuDigitalCardResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(menuDigitalCardService.create(request.menuId, request.dishId))

    @GetMapping("/{menuId}")
    @Operation(summary = "List all dishes in a menu's digital card")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of assignments"),
        ApiResponse(responseCode = "404", description = "Menu not found")
    )
    fun findByMenuId(
        @PathVariable menuId: UUID
    ): ResponseEntity<List<MenuDigitalCardResponse>> =
        ResponseEntity.ok(menuDigitalCardService.findByMenuId(menuId))

    @PutMapping("/{id}")
    @Operation(summary = "Update a dish assignment")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Assignment updated"),
        ApiResponse(responseCode = "404", description = "Assignment or dish not found"),
        ApiResponse(responseCode = "409", description = "Duplicate assignment")
    )
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateMenuDigitalCardRequest
    ): ResponseEntity<MenuDigitalCardResponse> =
        ResponseEntity.ok(menuDigitalCardService.update(id, request.dishId))

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a dish from a menu's digital card")
    @ApiResponse(responseCode = "204", description = "Assignment deleted")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        menuDigitalCardService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
