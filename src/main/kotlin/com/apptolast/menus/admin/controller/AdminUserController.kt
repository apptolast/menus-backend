package com.apptolast.menus.admin.controller

import com.apptolast.menus.admin.dto.request.ChangeRoleRequest
import com.apptolast.menus.admin.dto.response.UserListResponse
import com.apptolast.menus.admin.service.AdminUserService
import com.apptolast.menus.consumer.model.enum.UserRole
import com.apptolast.menus.restaurant.repository.RestaurantRepository
import com.apptolast.menus.shared.dto.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin - Users", description = "Admin user management (ADMIN only)")
@SecurityRequirement(name = "Bearer Authentication")
class AdminUserController(
    private val adminUserService: AdminUserService,
    private val restaurantRepository: RestaurantRepository
) {

    @GetMapping
    @Operation(
        summary = "List users",
        description = "List all users with pagination, optionally filter by role"
    )
    @ApiResponse(responseCode = "200", description = "Users listed successfully")
    fun listUsers(
        @RequestParam(required = false) role: UserRole?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<PageResponse<UserListResponse>> {
        val page = adminUserService.listUsers(role, pageable)
        val content = page.content.map { user ->
            UserListResponse(
                id = user.id,
                emailHash = user.emailHash,
                role = user.role,
                isActive = user.isActive,
                createdAt = user.createdAt,
                hasRestaurant = restaurantRepository.existsByOwnerId(user.id)
            )
        }
        return ResponseEntity.ok(
            PageResponse(
                content = content,
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                last = page.isLast
            )
        )
    }

    @PutMapping("/{userId}/role")
    @Operation(
        summary = "Change user role",
        description = "Change the role of any user. Cannot set RESTAURANT_OWNER without existing restaurant."
    )
    @ApiResponse(responseCode = "200", description = "Role changed successfully")
    fun changeRole(
        @PathVariable userId: UUID,
        @Valid @RequestBody request: ChangeRoleRequest
    ): ResponseEntity<UserListResponse> {
        val user = adminUserService.changeRole(userId, request.role)
        val response = UserListResponse(
            id = user.id,
            emailHash = user.emailHash,
            role = user.role,
            isActive = user.isActive,
            createdAt = user.createdAt,
            hasRestaurant = restaurantRepository.existsByOwnerId(user.id)
        )
        return ResponseEntity.ok(response)
    }
}
