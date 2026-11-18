package com.pawmotionai.ledger.adapter.`in`.rest

import com.pawmotionai.ledger.application.dto.WalletResponseDto
import com.pawmotionai.ledger.application.port.`in`.LedgerPostingCommands.CreditPosting
import com.pawmotionai.ledger.application.port.`in`.LedgerPostingCommands.DebitPosting
import com.pawmotionai.ledger.application.port.`in`.PostLedgerEntryUseCase
import com.pawmotionai.ledger.application.port.`in`.SetWalletStatusUseCase
import com.pawmotionai.ledger.domain.model.LedgerAccountCode
import com.pawmotionai.ledger.domain.model.LedgerReferenceType
import com.pawmotionai.shared.SecurityRoles
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class AdminAdjustmentRequest(
    @field:NotNull val accountCode: LedgerAccountCode,
    @field:NotBlank val direction: String,
    @field:Positive val amount: Long,
    @field:NotBlank val reason: String,
)

@RestController
@Secured(SecurityRoles.ADMIN)
@RequestMapping("/api/admin/partners/{partnerId}/wallet")
class AdminWalletController(
    private val setWalletStatusUseCase: SetWalletStatusUseCase,
    private val postLedgerEntryUseCase: PostLedgerEntryUseCase,
) {

    @PostMapping("/unblock")
    fun unblock(@PathVariable partnerId: UUID): WalletResponseDto =
        setWalletStatusUseCase.unblock(partnerId)

    @PostMapping("/adjustments")
    fun adjust(@PathVariable partnerId: UUID,
               @Valid @RequestBody request: AdminAdjustmentRequest): ResponseEntity<Void> {
        val referenceId = UUID.randomUUID()
        val correlationId = "admin-adjustment-$referenceId"
        if (request.direction.equals("CREDIT", ignoreCase = true)) {
            postLedgerEntryUseCase.credit(CreditPosting(partnerId, request.accountCode, request.amount,
                LedgerReferenceType.ADJUSTMENT, referenceId, request.reason, correlationId))
        } else {
            postLedgerEntryUseCase.debit(DebitPosting(partnerId, request.amount, LedgerReferenceType.ADJUSTMENT,
                referenceId, request.reason, correlationId))
        }
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
