package com.apptolast.menus.shared.exception

class FileUploadException(message: String) : BusinessException(
    errorCode = "FILE_UPLOAD_ERROR",
    message = message,
    httpStatus = 400
)
