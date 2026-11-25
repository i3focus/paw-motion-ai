package com.pawmotionai.configuration

import com.pawmotionai.shared.PageOutOfRangeException
import com.pawmotionai.activity.domain.exception.ActivityNotFoundException
import com.pawmotionai.activity.domain.exception.ActivityOutOfRangeException
import com.pawmotionai.activity.domain.exception.InvalidActivityException
import com.pawmotionai.billing.domain.exception.InvalidPaymentStateException
import com.pawmotionai.billing.domain.exception.PaymentGatewayException
import com.pawmotionai.billing.domain.exception.PaymentTransactionNotFoundException
import com.pawmotionai.billing.domain.exception.WebhookVerificationException
import com.pawmotionai.identity.domain.exception.InvalidCredentialsException
import com.pawmotionai.identity.domain.exception.OwnerAlreadyExistsException
import com.pawmotionai.identity.domain.exception.OwnerNotFoundException
import com.pawmotionai.ledger.domain.exception.InsufficientCreditsException
import com.pawmotionai.ledger.domain.exception.LedgerConflictException
import com.pawmotionai.ledger.domain.exception.WalletBlockedException
import com.pawmotionai.ledger.domain.exception.WalletNotFoundException
import com.pawmotionai.partner.domain.exception.CampaignAccessDeniedException
import com.pawmotionai.partner.domain.exception.CampaignNotFoundException
import com.pawmotionai.partner.domain.exception.CampaignStateException
import com.pawmotionai.pet.domain.exception.InvalidPetException
import com.pawmotionai.pet.domain.exception.PetNotFoundException
import com.pawmotionai.shared.ApiException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        val detail = exception.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed", detail)
    }

    @ExceptionHandler(ApiException::class)
    fun handleApiException(exception: ApiException): ResponseEntity<ProblemDetail> =
        problem(exception.status, exception.title, exception.message)

    @ExceptionHandler(OwnerAlreadyExistsException::class)
    fun handleOwnerAlreadyExists(exception: OwnerAlreadyExistsException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.CONFLICT, "Owner already exists", exception.message)

    @ExceptionHandler(InvalidCredentialsException::class, AuthenticationException::class)
    fun handleAuthentication(exception: RuntimeException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.UNAUTHORIZED, "Authentication failed", "Invalid credentials")

    @ExceptionHandler(CampaignAccessDeniedException::class)
    fun handleCampaignAccessDenied(exception: CampaignAccessDeniedException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.FORBIDDEN, "Access denied", exception.message)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(exception: AccessDeniedException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.FORBIDDEN, "Access denied", exception.message)

    @ExceptionHandler(CampaignStateException::class)
    fun handleCampaignState(exception: CampaignStateException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.CONFLICT, "Invalid campaign state", exception.message)

    @ExceptionHandler(OwnerNotFoundException::class, PetNotFoundException::class, ActivityNotFoundException::class,
        CampaignNotFoundException::class, WalletNotFoundException::class, PaymentTransactionNotFoundException::class)
    fun handleNotFound(exception: RuntimeException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.NOT_FOUND, "Resource not found", exception.message)

    @ExceptionHandler(InsufficientCreditsException::class)
    fun handleInsufficientCredits(exception: InsufficientCreditsException): ResponseEntity<ProblemDetail> {
        val detail = problem(HttpStatus.PAYMENT_REQUIRED, "Insufficient credits", exception.message).body!!
        detail.setProperty("requiredCredits", exception.requiredCredits)
        detail.setProperty("balance", exception.availableBalance)
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(detail)
    }

    @ExceptionHandler(WalletBlockedException::class)
    fun handleWalletBlocked(exception: WalletBlockedException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.LOCKED, "Wallet blocked", exception.message)

    @ExceptionHandler(WebhookVerificationException::class)
    fun handleWebhookVerification(exception: WebhookVerificationException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.UNAUTHORIZED, "Webhook verification failed", exception.message)

    @ExceptionHandler(PaymentGatewayException::class)
    fun handlePaymentGateway(exception: PaymentGatewayException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_GATEWAY, "Payment provider error", exception.message)

    @ExceptionHandler(InvalidPaymentStateException::class)
    fun handleInvalidPaymentState(exception: InvalidPaymentStateException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.CONFLICT, "Invalid payment state", exception.message)

    @ExceptionHandler(LedgerConflictException::class)
    fun handleLedgerConflict(exception: LedgerConflictException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.CONFLICT, "Ledger conflict", exception.message)

    @ExceptionHandler(InvalidPetException::class, InvalidActivityException::class, IllegalArgumentException::class)
    fun handleInvalidDomain(exception: RuntimeException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid request", exception.message)

    @ExceptionHandler(ActivityOutOfRangeException::class)
    fun handleActivityOutOfRange(exception: ActivityOutOfRangeException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_REQUEST, "Value out of range", exception.message)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(exception: HttpMessageNotReadableException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_REQUEST, "Malformed request body", "Request body is not valid JSON for this endpoint")

    @ExceptionHandler(PageOutOfRangeException::class)
    fun handlePageOutOfRange(exception: PageOutOfRangeException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_REQUEST, "Invalid page", exception.message)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception): ResponseEntity<ProblemDetail> {
        log.error("unhandled api exception", exception)
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "An unexpected error occurred")
    }

    private fun problem(status: HttpStatus, title: String, detail: String?): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(status, detail)
        problem.title = title
        problem.type = ProblemDetail.forStatus(status).type
        return ResponseEntity.status(status).body(problem)
    }
}
