package com.apptolast.menus.menu.controller

import com.apptolast.menus.menu.dto.request.MenuRequest
import com.apptolast.menus.menu.dto.request.SectionRequest
import com.apptolast.menus.menu.dto.response.MenuResponse
import com.apptolast.menus.menu.dto.response.SectionResponse
import com.apptolast.menus.menu.service.MenuService
import com.apptolast.menus.restaurant.service.RestaurantService
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/menus")
@Tag(name = "Admin - Menus", description = "Restaurant menu management")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
class AdminMenuController(
    private val menuService: MenuService,
    private val restaurantService: RestaurantService
) {

    @GetMapping
    @Operation(summary = "List all menus for own restaurant")
    fun listMenus(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "false") archived: Boolean
    ): ResponseEntity<List<MenuResponse>> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        return ResponseEntity.ok(menuService.findByRestaurant(restaurant.id, archived))
    }

    @PostMapping
    @Operation(summary = "Create a new menu")
    fun createMenu(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: MenuRequest
    ): ResponseEntity<MenuResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        val menu = menuService.create(restaurant.id, restaurant.tenantId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(menu)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a menu")
    fun updateMenu(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: MenuRequest
    ): ResponseEntity<MenuResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        return ResponseEntity.ok(menuService.update(id, restaurant.tenantId, request))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Archive (soft-delete) a menu")
    fun archiveMenu(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        menuService.archive(id, restaurant.tenantId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{menuId}/sections")
    @Operation(summary = "Add a section to a menu")
    fun addSection(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable menuId: UUID,
        @Valid @RequestBody request: SectionRequest
    ): ResponseEntity<SectionResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        val section = menuService.addSection(menuId, restaurant.tenantId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(section)
    }

    @PutMapping("/{menuId}/sections/{sectionId}")
    @Operation(summary = "Update a section")
    fun updateSection(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable menuId: UUID,
        @PathVariable sectionId: UUID,
        @Valid @RequestBody request: SectionRequest
    ): ResponseEntity<SectionResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        return ResponseEntity.ok(menuService.updateSection(menuId, sectionId, restaurant.tenantId, request))
    }

    @DeleteMapping("/{menuId}/sections/{sectionId}")
    @Operation(summary = "Delete a section")
    fun deleteSection(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable menuId: UUID,
        @PathVariable sectionId: UUID
    ): ResponseEntity<Void> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        menuService.deleteSection(menuId, sectionId, restaurant.tenantId)
        return ResponseEntity.noContent().build()
    }
}
