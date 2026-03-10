package com.apptolast.menus.allergen.service.impl

import com.apptolast.menus.allergen.dto.response.AllergenResponse
import com.apptolast.menus.allergen.repository.AllergenRepository
import com.apptolast.menus.allergen.service.AllergenService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AllergenServiceImpl(
    private val allergenRepository: AllergenRepository
) : AllergenService {

    override fun findAll(): List<AllergenResponse> =
        allergenRepository.findAllWithTranslations().map { allergen ->
            AllergenResponse(
                id = allergen.id,
                code = allergen.code,
                iconUrl = allergen.iconUrl,
                translations = allergen.translations.associate { t -> t.locale to t.name }
            )
        }
}
