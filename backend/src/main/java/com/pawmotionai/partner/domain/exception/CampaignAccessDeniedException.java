package com.pawmotionai.partner.domain.exception;

public class CampaignAccessDeniedException extends RuntimeException {

    public CampaignAccessDeniedException() {
        super("Campaign does not belong to the authenticated partner");
    }
}
