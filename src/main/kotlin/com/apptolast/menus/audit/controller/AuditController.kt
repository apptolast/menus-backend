package com.apptolast.menus.audit.controller

import com.apptolast.menus.audit.dto.response.AuditLogResponse
import com.apptolast.menus.audit.mapper.toResponse
import com.apptolast.menus.audit.service.AuditService
import com.apptolast.menus.restaurant.service.RestaurantService
import com.apptolast.menus.shared.dto.PageResponse
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@Tag(name = "Admin - Audit Logs", description = "Allergen change audit trail for the restaurant")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
class AuditController(
    private val auditService: AuditService,
    private val restaurantService: RestaurantService
) {

    @GetMapping
    @Operation(
        summary = "List all audit logs for own restaurant",
        description = "Returns paginated allergen change history scoped to the authenticated owner's restaurant (tenant)."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    )
    fun listAuditLogs(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PageableDefault(size = 20, sort = ["changedAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<PageResponse<AuditLogResponse>> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        val page = auditService.findByTenant(restaurant.id, pageable)
        val response = PageResponse(
            content = page.content.map { it.toResponse() },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            last = page.isLast
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/dish/{dishId}")
    @Operation(
        summary = "Get audit history for a specific dish",
        description = "Returns all allergen change records for the given dish, ordered by most recent first."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Dish audit logs retrieved successfully"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    )
    fun getAuditLogsByDish(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable dishId: UUID
    ): ResponseEntity<List<AuditLogResponse>> {
        // Verify the caller owns a restaurant (authorization check via tenant scoping)
        restaurantService.findByOwnerId(principal.userId)
        val logs = auditService.findByDish(dishId).map { it.toResponse() }
        return ResponseEntity.ok(logs)
    }
}
