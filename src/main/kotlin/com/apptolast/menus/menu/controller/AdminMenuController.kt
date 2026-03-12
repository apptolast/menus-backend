package com.apptolast.menus.menu.controller

import com.apptolast.menus.menu.dto.request.AddMenuRecipeRequest
import com.apptolast.menus.menu.dto.request.MenuRequest
import com.apptolast.menus.menu.dto.request.PublishRequest
import com.apptolast.menus.menu.dto.request.SectionRequest
import com.apptolast.menus.menu.dto.response.AllergenMatrixResponse
import com.apptolast.menus.menu.dto.response.MenuResponse
import com.apptolast.menus.menu.dto.response.SectionResponse
import com.apptolast.menus.menu.service.MenuService
import com.apptolast.menus.restaurant.service.RestaurantService
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Menu created"),
        ApiResponse(responseCode = "400", description = "Validation failed")
    )
    fun createMenu(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: MenuRequest
    ): ResponseEntity<MenuResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        val menu = menuService.create(restaurant.id, restaurant.id, request)
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
        return ResponseEntity.ok(menuService.update(id, restaurant.id, request))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Archive (soft-delete) a menu")
    @ApiResponse(responseCode = "204", description = "Menu archived")
    fun archiveMenu(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        menuService.archive(id, restaurant.id)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "Toggle publish status of a menu")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Publish status updated"),
        ApiResponse(responseCode = "404", description = "Menu not found")
    )
    fun publishMenu(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: PublishRequest
    ): ResponseEntity<MenuResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        return ResponseEntity.ok(menuService.publish(id, restaurant.id, request.published))
    }

    @PostMapping("/{menuId}/recipes")
    @Operation(summary = "Add a recipe to a menu")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Recipe added to menu"),
        ApiResponse(responseCode = "404", description = "Menu or recipe not found"),
        ApiResponse(responseCode = "409", description = "Recipe already in menu")
    )
    fun addRecipeToMenu(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable menuId: UUID,
        @Valid @RequestBody request: AddMenuRecipeRequest
    ): ResponseEntity<Void> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        menuService.addRecipeToMenu(menuId, restaurant.id, request.recipeId, request.section, request.sortOrder)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @DeleteMapping("/{menuId}/recipes/{recipeId}")
    @Operation(summary = "Remove a recipe from a menu")
    @ApiResponse(responseCode = "204", description = "Recipe removed from menu")
    fun removeRecipeFromMenu(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable menuId: UUID,
        @PathVariable recipeId: UUID
    ): ResponseEntity<Void> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        menuService.removeRecipeFromMenu(menuId, restaurant.id, recipeId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/allergen-matrix")
    @Operation(summary = "Get allergen matrix for a menu", description = "Returns a matrix of recipes vs 14 EU allergens with containment levels")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Allergen matrix returned"),
        ApiResponse(responseCode = "404", description = "Menu not found")
    )
    fun getAllergenMatrix(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<AllergenMatrixResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        return ResponseEntity.ok(menuService.getAllergenMatrix(id, restaurant.id))
    }

    @GetMapping("/{id}/export-pdf", produces = [MediaType.APPLICATION_PDF_VALUE])
    @Operation(summary = "Export allergen matrix as PDF", description = "Downloads a PDF file with the allergen declaration matrix for EU 1169/2011 compliance")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "PDF file returned"),
        ApiResponse(responseCode = "404", description = "Menu not found")
    )
    fun exportPdf(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<ByteArray> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        val matrix = menuService.getAllergenMatrix(id, restaurant.id)
        val pdfBytes = menuService.exportPdf(id, restaurant.id)

        val sanitizedName = matrix.menuName.replace(Regex("[^a-zA-Z0-9\\-_ ]"), "").replace(" ", "-")
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        headers.contentDisposition = ContentDisposition.attachment()
            .filename("allergen-matrix-$sanitizedName.pdf")
            .build()

        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfBytes)
    }

    @PostMapping("/{menuId}/sections")
    @Operation(summary = "Add a section to a menu")
    @ApiResponse(responseCode = "201", description = "Section created")
    fun addSection(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable menuId: UUID,
        @Valid @RequestBody request: SectionRequest
    ): ResponseEntity<SectionResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        val section = menuService.addSection(menuId, restaurant.id, request)
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
        return ResponseEntity.ok(menuService.updateSection(sectionId, restaurant.id, request))
    }

    @DeleteMapping("/{menuId}/sections/{sectionId}")
    @Operation(summary = "Delete a section")
    @ApiResponse(responseCode = "204", description = "Section deleted")
    fun deleteSection(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable menuId: UUID,
        @PathVariable sectionId: UUID
    ): ResponseEntity<Void> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        menuService.deleteSection(sectionId, restaurant.id)
        return ResponseEntity.noContent().build()
    }
}
