package com.apptolast.menus.shared.exception

class ForbiddenException(
    message: String,
    errorCode: String = "FORBIDDEN"
) : BusinessException(errorCode, message, 403)
