package com.apptolast.menus.shared.exception

class ConsentRequiredException(
    message: String = "Explicit consent required to process health data (GDPR Art. 9)"
) : BusinessException("ALLERGEN_PROFILE_CONSENT_REQUIRED", message, 403)
