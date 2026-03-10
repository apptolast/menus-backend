package com.apptolast.menus.shared.exception

class ResourceNotFoundException(
    message: String,
    errorCode: String = "RESOURCE_NOT_FOUND"
) : BusinessException(errorCode, message, 404)
