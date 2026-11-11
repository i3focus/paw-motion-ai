package com.pawmotionai.shared

import org.springframework.http.HttpStatus

open class ApiException(val status: HttpStatus, val title: String, detail: String) : RuntimeException(detail)

class NotFoundException(detail: String) : ApiException(HttpStatus.NOT_FOUND, "Resource not found", detail)

class ForbiddenException(detail: String) : ApiException(HttpStatus.FORBIDDEN, "Access denied", detail)

class ConflictException(detail: String) : ApiException(HttpStatus.CONFLICT, "Conflict", detail)

class BadRequestException(detail: String) : ApiException(HttpStatus.BAD_REQUEST, "Invalid request", detail)

class QuotaExceededException(detail: String) : ApiException(HttpStatus.TOO_MANY_REQUESTS, "Quota exceeded", detail)
