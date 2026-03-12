package com.apptolast.menus.gdpr.controller

import com.apptolast.menus.gdpr.dto.request.RectificationRequest
import com.apptolast.menus.gdpr.dto.response.DataExportResponse
import com.apptolast.menus.gdpr.service.GdprService
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "GDPR", description = "Data rights: export, delete, rectification (GDPR Art. 16, 17, 20)")
@SecurityRequirement(name = "Bearer Authentication")
class GdprController(
    private val gdprService: GdprService
) {

    @GetMapping("/data-export")
    @Operation(summary = "Export all personal data (GDPR Art. 20)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Data exported successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun exportData(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<DataExportResponse> =
        ResponseEntity.ok(gdprService.exportUserData(principal.userId))

    @DeleteMapping("/data-delete")
    @Operation(summary = "Delete account and anonymize personal data (GDPR Art. 17)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Account deleted and anonymized"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun deleteData(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<Void> {
        gdprService.deleteUserData(principal.userId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/data-rectification")
    @Operation(summary = "Rectify personal data (GDPR Art. 16)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Personal data rectified successfully"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "404", description = "User not found")
    )
    fun rectifyData(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: RectificationRequest
    ): ResponseEntity<Void> {
        gdprService.rectifyUserData(principal.userId, request)
        return ResponseEntity.noContent().build()
    }
}
