package com.apptolast.menus.shared.controller

import com.apptolast.menus.shared.dto.UploadResponse
import com.apptolast.menus.shared.service.FileStorageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin - File Upload")
@SecurityRequirement(name = "Bearer Authentication")
class FileUploadController(
    private val fileStorageService: FileStorageService
) {
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload a file (image)")
    fun upload(@RequestParam("file") file: MultipartFile): ResponseEntity<UploadResponse> {
        val url = fileStorageService.store(file)
        return ResponseEntity.status(HttpStatus.CREATED).body(UploadResponse(url = url))
    }
}
