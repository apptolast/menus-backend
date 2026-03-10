package com.apptolast.menus.menu.dto.response

import java.util.UUID

data class SectionResponse(
    val id: UUID,
    val name: String,
    val displayOrder: Int
)
