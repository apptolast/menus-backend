package com.apptolast.menus.auth.controller

import com.apptolast.menus.auth.dto.request.WhitelistRequest
import com.apptolast.menus.auth.dto.response.WhitelistResponse
import com.apptolast.menus.auth.service.AdminWhitelistService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/whitelist")
@Tag(name = "Admin Whitelist", description = "Manage admin email whitelist (requires ADMIN role)")
class AdminWhitelistController(
    private val adminWhitelistService: AdminWhitelistService
) {

    @GetMapping
    @Operation(summary = "List all whitelisted admin emails")
    fun getAll(): List<WhitelistResponse> =
        adminWhitelistService.findAll().map { WhitelistResponse(it.id, it.email) }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an email to the admin whitelist")
    fun addEmail(@Valid @RequestBody request: WhitelistRequest): WhitelistResponse {
        val saved = adminWhitelistService.addEmail(request.email)
        return WhitelistResponse(saved.id, saved.email)
    }

    @DeleteMapping("/{email}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove an email from the admin whitelist")
    fun removeEmail(@PathVariable email: String) {
        adminWhitelistService.removeEmail(email)
    }
}
