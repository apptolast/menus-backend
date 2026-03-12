package com.apptolast.menus.allergen.service.impl

import com.apptolast.menus.allergen.dto.response.AllergenResponse
import com.apptolast.menus.allergen.repository.AllergenRepository
import com.apptolast.menus.allergen.service.AllergenService
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AllergenServiceImpl(
    private val allergenRepository: AllergenRepository
) : AllergenService {

    override fun findAll(): List<AllergenResponse> =
        allergenRepository.findAll().sortedBy { it.displayOrder }.map { allergen ->
            AllergenResponse(
                id = allergen.id,
                code = allergen.code,
                nameEs = allergen.nameEs,
                nameEn = allergen.nameEn,
                iconUrl = allergen.iconUrl
            )
        }

    override fun findByCode(code: String): AllergenResponse {
        val allergen = allergenRepository.findByCode(code.uppercase())
            ?: throw ResourceNotFoundException("ALLERGEN_NOT_FOUND", "Allergen not found with code: $code")
        return AllergenResponse(
            id = allergen.id,
            code = allergen.code,
            nameEs = allergen.nameEs,
            nameEn = allergen.nameEn,
            iconUrl = allergen.iconUrl
        )
    }
}
