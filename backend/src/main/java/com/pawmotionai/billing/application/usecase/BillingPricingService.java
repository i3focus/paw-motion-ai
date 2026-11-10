package com.pawmotionai.billing.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.billing.application.dto.PricingResponseDto;
import com.pawmotionai.billing.application.port.in.GetBillingPricingUseCase;
import com.pawmotionai.billing.application.port.out.PaymentGatewayPort;
import com.pawmotionai.configuration.BillingProperties;
import com.pawmotionai.configuration.CampaignPricingProperties;

import java.util.List;

@Service
public class BillingPricingService implements GetBillingPricingUseCase {

    private final BillingProperties billingProperties;
    private final CampaignPricingProperties campaignPricingProperties;
    private final List<PaymentGatewayPort> gateways;

    public BillingPricingService(BillingProperties billingProperties, CampaignPricingProperties campaignPricingProperties,
                                 List<PaymentGatewayPort> gateways) {
        this.billingProperties = billingProperties;
        this.campaignPricingProperties = campaignPricingProperties;
        this.gateways = gateways;
    }

    @Override
    @Transactional(readOnly = true)
    public PricingResponseDto pricing() {
        return new PricingResponseDto(billingProperties.getCurrency(), billingProperties.getCreditsPerCurrencyUnit(),
            billingProperties.getMinPurchaseAmount(), billingProperties.getMaxPurchaseAmount(),
            campaignPricingProperties.getPriceAmount(), campaignPricingProperties.getCreationCostCredits(),
            campaignPricingProperties.getDurationDays(), billingProperties.getProvider());
    }
}
