package com.pawmotionai.ledger.adapter.in.rest;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.pawmotionai.ledger.application.dto.WalletResponseDto;
import com.pawmotionai.ledger.application.port.in.LedgerPostingCommands.CreditPosting;
import com.pawmotionai.ledger.application.port.in.LedgerPostingCommands.DebitPosting;
import com.pawmotionai.ledger.application.port.in.PostLedgerEntryUseCase;
import com.pawmotionai.ledger.application.port.in.SetWalletStatusUseCase;
import com.pawmotionai.ledger.domain.model.LedgerAccountCode;
import com.pawmotionai.ledger.domain.model.LedgerReferenceType;

@RestController
@RequestMapping("/api/admin/partners/{partnerId}/wallet")
public class AdminWalletController {

    private final SetWalletStatusUseCase setWalletStatusUseCase;
    private final PostLedgerEntryUseCase postLedgerEntryUseCase;

    public AdminWalletController(SetWalletStatusUseCase setWalletStatusUseCase, PostLedgerEntryUseCase postLedgerEntryUseCase) {
        this.setWalletStatusUseCase = setWalletStatusUseCase;
        this.postLedgerEntryUseCase = postLedgerEntryUseCase;
    }

    @PostMapping("/unblock")
    public WalletResponseDto unblock(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID partnerId) {
        requireAdmin(jwt);
        return setWalletStatusUseCase.unblock(partnerId);
    }

    @PostMapping("/adjustments")
    public ResponseEntity<Void> adjust(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID partnerId,
                                       @Valid @RequestBody AdminAdjustmentRequest request) {
        requireAdmin(jwt);
        UUID referenceId = UUID.randomUUID();
        String correlationId = "admin-adjustment-" + referenceId;
        if ("CREDIT".equalsIgnoreCase(request.direction())) {
            postLedgerEntryUseCase.credit(new CreditPosting(partnerId, request.accountCode(), request.amount(),
                LedgerReferenceType.ADJUSTMENT, referenceId, request.reason(), correlationId));
        } else {
            postLedgerEntryUseCase.debit(new DebitPosting(partnerId, request.amount(), LedgerReferenceType.ADJUSTMENT,
                referenceId, request.reason(), correlationId));
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private void requireAdmin(Jwt jwt) {
        if (jwt == null || !"ADMIN".equalsIgnoreCase(jwt.getClaimAsString("role"))) {
            throw new AccessDeniedException("Admin role required");
        }
    }

    public record AdminAdjustmentRequest(@NotNull LedgerAccountCode accountCode, @NotBlank String direction,
                                         @Positive long amount, @NotBlank String reason) {
    }
}
