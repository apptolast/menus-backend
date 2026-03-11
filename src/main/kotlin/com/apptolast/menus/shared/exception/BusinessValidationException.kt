package com.apptolast.menus.shared.exception

class BusinessValidationException(
    errorCode: String = "VALIDATION_ERROR",
    message: String
) : BusinessException(errorCode, message, 400)
