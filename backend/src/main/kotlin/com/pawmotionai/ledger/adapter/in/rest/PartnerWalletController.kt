package com.pawmotionai.ledger.adapter.`in`.rest

import com.pawmotionai.ledger.application.dto.LedgerEntryPage
import com.pawmotionai.ledger.application.dto.WalletResponseDto
import com.pawmotionai.ledger.application.port.`in`.EnsureWalletUseCase
import com.pawmotionai.ledger.application.port.`in`.GetWalletUseCase
import com.pawmotionai.ledger.application.port.`in`.ListWalletEntriesUseCase
import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.SecurityRoles
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Secured(SecurityRoles.PARTNER)
@RequestMapping("/api/partner/wallet")
class PartnerWalletController(
    private val getWalletUseCase: GetWalletUseCase,
    private val listWalletEntriesUseCase: ListWalletEntriesUseCase,
    private val ensureWalletUseCase: EnsureWalletUseCase,
) {

    @GetMapping
    fun get(@AuthenticationPrincipal jwt: Jwt): WalletResponseDto {
        val partnerId = UUID.fromString(jwt.subject)
        ensureWalletUseCase.ensureWallet(partnerId)
        return getWalletUseCase.getByPartner(partnerId)
    }

    @GetMapping("/accounts/{accountId}/entries")
    fun entries(@AuthenticationPrincipal jwt: Jwt, @PathVariable accountId: UUID,
                @RequestParam(defaultValue = "0") page: Int,
                @RequestParam(defaultValue = "20") size: Int): LedgerEntryPage {
        val paging = PageRequest.of(page, size, 20)
        val partnerId = UUID.fromString(jwt.subject)
        ensureWalletUseCase.ensureWallet(partnerId)
        return listWalletEntriesUseCase.listByPartner(partnerId, accountId, paging.page, paging.size)
    }
}
