package com.pawmotionai.configuration;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pawmotionai.activity.domain.exception.ActivityNotFoundException;
import com.pawmotionai.activity.domain.exception.InvalidActivityException;
import com.pawmotionai.identity.domain.exception.InvalidCredentialsException;
import com.pawmotionai.identity.domain.exception.OwnerAlreadyExistsException;
import com.pawmotionai.identity.domain.exception.OwnerNotFoundException;
import com.pawmotionai.pet.domain.exception.InvalidPetException;
import com.pawmotionai.pet.domain.exception.PetNotFoundException;
import com.pawmotionai.partner.domain.exception.CampaignAccessDeniedException;
import com.pawmotionai.partner.domain.exception.CampaignNotFoundException;
import com.pawmotionai.partner.domain.exception.CampaignStateException;
import com.pawmotionai.billing.domain.exception.InvalidPaymentStateException;
import com.pawmotionai.billing.domain.exception.PaymentGatewayException;
import com.pawmotionai.billing.domain.exception.PaymentTransactionNotFoundException;
import com.pawmotionai.billing.domain.exception.WebhookVerificationException;
import com.pawmotionai.ledger.domain.exception.InsufficientCreditsException;
import com.pawmotionai.ledger.domain.exception.LedgerConflictException;
import com.pawmotionai.ledger.domain.exception.WalletBlockedException;
import com.pawmotionai.ledger.domain.exception.WalletNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed", detail);
    }

    @ExceptionHandler(OwnerAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleOwnerAlreadyExists(OwnerAlreadyExistsException exception) {
        return problem(HttpStatus.CONFLICT, "Owner already exists", exception.getMessage());
    }

    @ExceptionHandler({InvalidCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ProblemDetail> handleAuthentication(RuntimeException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentication failed", "Invalid credentials");
    }

    @ExceptionHandler(CampaignAccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleCampaignAccessDenied(CampaignAccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Access denied", exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Access denied", exception.getMessage());
    }

    @ExceptionHandler(CampaignStateException.class)
    public ResponseEntity<ProblemDetail> handleCampaignState(CampaignStateException exception) {
        return problem(HttpStatus.CONFLICT, "Invalid campaign state", exception.getMessage());
    }

    @ExceptionHandler({OwnerNotFoundException.class, PetNotFoundException.class, ActivityNotFoundException.class,
        CampaignNotFoundException.class, WalletNotFoundException.class, PaymentTransactionNotFoundException.class})
    public ResponseEntity<ProblemDetail> handleNotFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
    }

    @ExceptionHandler(InsufficientCreditsException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientCredits(InsufficientCreditsException exception) {
        ProblemDetail problem = problem(HttpStatus.PAYMENT_REQUIRED, "Insufficient credits", exception.getMessage()).getBody();
        problem.setProperty("requiredCredits", exception.getRequiredCredits());
        problem.setProperty("balance", exception.getAvailableBalance());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(problem);
    }

    @ExceptionHandler(WalletBlockedException.class)
    public ResponseEntity<ProblemDetail> handleWalletBlocked(WalletBlockedException exception) {
        return problem(HttpStatus.LOCKED, "Wallet blocked", exception.getMessage());
    }

    @ExceptionHandler(WebhookVerificationException.class)
    public ResponseEntity<ProblemDetail> handleWebhookVerification(WebhookVerificationException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Webhook verification failed", exception.getMessage());
    }

    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ProblemDetail> handlePaymentGateway(PaymentGatewayException exception) {
        return problem(HttpStatus.BAD_GATEWAY, "Payment provider error", exception.getMessage());
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPaymentState(InvalidPaymentStateException exception) {
        return problem(HttpStatus.CONFLICT, "Invalid payment state", exception.getMessage());
    }

    @ExceptionHandler(LedgerConflictException.class)
    public ResponseEntity<ProblemDetail> handleLedgerConflict(LedgerConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Ledger conflict", exception.getMessage());
    }

    @ExceptionHandler({InvalidPetException.class, InvalidActivityException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> handleInvalidDomain(RuntimeException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid request", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "An unexpected error occurred");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(ProblemDetail.forStatus(status).getType());
        return ResponseEntity.status(status).body(problem);
    }
}
