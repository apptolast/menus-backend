package com.apptolast.menus.shared.exception

class ConflictException(
    errorCode: String = "CONFLICT",
    message: String
) : BusinessException(errorCode, message, 409)
