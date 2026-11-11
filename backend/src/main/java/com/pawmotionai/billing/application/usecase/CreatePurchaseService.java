package com.pawmotionai.billing.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.pawmotionai.billing.application.dto.PaymentTransactionResponseDto;
import com.pawmotionai.billing.application.dto.PurchaseCreditsCommand;
import com.pawmotionai.billing.application.port.in.CreatePurchaseUseCase;
import com.pawmotionai.billing.application.port.out.CreateCheckoutRequest;
import com.pawmotionai.billing.application.port.out.PaymentGatewayPort;
import com.pawmotionai.billing.application.port.out.PaymentTransactionRepository;
import com.pawmotionai.billing.domain.exception.PaymentGatewayException;
import com.pawmotionai.billing.domain.model.PaymentTransaction;
import com.pawmotionai.configuration.BillingProperties;
import com.pawmotionai.ledger.application.dto.LedgerAccountResponseDto;
import com.pawmotionai.ledger.application.dto.WalletResponseDto;
import com.pawmotionai.ledger.application.port.in.GetWalletUseCase;
import com.pawmotionai.ledger.domain.exception.WalletBlockedException;

@Service
public class CreatePurchaseService implements CreatePurchaseUseCase {

    private final BillingProperties billingProperties;
    private final PaymentTransactionRepository transactionRepository;
    private final GetWalletUseCase getWalletUseCase;
    private final List<PaymentGatewayPort> gateways;

    public CreatePurchaseService(BillingProperties billingProperties, PaymentTransactionRepository transactionRepository,
                                 GetWalletUseCase getWalletUseCase, List<PaymentGatewayPort> gateways) {
        this.billingProperties = billingProperties;
        this.transactionRepository = transactionRepository;
        this.getWalletUseCase = getWalletUseCase;
        this.gateways = gateways;
    }

    @Override
    public PaymentTransactionResponseDto create(PurchaseCreditsCommand command) {
        validate(command);
        PaymentGatewayPort gateway = gateway();
        WalletResponseDto wallet = getWalletUseCase.getByPartner(command.partnerId());
        if (wallet.status() == com.pawmotionai.ledger.domain.model.WalletStatus.BLOCKED) {
            throw new WalletBlockedException(wallet.blockedReason());
        }
        UUID mainAccountId = wallet.accounts().stream()
            .filter(account -> account.code() == com.pawmotionai.ledger.domain.model.LedgerAccountCode.MAIN)
            .map(LedgerAccountResponseDto::id)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Partner wallet is missing MAIN account"));

        var existing = transactionRepository.findByPartnerIdAndIdempotencyKey(command.partnerId(), command.idempotencyKey());
        if (existing.isPresent()) {
            var existingTransaction = existing.get();
            if (existingTransaction.getAmount().compareTo(command.amount()) != 0
                || !java.util.Objects.equals(existingTransaction.getMethod(), command.method())
                || (existingTransaction.getCustomerId() != null
                    && !java.util.Objects.equals(existingTransaction.getCustomerId(), normalizeCustomerId(command.customerId())))) {
                throw new IllegalArgumentException("Idempotency key was already used for a different purchase");
            }
            return PaymentTransactionResponseDtoMapper.from(existingTransaction);
        }

        BigDecimal amount = command.amount().setScale(2, RoundingMode.UNNECESSARY);
        long expectedCredits = amount.multiply(billingProperties.getCreditsPerCurrencyUnit())
            .setScale(0, RoundingMode.DOWN).longValueExact();
        Instant now = Instant.now();
        PaymentTransaction transaction;
        try {
            transaction = transactionRepository.save(new PaymentTransaction(UUID.randomUUID(), command.partnerId(), amount,
                billingProperties.getCurrency(), billingProperties.getCreditsPerCurrencyUnit(), expectedCredits,
                mainAccountId, command.method(), gateway.provider(), command.idempotencyKey(), now,
                normalizeCustomerId(command.customerId())));
        } catch (DataIntegrityViolationException exception) {
            return transactionRepository.findByPartnerIdAndIdempotencyKey(command.partnerId(), command.idempotencyKey())
                .map(PaymentTransactionResponseDtoMapper::from)
                .orElseThrow(() -> exception);
        }

        String completionUrl = billingProperties.getPartnerPortalBaseUrl() + "/billing/return?transactionId=" + transaction.getId();
        String returnUrl = completionUrl;
        try {
            var session = gateway.createCheckout(new CreateCheckoutRequest(command.partnerId(), transaction.getId(), amount,
                billingProperties.getCurrency(), expectedCredits, completionUrl, returnUrl,
                normalizeCustomerId(command.customerId())));
            transaction.attachCheckout(session.providerCheckoutId(), session.checkoutUrl(), Instant.now());
            return PaymentTransactionResponseDtoMapper.from(transactionRepository.save(transaction));
        } catch (RuntimeException exception) {
            transaction.fail(exception.getMessage(), Instant.now());
            transactionRepository.save(transaction);
            throw new PaymentGatewayException("Payment provider is unavailable", exception);
        }
    }

    private String normalizeCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return null;
        }
        return customerId.trim();
    }

    private PaymentGatewayPort gateway() {
        return gateways.stream()
            .filter(candidate -> candidate.provider().equalsIgnoreCase(billingProperties.getProvider()))
            .findFirst()
            .orElseThrow(() -> new PaymentGatewayException("No payment gateway configured for provider " + billingProperties.getProvider()));
    }

    private void validate(PurchaseCreditsCommand command) {
        if (command == null || command.partnerId() == null || command.amount() == null
            || command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("Purchase is incomplete");
        }
        if (command.amount().compareTo(billingProperties.getMinPurchaseAmount()) < 0
            || command.amount().compareTo(billingProperties.getMaxPurchaseAmount()) > 0) {
            throw new IllegalArgumentException("Purchase amount is outside the configured limits");
        }
        if (command.amount().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Purchase amount must be a whole currency unit");
        }
    }
}
