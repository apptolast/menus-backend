package com.apptolast.menus.shared.service.impl

import com.apptolast.menus.config.AppConfig
import com.apptolast.menus.shared.exception.FileUploadException
import com.apptolast.menus.shared.service.FileStorageService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class LocalFileStorageService(
    private val appConfig: AppConfig
) : FileStorageService {

    private val logger = LoggerFactory.getLogger(LocalFileStorageService::class.java)
    private lateinit var uploadPath: Path

    @PostConstruct
    fun init() {
        uploadPath = Paths.get(appConfig.upload.dir).toAbsolutePath().normalize()
        try {
            Files.createDirectories(uploadPath)
            logger.info("Upload directory ready: $uploadPath")
        } catch (e: Exception) {
            logger.warn("Could not create upload directory: $uploadPath — file uploads will fail until directory is available", e)
        }
    }

    override fun store(file: MultipartFile): String {
        if (file.isEmpty) {
            throw FileUploadException("File is empty")
        }
        val contentType = file.contentType ?: throw FileUploadException("Content type is required")
        if (contentType !in appConfig.upload.allowedTypes) {
            throw FileUploadException("File type '$contentType' is not allowed. Allowed: ${appConfig.upload.allowedTypes}")
        }
        val maxBytes = appConfig.upload.maxSizeMb * 1024 * 1024
        if (file.size > maxBytes) {
            throw FileUploadException("File size exceeds maximum of ${appConfig.upload.maxSizeMb}MB")
        }
        val extension = file.originalFilename?.substringAfterLast('.', "") ?: ""
        val safeExtension = if (extension.matches(Regex("[a-zA-Z0-9]{1,10}"))) ".$extension" else ""
        val filename = "${UUID.randomUUID()}$safeExtension"
        val targetPath = uploadPath.resolve(filename).normalize()
        if (!targetPath.startsWith(uploadPath)) {
            throw FileUploadException("Invalid file path")
        }
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath)
        }
        Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
        return "/uploads/$filename"
    }

    override fun delete(filename: String) {
        val targetPath = uploadPath.resolve(filename).normalize()
        if (targetPath.startsWith(uploadPath)) {
            Files.deleteIfExists(targetPath)
        }
    }
}
