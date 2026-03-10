package com.apptolast.menus.shared.exception

class ConflictException(
    message: String,
    errorCode: String = "CONFLICT"
) : BusinessException(errorCode, message, 409)
