package com.apptolast.menus.shared.service

import org.springframework.web.multipart.MultipartFile

interface FileStorageService {
    fun store(file: MultipartFile): String
    fun delete(filename: String)
}
