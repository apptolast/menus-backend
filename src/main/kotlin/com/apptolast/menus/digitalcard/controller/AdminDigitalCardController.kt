package com.apptolast.menus.digitalcard.controller

import com.apptolast.menus.digitalcard.dto.request.CreateDigitalCardRequest
import com.apptolast.menus.digitalcard.dto.request.UpdateDigitalCardRequest
import com.apptolast.menus.digitalcard.dto.response.DigitalCardResponse
import com.apptolast.menus.digitalcard.mapper.toEntity
import com.apptolast.menus.digitalcard.mapper.toResponse
import com.apptolast.menus.digitalcard.model.entity.DigitalCard
import com.apptolast.menus.digitalcard.service.DigitalCardService
import com.apptolast.menus.restaurant.service.RestaurantService
import com.apptolast.menus.shared.dto.ErrorResponse
import com.apptolast.menus.shared.security.UserPrincipal
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayOutputStream
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/digital-cards")
@Tag(name = "Admin - Digital Cards", description = "Digital card management for restaurant menus")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
class AdminDigitalCardController(
    private val digitalCardService: DigitalCardService,
    private val restaurantService: RestaurantService
) {

    @GetMapping
    @Operation(summary = "List all digital cards for own restaurant")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of digital cards"),
        ApiResponse(
            responseCode = "401", description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun listDigitalCards(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<List<DigitalCardResponse>> {
        val cards = digitalCardService.findAll().map { it.toResponse() }
        return ResponseEntity.ok(cards)
    }

    @PostMapping
    @Operation(summary = "Create a digital card for a menu")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Digital card created"),
        ApiResponse(
            responseCode = "400", description = "Validation failed",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404", description = "Menu not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "Slug already exists",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun createDigitalCard(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateDigitalCardRequest
    ): ResponseEntity<DigitalCardResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        val card = request.toEntity(
            tenantId = principal.tenantId ?: restaurant.id,
            restaurantId = restaurant.id
        )
        val created = digitalCardService.create(card)
        return ResponseEntity.status(HttpStatus.CREATED).body(created.toResponse())
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a digital card")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Digital card updated"),
        ApiResponse(
            responseCode = "400", description = "Validation failed",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404", description = "Digital card not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "Slug already exists",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun updateDigitalCard(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateDigitalCardRequest
    ): ResponseEntity<DigitalCardResponse> {
        val existing = digitalCardService.findById(id)
        val updatedEntity = DigitalCard(
            id = existing.id,
            restaurantId = existing.restaurantId,
            menuId = existing.menuId,
            tenantId = existing.tenantId,
            slug = request.slug ?: existing.slug,
            qrCodeUrl = existing.qrCodeUrl,
            isActive = request.isActive ?: existing.isActive,
            customCss = request.customCss ?: existing.customCss,
            createdAt = existing.createdAt,
            updatedAt = existing.updatedAt
        )
        val saved = digitalCardService.update(id, updatedEntity)
        return ResponseEntity.ok(saved.toResponse())
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a digital card (soft delete)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Digital card deactivated"),
        ApiResponse(
            responseCode = "404", description = "Digital card not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun deleteDigitalCard(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        digitalCardService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/generate-qr")
    @Operation(
        summary = "Generate QR code for a digital card",
        description = "Returns a PNG image containing the QR code that links to the public card URL"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "QR code generated as PNG image"),
        ApiResponse(
            responseCode = "404", description = "Digital card not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun generateQrCode(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<ByteArray> {
        val card = digitalCardService.findById(id)
        val publicUrl = "/api/v1/public/cards/${card.slug}"

        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(publicUrl, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT)

        val outputStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream)
        val imageBytes = outputStream.toByteArray()

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(imageBytes)
    }

    companion object {
        private const val QR_WIDTH = 300
        private const val QR_HEIGHT = 300
    }
}
