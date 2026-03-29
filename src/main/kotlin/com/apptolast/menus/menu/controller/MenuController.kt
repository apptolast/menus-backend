package com.apptolast.menus.menu.controller

import com.apptolast.menus.menu.dto.response.MenuResponse
import com.apptolast.menus.menu.service.MenuService
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/menu")
@Tag(name = "Menu (Public)", description = "Consumer menu browsing with allergen semáforo")
class MenuController(
    private val menuService: MenuService
) {

    @GetMapping
    @Operation(summary = "Get restaurant menu with allergen semáforo if authenticated")
    fun getMenu(
        @PathVariable restaurantId: UUID,
        @AuthenticationPrincipal principal: UserPrincipal?
    ): ResponseEntity<List<MenuResponse>> {
        val menus = menuService.findByRestaurant(restaurantId, false)
        return ResponseEntity.ok(menus)
    }
}
