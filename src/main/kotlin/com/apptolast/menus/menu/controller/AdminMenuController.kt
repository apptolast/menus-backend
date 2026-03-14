package com.apptolast.menus.menu.controller

import com.apptolast.menus.menu.dto.request.MenuRequest
import com.apptolast.menus.menu.dto.request.PublishRequest
import com.apptolast.menus.menu.dto.request.SectionRequest
import com.apptolast.menus.menu.dto.request.UpdateMenuRecipesRequest
import com.apptolast.menus.menu.dto.response.MenuResponse
import com.apptolast.menus.menu.dto.response.SectionResponse
import com.apptolast.menus.menu.service.MenuService
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
@Tag(name = "Admin - Menus", description = "Restaurant menu management")
@SecurityRequirement(name = "Bearer Authentication")
class AdminMenuController(
    private val menuService: MenuService
) {

    @GetMapping("/api/v1/admin/restaurants/{restaurantId}/menus")
    @Operation(summary = "List menus for a restaurant")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of menus"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    )
    fun listMenus(
        @PathVariable restaurantId: UUID,
        @RequestParam(defaultValue = "false") archived: Boolean
    ): ResponseEntity<List<MenuResponse>> =
        ResponseEntity.ok(menuService.findByRestaurant(restaurantId, archived))

    @PostMapping("/api/v1/admin/restaurants/{restaurantId}/menus")
    @Operation(summary = "Create a menu for a restaurant")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Menu created"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "404", description = "Restaurant not found")
    )
    fun createMenu(
        @PathVariable restaurantId: UUID,
        @Valid @RequestBody request: MenuRequest
    ): ResponseEntity<MenuResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(menuService.create(restaurantId, request))

    @PutMapping("/api/v1/admin/menus/{id}")
    @Operation(summary = "Update a menu")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Menu updated"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "404", description = "Menu not found")
    )
    fun updateMenu(
        @PathVariable id: UUID,
        @Valid @RequestBody request: MenuRequest
    ): ResponseEntity<MenuResponse> =
        ResponseEntity.ok(menuService.update(id, request))

    @DeleteMapping("/api/v1/admin/menus/{id}")
    @Operation(summary = "Archive (soft-delete) a menu")
    @ApiResponse(responseCode = "204", description = "Menu archived")
    fun archiveMenu(@PathVariable id: UUID): ResponseEntity<Void> {
        menuService.archive(id)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/api/v1/admin/menus/{id}/publish")
    @Operation(summary = "Toggle publish status of a menu")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Publish status updated"),
        ApiResponse(responseCode = "404", description = "Menu not found")
    )
    fun publishMenu(
        @PathVariable id: UUID,
        @Valid @RequestBody request: PublishRequest
    ): ResponseEntity<MenuResponse> =
        ResponseEntity.ok(menuService.publish(id, request.published))

    @PostMapping("/api/v1/admin/menus/{menuId}/sections")
    @Operation(summary = "Add a section to a menu")
    @ApiResponse(responseCode = "201", description = "Section created")
    fun addSection(
        @PathVariable menuId: UUID,
        @Valid @RequestBody request: SectionRequest
    ): ResponseEntity<SectionResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(menuService.addSection(menuId, request))

    @PutMapping("/api/v1/admin/menus/{menuId}/sections/{sectionId}")
    @Operation(summary = "Update a section")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Section updated"),
        ApiResponse(responseCode = "404", description = "Section not found")
    )
    fun updateSection(
        @PathVariable menuId: UUID,
        @PathVariable sectionId: UUID,
        @Valid @RequestBody request: SectionRequest
    ): ResponseEntity<SectionResponse> =
        ResponseEntity.ok(menuService.updateSection(sectionId, request))

    @DeleteMapping("/api/v1/admin/menus/{menuId}/sections/{sectionId}")
    @Operation(summary = "Delete a section")
    @ApiResponse(responseCode = "204", description = "Section deleted")
    fun deleteSection(
        @PathVariable menuId: UUID,
        @PathVariable sectionId: UUID
    ): ResponseEntity<Void> {
        menuService.deleteSection(sectionId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/api/v1/admin/menus/{id}/recipes")
    @Operation(summary = "Update recipes assigned to a menu")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Recipes updated"),
        ApiResponse(responseCode = "404", description = "Menu or recipe not found")
    )
    fun updateMenuRecipes(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateMenuRecipesRequest
    ): ResponseEntity<MenuResponse> =
        ResponseEntity.ok(menuService.updateRecipes(id, request.recipeIds))
}
