package com.apptolast.menus.shared.exception

class ForbiddenException(
    errorCode: String = "FORBIDDEN",
    message: String
) : BusinessException(errorCode, message, 403)
