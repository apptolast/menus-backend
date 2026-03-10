package com.apptolast.menus.gdpr.controller

import com.apptolast.menus.gdpr.dto.response.DataExportResponse
import com.apptolast.menus.gdpr.service.GdprService
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "GDPR", description = "Data rights: export, delete, rectification (GDPR Art. 17, 20)")
@SecurityRequirement(name = "Bearer Authentication")
class GdprController(
    private val gdprService: GdprService
) {

    @GetMapping("/data-export")
    @Operation(summary = "Export all personal data (GDPR Art. 20)")
    fun exportData(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<DataExportResponse> =
        ResponseEntity.ok(gdprService.exportUserData(principal.userId))

    @DeleteMapping("/data-delete")
    @Operation(summary = "Delete account and anonymize personal data (GDPR Art. 17)")
    fun deleteData(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<Void> {
        gdprService.deleteUserData(principal.userId)
        return ResponseEntity.noContent().build()
    }
}
