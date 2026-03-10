package com.apptolast.menus.shared.handler

import com.apptolast.menus.shared.dto.ErrorDetail
import com.apptolast.menus.shared.dto.ErrorResponse
import com.apptolast.menus.shared.exception.BusinessException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(
        ex: BadCredentialsException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            error = ErrorDetail(
                code = "INVALID_CREDENTIALS",
                message = "Invalid credentials",
                status = 401,
                timestamp = OffsetDateTime.now(),
                path = request.requestURI
            )
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response)
    }

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
