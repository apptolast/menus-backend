package com.apptolast.menus.shared.dto

import java.time.OffsetDateTime

data class ErrorResponse(
    val error: ErrorDetail
)

data class ErrorDetail(
    val code: String,
    val message: String,
    val status: Int,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    val path: String = ""
)
