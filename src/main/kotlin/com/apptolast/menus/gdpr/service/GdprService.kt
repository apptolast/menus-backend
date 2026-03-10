package com.apptolast.menus.gdpr.service

import com.apptolast.menus.gdpr.dto.response.DataExportResponse
import java.util.UUID

interface GdprService {
    fun exportUserData(userId: UUID): DataExportResponse
    fun deleteUserData(userId: UUID)
}
