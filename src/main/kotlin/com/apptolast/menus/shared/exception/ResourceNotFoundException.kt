package com.apptolast.menus.shared.exception

class ResourceNotFoundException(
    errorCode: String = "RESOURCE_NOT_FOUND",
    message: String
) : BusinessException(errorCode, message, 404)
