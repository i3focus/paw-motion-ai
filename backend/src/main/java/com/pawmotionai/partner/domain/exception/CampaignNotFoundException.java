package com.pawmotionai.partner.domain.exception;

public class CampaignNotFoundException extends RuntimeException {

    public CampaignNotFoundException() {
        super("Campaign not found");
    }
}
