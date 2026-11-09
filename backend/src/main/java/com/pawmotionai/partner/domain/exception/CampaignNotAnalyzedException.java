package com.pawmotionai.partner.domain.exception;

public class CampaignNotAnalyzedException extends RuntimeException {

    public CampaignNotAnalyzedException() {
        super("Campaign has not been analyzed yet");
    }
}
