package com.apptolast.menus.shared.handler

import com.apptolast.menus.shared.dto.ErrorDetail
import com.apptolast.menus.shared.dto.ErrorResponse
import com.apptolast.menus.shared.exception.BusinessException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            error = ErrorDetail(
                code = ex.errorCode,
                message = ex.message ?: "An error occurred",
                status = ex.httpStatus,
                timestamp = OffsetDateTime.now(),
                path = request.requestURI
            )
        )
        return ResponseEntity.status(ex.httpStatus).body(response)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.allErrors
            .filterIsInstance<FieldError>()
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        val response = ErrorResponse(
            error = ErrorDetail(
                code = "VALIDATION_ERROR",
                message = fieldErrors,
                status = 400,
                timestamp = OffsetDateTime.now(),
                path = request.requestURI
            )
        )
        return ResponseEntity.badRequest().body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error processing request [{}] {}", request.method, request.requestURI, ex)
        val response = ErrorResponse(
            error = ErrorDetail(
                code = "INTERNAL_ERROR",
                message = "An unexpected error occurred",
                status = 500,
                timestamp = OffsetDateTime.now(),
                path = request.requestURI
            )
        )
        return ResponseEntity.internalServerError().body(response)
    }
}
