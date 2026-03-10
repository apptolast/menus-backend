package com.apptolast.menus.allergen.service

import com.apptolast.menus.allergen.dto.response.AllergenResponse

interface AllergenService {
    fun findAll(): List<AllergenResponse>
}
