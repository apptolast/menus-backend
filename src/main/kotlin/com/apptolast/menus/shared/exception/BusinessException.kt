package com.apptolast.menus.shared.exception

abstract class BusinessException(
    val errorCode: String,
    message: String,
    val httpStatus: Int = 400
) : RuntimeException(message)
