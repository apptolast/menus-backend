package com.apptolast.menus.allergen.service

import com.apptolast.menus.allergen.dto.response.AllergenDetailResponse
import com.apptolast.menus.allergen.dto.response.AllergenResponse

interface AllergenService {
    fun findAll(): List<AllergenResponse>
    fun findByCode(code: String): AllergenDetailResponse
}
